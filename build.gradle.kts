import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    jacoco
}

val ktlintToolVersion = libs.versions.ktlintTool.get()

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<KtlintExtension> {
        version.set(ktlintToolVersion)
        ignoreFailures.set(false)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.SARIF)
        }
        filter {
            exclude { it.file.invariantSeparatorsPath.contains("/build/") }
        }
    }
}

// ---------------------------------------------------------------------------
// JaCoCo coverage: per-module HTML reports plus one merged project report.
//
// Per module:  ./gradlew :data:jacocoTestReport      -> <module>/build/reports/jacoco/test/html
// Whole app:   ./gradlew jacocoMergedReport          -> build/reports/jacoco/merged/html
//
// Both reports show line AND branch coverage per file, so an untested if/else
// branch shows up as a partially-covered (yellow) or missed (red) line.
// ---------------------------------------------------------------------------

/** Class-file patterns excluded from every coverage report (generated code). */
val jacocoExcludes =
    listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        // Hilt-generated factories for the hand-written *Module classes.
        "**/*Module_*.*",
        // Room-generated database/DAO implementations.
        "**/*Dao_Impl.class",
        "**/NoveliqDatabase_Impl.class",
        "**/hilt_aggregated_deps/**",
        "**/*Binding*.*",
        "**/BR.*",
    )

subprojects {
    apply(plugin = "jacoco")

    // Attach the JaCoCo agent to every unit-test task (required for Android
    // modules; the no-location-classes flag keeps JDK-internal classes from
    // breaking instrumentation on newer toolchains).
    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            isEnabled = true
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    // Per-module HTML + XML report, generated code filtered out.
    tasks.withType<JacocoReport>().configureEach {
        dependsOn("testDebugUnitTest")
        classDirectories.setFrom(
            files(classDirectories).asFileTree.matching {
                exclude(jacocoExcludes)
            },
        )
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }
}

// Merged report across every module's debug unit tests.
tasks.register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    description = "Merged JaCoCo HTML + XML coverage from all modules (debug unit tests)."

    dependsOn(subprojects.map { it.tasks.named("testDebugUnitTest") })

    executionData.setFrom(
        files(
            subprojects.map {
                it.layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
            },
        ),
    )
    classDirectories.setFrom(
        files(
            subprojects.map {
                it.layout.buildDirectory.dir("tmp/kotlin-classes/debug")
            } +
                subprojects.map {
                    it.layout.buildDirectory.dir("intermediates/javac/debug/classes")
                },
        ).asFileTree.matching {
            exclude(jacocoExcludes)
        },
    )
    sourceDirectories.setFrom(
        files(
            subprojects.map {
                it.layout.projectDirectory.dir("src/main/java")
            },
        ),
    )

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}
