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
- Session persistence: encrypted shared preferences in `:core`
- Playback: AndroidX Media3 (ExoPlayer + `MediaLibraryService`/`MediaSession`)

## Current Module Layout

- `app`: app shell, `Application`, app-level DI wiring, app-wide sync coordinator.
- `presentation`: Compose UI, screen ViewModels, app theme, navigation, and the Media3 playback surfaces/service (`presentation.player`).
- `domain`: domain models, repository interfaces, use cases.
- `data`: repository implementations, Retrofit services, Room DAOs/entities, connectivity observation.
- `core`: shared session and settings persistence types and helpers.

## What Exists Today

- Server URL validation and server health checks.
- Login flow against Audiobookshelf.
- Session persistence across app launches.
- Library fetch and local library cache.
- Audiobook list fetch for the selected library.
- Local Room-backed catalog state and sync status.
- Server-backed Continue Listening synced from personalized shelves and cached locally.
- Audiobook detail screen reachable from the home catalog.
- Chapter fetch for a selected audiobook via item-detail API call, cached in Room along with ordered remote tracks.
- Real audio playback from the `Play` action using Media3 (ExoPlayer + MediaSession).
- Background playback with a media notification and system media controls.
- Now-playing surfaces (bar, overlay, full screen) kept in sync via shared playback state.
- Basic settings and appearance preferences UI.

## What Does Not Exist Yet

- Playback queue model and queue source.
- Playback progress persistence and progress sync with the server.
- Download manager / offline storage.
- Download action and offline media storage.
- Local-file playback source resolution (remote streaming only today).
- Bookmarks, sleep timer, playback speed.
- Library search and filtering.
- Android Auto and Wear OS surfaces.

## Architectural Intent

The codebase already uses module separation and repository/use-case boundaries, which is a good foundation. However, the project should continue evolving from a phone-first app with a single UI surface into a platform-capable media product. That means shared business logic must be reusable by multiple clients, and playback/download responsibilities must be structured so they can be shared across surfaces rather than embedded into the phone UI layer.

Playback currently lives in the `presentation.player` package (Media3 service plus a `PlaybackConnection` and now-playing UI). It is service-backed rather than screen-ViewModel-backed, but it is not yet extracted into a dedicated surface-agnostic playback module. That extraction should happen before Android Auto and Wear OS work begins.

The target direction is:

- Keep domain logic platform-agnostic where possible.
- Keep data access behind domain contracts.
- Avoid making phone UI modules the center of the system.
- Extract playback (and later downloads) into dedicated capability modules before those surfaces expand.
- Design local data models for sync, resume, and offline-first behavior early enough to avoid expensive rewrites later.
