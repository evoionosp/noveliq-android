# Road Map

## Intent

This roadmap is designed for the current actual state of the project: login and catalog fetch are in place, while playback, downloads, Android Auto, and Wear OS are future work.

The goal is not to add features as quickly as possible. The goal is to add them in an order that avoids architectural rework.

## Current Progress

- Milestone 1 is largely complete.
- Milestone 2 has started.
- The app now has a real detail route and chapter loading, but playback and downloads have not started.

## Cross-Cutting Standards

All milestones should follow these rules consistently:

- Keep `domain` pure Kotlin and independent from Android framework concerns.
- Treat repositories as the single source of truth and prefer offline-first data flow.
- Make all data and domain APIs main-safe.
- Inject dispatchers rather than hardcoding them inside classes.
- Use `StateFlow` for persistent screen state and `SharedFlow` for one-off UI events.
- Use Navigation Compose for expanding multi-screen flows.
- Pass route IDs, not full models.
- Prefer testing state emission and repository behavior rather than implementation details.

## Milestones

### Milestone 1: Foundation Hardening

Stabilize startup state handling, clean up boundaries, make domain/data APIs align with Android architecture guidance, improve security handling, and prepare the app for growth.

Status: mostly complete

Details: `milestones/01-foundation-hardening.md`

### Milestone 2: Catalog Expansion

Move from basic library listing to a more complete browsing foundation with better models, Navigation Compose routes, and detail-oriented catalog flows.

Status: in progress

Details: `milestones/02-catalog-expansion.md`

### Milestone 3: Playback Core

Introduce Media3-based playback, shared playback state, and the service/session architecture needed by all future surfaces.

Details: `milestones/03-playback-core.md`

### Milestone 4: Offline Downloads

Add durable download orchestration, local media management, and offline playback support.

Details: `milestones/04-offline-downloads.md`

### Milestone 5: Android Auto

Expose the shared playback and browsing capabilities through Android Auto.

Details: `milestones/05-android-auto.md`

### Milestone 6: Wear OS

Add a Wear-friendly experience built on the same playback and sync core.

Details: `milestones/06-wear-os.md`

### Milestone 7: Quality and Release Readiness

Harden testing, performance, analytics/logging strategy, migration strategy, and production readiness using the same architecture and coroutine standards established earlier.

Details: `milestones/07-quality-and-release.md`

## Recommended Order

1. Foundation hardening
2. Catalog expansion
3. Playback core
4. Offline downloads
5. Android Auto
6. Wear OS
7. Quality and release hardening

## Why This Order

- Playback should come before Android Auto and Wear OS because both depend on a proper shared playback core.
- Downloads should come after playback architecture exists, otherwise local media support will be bolted on awkwardly.
- Catalog expansion should come before playback so book details and richer metadata have a place to live.
- Foundation hardening comes first because it is where pure-domain boundaries, dispatcher injection, startup state modeling, and navigation direction are corrected.
- Quality hardening should happen continuously, but there should also be an explicit stabilization milestone before broader release.

## Immediate Next Focus

1. Finish polishing the main browsing surfaces: `Home`, `Library`, and `Authors`, including stronger navigation structure and production-ready section design.
2. After the home/dashboard polish pass, add server-backed `Continue Listening` sync and cache it locally so Room remains the source of truth for the home screen.
3. Decide whether audiobook detail data should remain partially network-backed or become more fully cached locally.
4. Keep the chapter-capable detail screen as the starting point for later playback entry and download actions.
5. Start the first real playback architecture slice only after the browsing shell and near-term home data model are stable.
