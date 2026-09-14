#!/usr/bin/env python3
"""Turn Gradle's XML output into readable CI summaries.

Deliberately dependency-free and written against the stdlib only: this keeps the
pipeline free of third-party actions for what is, in the end, some XML parsing.

Usage:
    ci_summary.py tests            # JUnit results -> job summary
    ci_summary.py coverage         # Kover report  -> job summary
    ci_summary.py comment <pr>     # Kover report  -> sticky pull request comment
"""

from __future__ import annotations

import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

# Packages are named org.evoionosp.noveliq.<module>.*, so the 4th segment
# identifies the Gradle module. Anything that does not match lives in :app.
KNOWN_MODULES = ("domain", "data", "presentation", "playback")

COMMENT_MARKER = "<!-- noveliq-coverage -->"

# Where the project wants to end up. Used only to render the gap, never to fail
# the build — koverVerify owns enforcement.
COVERAGE_TARGET = 90.0


def emit(markdown: str) -> None:
    """Append to the GitHub job summary, or stdout when run locally."""
    print(markdown)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write(markdown + "\n")


def find_kover_report() -> Path | None:
    candidates = sorted(REPO_ROOT.glob("**/build/reports/kover/report.xml"))
    return candidates[0] if candidates else None


# ---------------------------------------------------------------------------
# Unit tests
# ---------------------------------------------------------------------------


def summarise_tests() -> int:
    files = sorted(REPO_ROOT.glob("**/build/test-results/**/*.xml"))
    if not files:
        emit("### Unit tests\n\nNo test result XML found.")
        return 0

    total = failures = errors = skipped = 0
    duration = 0.0
    failed_cases: list[tuple[str, str]] = []

    for path in files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue

        suites = [root] if root.tag == "testsuite" else root.iter("testsuite")
        for suite in suites:
            total += int(suite.get("tests", 0))
            failures += int(suite.get("failures", 0))
            errors += int(suite.get("errors", 0))
            skipped += int(suite.get("skipped", 0))
            duration += float(suite.get("time", 0) or 0)

            for case in suite.iter("testcase"):
                # Must compare against None explicitly: an Element with no
                # children is falsy, so `a or b` would silently discard it.
                problem = case.find("failure")
                if problem is None:
                    problem = case.find("error")
                if problem is not None:
                    name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
                    message = (problem.get("message") or "").strip().splitlines()
                    failed_cases.append((name, message[0] if message else ""))

    passed = total - failures - errors - skipped
    verdict = "passing" if not (failures or errors) else "FAILING"

    lines = [
        "### Unit tests",
        "",
        f"**{passed}/{total} passing** in {duration:.1f}s — {verdict}",
        "",
        "| Passed | Failed | Errors | Skipped |",
        "|---:|---:|---:|---:|",
        f"| {passed} | {failures} | {errors} | {skipped} |",
    ]

    if failed_cases:
        lines += ["", "#### Failures", ""]
        for name, message in failed_cases[:25]:
            suffix = f" — {message}" if message else ""
            lines.append(f"- `{name}`{suffix}")
        if len(failed_cases) > 25:
            lines.append(f"- …and {len(failed_cases) - 25} more")

    emit("\n".join(lines))
    return 0


# ---------------------------------------------------------------------------
# Coverage
# ---------------------------------------------------------------------------


def counter(element: ET.Element, kind: str) -> tuple[int, int]:
    """Return (covered, total) for a direct-child counter of the given type."""
    for node in element.findall("counter"):
        if node.get("type") == kind:
            missed = int(node.get("missed", 0))
            covered = int(node.get("covered", 0))
            return covered, missed + covered
    return 0, 0


def pct(covered: int, total: int) -> float:
    return 100.0 * covered / total if total else 0.0


def module_of(package_name: str) -> str:
    parts = package_name.replace(".", "/").split("/")
    if len(parts) >= 4 and parts[3] in KNOWN_MODULES:
        return parts[3]
    return "app"


