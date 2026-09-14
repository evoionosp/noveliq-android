# Current State

## Summary

Noveliq now has a materially better foundation than the initial prototype, and it has crossed from a catalog-only app into one with real audio playback. It supports login, server checks, library discovery, local caching, selected-library persistence through local DB state, listing audiobook items for the selected library, a detail flow with server-backed detail/chapter/track loading cached locally, and Media3-based playback of selected audiobooks.

The remaining gap to a full playback product is not the player itself but the durable pieces around it: local progress persistence (server sync already works online), a queue model/UI, and offline downloads.

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
- Open an audiobook detail overlay (glance mode) from the catalog grid — there is no detail route in the nav graph.
- Fetch and display audiobook chapters from the Audiobookshelf item-detail API.
- Cache expanded audiobook detail, chapters, and ordered remote tracks in Room as a playback-ready catalog model.
- Real audio playback support using AndroidX Media3 (ExoPlayer + MediaSession), with the core extracted into the `:playback` module.
- Background playback with system media controls and notification.
- Real-time playback synchronization between UI surfaces (Bar, Overlay, Screen).
- Server-side playback progress sync and resume-from-saved-position.
- Chapter navigation and playback speed control (0.5x–4x).
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

#### 1. Playback exists and syncs with the server, but is not yet durable offline or complete

Playback is implemented with Media3 (ExoPlayer + `MediaLibrarySession`), with the core extracted into the `:playback` module (`PlaybackService`, `PlaybackConnection`, `PlaybackState`) and exposed to the now-playing surfaces. Progress syncs with the Audiobookshelf server: resume position is fetched on play, and progress is saved every 15s while playing plus on pause, seek, chapter jump, track switch, and playback end. Chapter navigation and speed control (0.5x–4x) work. However, several production-critical pieces are missing:

- No local progress persistence: progress lives on the server only, saves are skipped while offline, and there is no Room entity or offline save queue.
- No playback queue/queue-source model or queue UI (tracks play in order; no queue management).
- Streaming from remote track URLs only; no local-file source resolution.

Impact:

- Resume-across-sessions and cross-device progress work when online, but progress made offline is lost.
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

#### 3.5 Session lifetime is now handled centrally

Audiobookshelf 2.26.0+ issues short-lived JWT access tokens with a longer-lived refresh token.
Token rotation is centralised in `SessionRefreshCoordinator` (domain) and driven from an OkHttp
`Authenticator` (`data.network.TokenAuthenticator`) plus a proactive `GetValidSessionUseCase`
that rotates an expired token before a request rather than after it 401s.

Rules that must be preserved:

- Only the coordinator clears the session, and only when the server definitively rejects the
  refresh token. Transient failures must never log the user out.
- The authenticated OkHttp client and the auth client (login / server check / refresh) are
  separate on purpose. See `NetworkModule`.
- Cache fallbacks must not absorb `CatalogError.AUTH`; serving stale content over a dead session
  is what made an expired token look like a working signed-in app.

#### 4. Sync orchestration is still app-process scoped

Catalog refresh is still coordinated from `Application` with an application-scoped coroutine pattern. This is better structured than before, but it is still not a durable background execution model.

Impact:

- Still not a good base for downloads or durable progress sync.
- Still not the right mechanism for durable retries or process-death-safe work.

#### 5. The module layout is cleaner, but still transitional

Session persistence lives in `:data` (`SessionDataStore`, encrypted) with contracts in `:domain/session`. Playback core is already extracted into `:playback`, with now-playing UI in `presentation.player`, but the app still uses broad modules like `presentation` and `data` rather than feature-oriented or capability-oriented splits.

Impact:

- Acceptable for the current project size.
- No further playback extraction is needed before Auto and Wear; new surfaces should reuse `:playback` plus the domain playback rules.

## What Was Improved Recently

- Startup/auth/catalog state separation.
- Dedicated authenticated catalog error route instead of routing failures back to auth.
- Root Navigation Compose graph.
- Encrypted session storage (`SessionDataStore` in `:data`).
- Safer debug logging for authenticated traffic.
- Injected dispatchers and better coroutine cancellation handling.
- `SharedFlow` for transient UI messages in key screens.
- `ConnectivityObserver` moved out of `data` contract ownership.
- Audiobook detail overlay (glance mode) with no detail route in the nav graph.
- Chapter loading from server item detail.
- Room-backed expanded audiobook detail cache with chapters and ordered remote tracks.
- Server-backed Continue Listening shelf sync cached locally for Room-first home reads.
- Presentation UI files have been split into screen-specific and component-specific Kotlin files for home/catalog, detail, and now-playing surfaces.
- App navigation, route definitions, transition helpers, root bottom navigation, and now-playing scaffold state have been extracted from `MainActivity` into a dedicated `presentation.navigation` package.
- Real Media3 playback wired behind the detail `Play` action, with a background media session service and now-playing surfaces synchronized through shared playback state.
- Centralised access-token refresh: `SessionRefreshCoordinator`, an OkHttp `Authenticator`, JWT
  expiry tracking on the stored session, and proactive rotation via `GetValidSessionUseCase`.
- The login form remembers the last server URL and protocol across logout, so signing back in
  does not start from a blank field.

## Current Product Fit

### Good enough for current scope

- Login and server validation.
- Initial catalog browsing.
- Basic cached library experience.
- Detail-oriented catalog browsing.
- Streaming playback of selected audiobooks with background controls.

### Not ready yet for future scope

- Local playback progress persistence and offline save queue (server sync and resume already work online).
- Playback queue management.
- Offline downloads.
- Android Auto.
- Wear OS.

## Recommended Immediate Priorities

1. Add local playback progress persistence (Room entity and/or offline save queue) so progress made offline is not lost.
2. Introduce a playback queue/queue-source model plus queue UI on top of the cached tracks.
3. Finish polishing the main authenticated browsing shell across `Home`, `Library`, and `Authors`, including search, filtering, and sort affordances.
4. Keep reusing the `:playback` module for Android Auto and Wear OS — no further extraction needed.
