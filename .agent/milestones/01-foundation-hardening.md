# Milestone 1: Foundation Hardening

Status: mostly complete

## Goal

Turn the current prototype foundation into a safer and cleaner base for future catalog, playback, download, Auto, and Wear work.

## Main Work

- Separate session state from catalog bootstrap state.
- Introduce a proper startup state model.
- Move `domain` toward a pure Kotlin layer with no Android dependencies.
- Remove or redact sensitive debug logging.
- Move session storage to encrypted persistence.
- Reduce direct presentation dependency on implementation details such as DataStore-backed session types.
- Start moving toward feature-based navigation with Navigation Compose.
- Define target ownership of session, connectivity, and app-wide coordination abstractions.
- Replace hardcoded dispatcher usage with injected dispatchers in repositories and other async classes.
- Introduce `SharedFlow` for one-off UI events where the app currently mixes transient events into state.

## Important Notes

- This milestone should avoid large-scale rewrites.
- The goal is to correct the direction before more features are stacked on top.
- If a refactor is not needed to support an immediate follow-up milestone, prefer the smaller change.

## Exit Criteria

- Startup can distinguish authenticated-but-offline from unauthenticated.
- Session secrets are handled more safely.
- Data/domain async APIs are main-safe and dispatcher usage is testable.
- Navigation is ready to expand beyond a few hard-coded screens.
- Presentation no longer depends directly on low-level persistence details where avoidable.
- Domain direction is clearly pure Kotlin even if the module split is still incremental.

## Risks

- Over-refactoring too early.
- Changing too many module boundaries at once.

## Suggested Follow-Up

- Begin catalog expansion with clearer route and state boundaries.

## Completed In Repo

- Startup/auth/catalog state separation is in place.
- Navigation Compose is now used for root navigation.
- Session storage is encrypted and owned by `:core`.
- Debug logging is safer for authenticated traffic.
- Main repository/coordinator paths now use injected dispatchers.
- Key transient UI messages use `SharedFlow`.
- The connectivity contract no longer lives in `data`.
- `common` has been renamed to `core`.
