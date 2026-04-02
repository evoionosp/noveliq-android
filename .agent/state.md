# Current State

## Summary

Noveliq is still early-stage, but it now has a materially better foundation than the initial prototype. The app supports login, server checks, library discovery, local caching, selected-library persistence through local DB state, listing audiobook items for the selected library, and a first-pass audiobook detail flow with server-backed chapter loading.

The project is not yet a playback app. It is currently a catalog and authentication app with a cleaner foundation and the first detail-oriented catalog flow in place.

## Implemented Features

- Enter custom Audiobookshelf server URL.
- Validate server reachability and health.
- Authenticate with username and password.
- Persist login session locally.
- Fetch audiobook libraries.
- Select one library.
- Fetch audiobook list for the selected library.
- Cache libraries and audiobooks in Room.
- Show sync status for current library.
- Navigate from the catalog grid to an audiobook detail screen.
- Fetch and display audiobook chapters from the Audiobookshelf item-detail API.
- Show a `Play` action placeholder on the detail screen.
- Basic settings and appearance screens.

## Current Architecture Assessment

### Good Foundations

- Separate modules already exist: `app`, `presentation`, `domain`, `data`, `core`.
- Domain layer defines repository interfaces and use cases.
- Data layer owns Retrofit and Room implementations.
- Compose UI uses screen ViewModels and `StateFlow`.
- The app already uses local cache instead of depending entirely on the network.
- Root navigation now uses Navigation Compose.
- Session storage is encrypted.
- Hardcoded dispatcher usage has been replaced in the main repository and coordinator paths.
- Snackbar-style transient messages have been moved to `SharedFlow` events in key screens.

### Important Weaknesses

#### 1. Playback is still not implemented

The detail screen now has a `Play` action, but it is still only a placeholder. There is no playback service, no Media3 integration, no media session, and no progress persistence.

Impact:

- Android Auto and Wear OS work cannot start properly until playback architecture exists.
- The current `Play` action is only a UI contract, not a feature.

#### 2. Downloads are still not implemented

There is still no offline download pipeline, file storage strategy, download state model, or source selection between local and remote audio.

Impact:

- Offline listening is not yet possible.
- Playback architecture should be designed with downloads in mind before the implementation gets too far.

#### 3. Catalog detail data is still split between local cache and network-only detail fetches

The audiobook detail screen uses cached local metadata for summary fields and fetches chapters on demand from the server. This is acceptable for the current stage, but it is not the final detail model.

Impact:

- The app does not yet persist full detail state locally.
- Playback and offline support will eventually need richer local detail models than the current list-derived `Audiobook` entity.

#### 4. Sync orchestration is still app-process scoped

Catalog refresh is still coordinated from `Application` with an application-scoped coroutine pattern. This is better structured than before, but it is still not a durable background execution model.

Impact:

- Still not a good base for downloads.
- Still not the right mechanism for durable retries or process-death-safe work.

#### 5. The module layout is cleaner, but still transitional

The rename from `common` to `core` is an improvement, but the app is still using broad modules like `presentation` and `data` rather than feature-oriented or capability-oriented splits.

Impact:

- Acceptable for the current project size.
- Will need further modularization before playback, downloads, Auto, and Wear expand the codebase significantly.

## What Was Improved Recently

- Startup/auth/catalog state separation.
- Dedicated authenticated catalog error route instead of routing failures back to auth.
- Root Navigation Compose graph.
- Encrypted session storage in `:core`.
- Safer debug logging for authenticated traffic.
- Injected dispatchers and better coroutine cancellation handling.
- `SharedFlow` for transient UI messages in key screens.
- `ConnectivityObserver` moved out of `data` contract ownership.
- `common` renamed to `core`.
- Audiobook detail route and screen.
- Chapter loading from server item detail.

## Current Product Fit

### Good enough for current scope

- Login and server validation.
- Initial catalog browsing.
- Basic cached library experience.
- Detail-oriented catalog browsing.

### Not ready yet for future scope

- Playback.
- Media session architecture.
- Offline downloads.
- Syncing listening progress.
- Android Auto.
- Wear OS.

## Recommended Immediate Priorities

1. Implement the first real playback slice behind the current `Play` action.
2. Define playback/session architecture that can later power Android Auto and Wear OS.
3. Decide how much audiobook detail should be cached locally versus fetched on demand.
4. Introduce download-domain models before adding offline behavior.
5. Continue splitting broad modules as feature/capability complexity grows.