def build_coverage_markdown() -> str | None:
    report = find_kover_report()
    if report is None:
        return None

    try:
        root = ET.parse(report).getroot()
    except ET.ParseError:
        return None

    line_cov, line_total = counter(root, "LINE")
    branch_cov, branch_total = counter(root, "BRANCH")
    if line_total == 0:
        # Seen when koverXmlReport runs without the unit tests (it does not depend
        # on them): no Kover binaries exist, so the report is an empty skeleton.
        return (
            "### Coverage\n"
            "\n"
            "The Kover report is empty (0 lines). The unit tests probably did not run "
            "before `koverXmlReport` — make sure the workflow invokes `testDebugUnitTest` "
            "first."
        )
    overall = pct(line_cov, line_total)

    # Aggregate packages up to their owning Gradle module.
    modules: dict[str, list[int]] = {}
    gaps: list[tuple[str, int, float]] = []
    for package in root.findall("package"):
        name = package.get("name", "")
        covered, total = counter(package, "LINE")
        if total == 0:
            continue
        bucket = modules.setdefault(module_of(name), [0, 0])
        bucket[0] += covered
        bucket[1] += total
        gaps.append((name.replace("/", "."), total - covered, pct(covered, total)))

    gap_to_target = max(0.0, COVERAGE_TARGET - overall)
    headline = f"**{overall:.1f}%** line coverage ({line_cov:,}/{line_total:,} lines)"
    if gap_to_target > 0:
        headline += (
            f" — {gap_to_target:.1f} points below the {COVERAGE_TARGET:.0f}% target"
        )
    else:
        headline += f" — at or above the {COVERAGE_TARGET:.0f}% target"

    lines = [
        "### Coverage",
        "",
        headline,
        "",
    ]

    if branch_total:
        lines += [
            f"Branch coverage: **{pct(branch_cov, branch_total):.1f}%** "
            f"({branch_cov:,}/{branch_total:,})",
            "",
        ]

    lines += [
        "| Module | Line coverage | Covered | Uncovered |",
        "|---|---:|---:|---:|",
    ]
    for module in sorted(modules, key=lambda m: pct(*modules[m][:2])):
        covered, total = modules[module]
        lines.append(
            f"| `:{module}` | {pct(covered, total):.1f}% | {covered:,} | {total - covered:,} |"
        )

    # The most valuable thing this report can tell you is where to write the
    # next test, so rank packages by how many uncovered lines they hold.
    gaps.sort(key=lambda row: row[1], reverse=True)
    biggest = [row for row in gaps if row[1] > 0][:10]
    if biggest:
        lines += [
            "",
            "<details><summary>Largest coverage gaps</summary>",
            "",
            "| Package | Uncovered lines | Coverage |",
            "|---|---:|---:|",
        ]
        for name, uncovered, package_pct in biggest:
            lines.append(f"| `{name}` | {uncovered:,} | {package_pct:.1f}% |")
        lines += ["", "</details>"]

    return "\n".join(lines)


def summarise_coverage() -> int:
    markdown = build_coverage_markdown()
    if markdown is None:
        emit("### Coverage\n\nNo Kover report found.")
        return 0
    emit(markdown)
    return 0


def comment_coverage(pr_number: str) -> int:
    markdown = build_coverage_markdown()
    if markdown is None:
        print("No Kover report found; skipping comment.")
        return 0

    body = f"{COMMENT_MARKER}\n{markdown}"
    repo = os.environ.get("GITHUB_REPOSITORY", "")

    def gh(*args: str, stdin: str | None = None) -> str:
        return subprocess.run(
            ["gh", *args],
            check=True,
            capture_output=True,
            text=True,
            input=stdin,
        ).stdout

    # Reuse the existing comment so a long-lived pull request does not accumulate
    # one coverage report per push.
    existing = gh(
        "api",
        f"repos/{repo}/issues/{pr_number}/comments",
        "--jq",
        f'.[] | select(.body | contains("{COMMENT_MARKER}")) | .id',
    ).split()

    if existing:
        gh(
            "api",
            "--method",
            "PATCH",
            f"repos/{repo}/issues/comments/{existing[0]}",
            "-f",
            f"body={body}",
        )
    else:
        gh(
            "api",
            "--method",
            "POST",
            f"repos/{repo}/issues/{pr_number}/comments",
            "-f",
            f"body={body}",
        )
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2

    mode = sys.argv[1]
    if mode == "tests":
        return summarise_tests()
    if mode == "coverage":
        return summarise_coverage()
    if mode == "comment":
        if len(sys.argv) < 3:
            print("comment mode requires a pull request number")
            return 2
        return comment_coverage(sys.argv[2])

    print(f"Unknown mode: {mode}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
