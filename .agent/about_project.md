# About Noveliq Android

## Overview

Noveliq is an Android client for the Audiobookshelf server. The app is currently focused on the foundation: authenticating against a user-provided Audiobookshelf instance, discovering audiobook libraries, caching catalog data locally, and presenting the library contents in a modern Compose-based UI.

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

## Current Module Layout

- `app`: app shell, `Application`, app-level DI wiring, app-wide sync coordinator.
- `presentation`: Compose UI, screen ViewModels, app theme.
- `domain`: domain models, repository interfaces, use cases.
- `data`: repository implementations, Retrofit services, Room DAOs/entities, connectivity observation.
- `core`: shared session persistence types and helpers.

## What Exists Today

- Server URL validation and server health checks.
- Login flow against Audiobookshelf.
- Session persistence across app launches.
- Library fetch and local library cache.
- Audiobook list fetch for the selected library.
- Local Room-backed catalog state and sync status.
- Audiobook detail screen reachable from the home catalog.
- Chapter fetch for a selected audiobook via item-detail API call.
- Play action placeholder on the detail screen.
- Basic settings and appearance preferences UI.

## What Does Not Exist Yet

- Audiobook playback.
- Media session and notification controls.
- Background audio service.
- Download manager / offline storage.
- Playback progress sync.
- Real playback from the `Play` action.
- Download action and offline media storage.
- Bookmarks, sleep timer, playback speed.
- Android Auto and Wear OS surfaces.

## Architectural Intent

The codebase already uses module separation and repository/use-case boundaries, which is a good foundation. However, the project should evolve from a phone-first app with a single UI surface into a platform-capable media product. That means shared business logic must be reusable by multiple clients, and playback/download responsibilities must live in dedicated modules rather than being embedded into the phone UI layer.

The target direction is:

- Keep domain logic platform-agnostic where possible.
- Keep data access behind domain contracts.
- Avoid making phone UI modules the center of the system.
- Introduce dedicated playback and download modules before those features ship.
- Design local data models for sync, resume, and offline-first behavior early enough to avoid expensive rewrites later.
