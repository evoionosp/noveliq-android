# Current State

## Summary

Noveliq now has a materially better foundation than the initial prototype, and it has crossed from a catalog-only app into one with real audio playback. It supports login, server checks, library discovery, local caching, selected-library persistence through local DB state, listing audiobook items for the selected library, a detail flow with server-backed detail/chapter/track loading cached locally, and Media3-based playback of selected audiobooks.

The remaining gap to a full playback product is not the player itself but the durable pieces around it: playback progress persistence, progress sync with the server, a queue model, and offline downloads.

## Implemented Features

- Enter custom Audiobookshelf server URL.
- Validate server reachability and health.
- Authenticate with username and password.
- Persist login session locally.
- Fetch audiobook libraries.
- Select one library.
- Fetch audiobook list for the selected library.
- Cache libraries and audiobooks in Room.
- Sync server-backed Continue Listening from Audiobookshelf personalized shelves and cache it locally.
- Show sync status for current library.
- Navigate from the catalog grid to an audiobook detail screen.
- Fetch and display audiobook chapters from the Audiobookshelf item-detail API.
- Cache expanded audiobook detail, chapters, and ordered remote tracks in Room as a playback-ready catalog model.
- Real audio playback support using AndroidX Media3 (ExoPlayer + MediaSession).
- Background playback with system media controls and notification.
- Real-time playback synchronization between UI surfaces (Bar, Overlay, Screen).
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
- Playback is service-backed via a Media3 `MediaLibraryService`, not embedded in a screen ViewModel.

### Important Weaknesses

#### 1. Playback exists but is not yet durable or complete

Playback is implemented with Media3 (ExoPlayer + `MediaLibrarySession`), exposed to the UI through `PlaybackConnection`/`PlaybackState` and the now-playing surfaces, and backed by a foreground `mediaPlayback` service. However, several production-critical pieces are missing:

- No playback progress persistence: position is held in memory only and is lost on process death.
- No progress sync with the Audiobookshelf server.
- No playback queue/queue-source model.
- Streaming from remote track URLs only; no local-file source resolution.

Impact:

- Resume-across-sessions and cross-device progress are not yet possible.
- The player is usable but not yet a complete listening experience.

#### 2. Downloads are still not implemented

There is still no offline download pipeline, file storage strategy, download state model, or source selection between local and remote audio.

Impact:

- Offline listening is not yet possible.
- Download support should be designed to plug into the existing playback source resolution.

#### 3. Catalog detail data is cached, but still needs richer playback semantics

The audiobook detail screen observes a Room-backed expanded detail model and refreshes item detail from the server. Chapters and ordered remote tracks are cached locally, which gives playback a stable catalog source.

Impact:

- Playback resolves cached detail and remote track URLs from the repository layer.
- The model still needs progress, bookmarks, local-file resolution, and eventual download state before offline playback.

#### 4. Sync orchestration is still app-process scoped

Catalog refresh is still coordinated from `Application` with an application-scoped coroutine pattern. This is better structured than before, but it is still not a durable background execution model.

Impact:

- Still not a good base for downloads or durable progress sync.
- Still not the right mechanism for durable retries or process-death-safe work.

#### 5. The module layout is cleaner, but still transitional

The rename from `common` to `core` is an improvement, and playback lives in its own `presentation.player` package, but the app still uses broad modules like `presentation` and `data` rather than feature-oriented or capability-oriented splits. Playback is not yet a standalone surface-agnostic module.

Impact:

- Acceptable for the current project size.
- Playback should be extracted into a dedicated module before Auto and Wear expand the codebase significantly.

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
- Room-backed expanded audiobook detail cache with chapters and ordered remote tracks.
- Server-backed Continue Listening shelf sync cached locally for Room-first home reads.
- Presentation UI files have been split into screen-specific and component-specific Kotlin files for home/catalog, detail, and now-playing surfaces.
- App navigation, route definitions, transition helpers, root bottom navigation, and now-playing scaffold state have been extracted from `MainActivity` into a dedicated `presentation.navigation` package.
- Real Media3 playback wired behind the detail `Play` action, with a background media session service and now-playing surfaces synchronized through shared playback state.

## Current Product Fit

### Good enough for current scope

- Login and server validation.
- Initial catalog browsing.
- Basic cached library experience.
- Detail-oriented catalog browsing.
- Streaming playback of selected audiobooks with background controls.

### Not ready yet for future scope

- Playback progress persistence and resume.
- Progress sync with the server.
- Playback queue management.
- Offline downloads.
- Android Auto.
- Wear OS.

## Recommended Immediate Priorities

1. Persist playback position and add resume-from-last-position behavior.
2. Sync playback progress with the Audiobookshelf server.
3. Introduce a playback queue/queue-source model on top of the cached tracks.
4. Finish polishing the main authenticated browsing shell across `Home`, `Library`, and `Authors`, including search, filtering, and sort affordances.
5. Extract playback into a surface-agnostic module in preparation for Android Auto and Wear OS.
