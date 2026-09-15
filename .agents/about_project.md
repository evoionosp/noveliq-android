# About Noveliq Android

## Overview

Noveliq is an Android client for the Audiobookshelf server. The app authenticates against a user-provided Audiobookshelf instance, discovers audiobook libraries, caches catalog data locally, presents the library contents in a modern Compose-based UI, and now supports real audio playback of selected audiobooks.

The long-term product direction is broader than a basic Android phone app. The architecture should support:

- Phone and tablet Android experiences.
- Android Auto support.
- Wear OS support.
- Offline downloads for audiobook listening.
- Playback progress sync and other playback-related features.

## Current Technical Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture style: layered MVVM
- Dependency injection: Hilt
- Async/reactive state: Kotlin coroutines + Flow + StateFlow
- Networking: Retrofit + OkHttp
- Local persistence: Room
- Session persistence: encrypted shared preferences in `:data` (`SessionDataStore`, contracts in `:domain/session`)
- Playback: AndroidX Media3 (ExoPlayer + `MediaLibraryService`/`MediaSession`)

## Current Module Layout

- `app`: app shell, `Application`, app-level DI wiring, app-wide sync coordinator.
- `playback`: surface-agnostic playback core — Media3 service (`PlaybackService`), `MediaController` adapter (`PlaybackConnection`), shared `PlaybackState`, playback DI (`PlaybackModule`).
- `presentation`: Compose UI, screen ViewModels, app theme, navigation, and the now-playing surfaces (`presentation.player`: bar, overlay, full screen, chapters/speed sheets).
- `domain`: domain models, repository interfaces, use cases.
- `data`: repository implementations, Retrofit services, Room DAOs/entities, connectivity observation.

## What Exists Today

- Server URL validation and server health checks.
- Login flow against Audiobookshelf.
- Session persistence across app launches.
- Library fetch and local library cache.
- Audiobook list fetch for the selected library.
- Local Room-backed catalog state and sync status.
- Server-backed Continue Listening synced from the items-in-progress API and cached locally, with per-book playback positions and time-left-at-1x card subtitles.
- Ordered, fail-open logout flow (stop playback, clear session, wipe caches/downloads), confirmed via a Settings dialog; only the last server URL and appearance settings survive.
- Audiobook detail overlay reachable from the home catalog (glance vs playing states in `NowPlayingUiState`; there is no detail destination in the Navigation Compose graph).
- Chapter fetch for a selected audiobook via item-detail API call, cached in Room along with ordered remote tracks.
- Real audio playback from the `Play` action using Media3 (ExoPlayer + MediaSession).
- Background playback with a media notification and system media controls.
- Now-playing surfaces (bar, overlay, full screen) kept in sync via shared playback state.
- Server-side playback progress sync: resume from saved position on play; save every 15s while playing plus on pause, seek, chapter jump, track switch, and playback end.
- Chapter navigation (next/previous, play-from-chapter) and playback speed control (0.5x–4x).
- Basic settings and appearance preferences UI.

## What Does Not Exist Yet

- Playback queue model and queue UI (tracks play in order, but there is no queue management yet).
- Local playback progress persistence (progress syncs with the server; nothing is cached locally and saves are skipped while offline).
- Download manager / offline storage (a `DownloadStore` seam exists for logout wiping; no implementation yet).
- Download action and offline media storage.
- Local-file playback source resolution (remote streaming only today).
- Bookmarks and sleep timer (the sleep action in the player footer is a placeholder with no handler).
- Library search and filtering (the search FAB is a `TODO` placeholder).
- Android Auto and Wear OS surfaces.

## Architectural Intent

The codebase already uses module separation and repository/use-case boundaries, which is a good foundation. However, the project should continue evolving from a phone-first app with a single UI surface into a platform-capable media product. That means shared business logic must be reusable by multiple clients, and playback/download responsibilities must be structured so they can be shared across surfaces rather than embedded into the phone UI layer.

Playback core lives in the `:playback` module (`PlaybackService`, `PlaybackConnection`, `PlaybackState`, `PlaybackModule`); now-playing UI surfaces stay in the `presentation.player` package. The connection owns the `MediaController` and delegates position math, chapter navigation, resume, and progress-save policy to domain (`PlaybackPositionCalculator` plus the fetch/save progress use-cases), so the rules stay reusable across phone, Auto, and Wear without further extraction.

The target direction is:

- Keep domain logic platform-agnostic where possible.
- Keep data access behind domain contracts.
- Avoid making phone UI modules the center of the system.
- Extract playback (and later downloads) into dedicated capability modules before those surfaces expand.
- Design local data models for sync, resume, and offline-first behavior early enough to avoid expensive rewrites later.
