# Target Architecture

## Goals

The architecture should allow Noveliq to grow from a phone catalog client into a media app with multiple Android surfaces:

- Phone/tablet app
- Android Auto
- Wear OS
- Background playback
- Offline downloads

## Target Principles

- Domain logic should be pure Kotlin and should not depend on Android framework APIs.
- UI modules should consume domain contracts, not data-store or network implementation details.
- Playback should be a shared capability, not a phone-screen feature.
- Downloads should be durable background work, not tied to UI lifecycle.
- Local persistence should be designed for sync, resume, and offline use.
- Repositories should act as the single source of truth for data exposed to features.
- Main-safe APIs and injected dispatchers should be standard across data and domain work.

## Recommended Module Direction

Suggested long-term structure:

- `:app-phone`
- `:app-auto`
- `:app-wear`
- `:feature-auth`
- `:feature-catalog`
- `:feature-settings`
- `:feature-book-details`
- `:feature-playback`
- `:feature-downloads`
- `:core-domain`
- `:core-data`
- `:core-database`
- `:core-network`
- `:core-session`
- `:core-playback`
- `:core-common`

This does not need to happen all at once. The current codebase can move there incrementally.

## Near-Term Adaptation From Current Modules

Current modules can evolve as follows:

- `domain` becomes the pure domain contract layer.
- `data` keeps repository implementations and persistence/network adapters.
- `presentation` should eventually split into feature-oriented UI modules.
- `core` should stay narrow and intentional rather than becoming a generic shared bucket.
- `app` should remain a thin app shell.
- navigation should move to Navigation Compose before the route graph expands significantly.

Current note:

- `common` has already been renamed to `core`.
- `core` currently contains session persistence only.
- Additional shared capabilities should become dedicated core modules when they justify their own boundaries.

## Playback Direction

When playback starts, it should be built around:

- Media3 player
- MediaSession / MediaSessionService
- shared playback controller abstractions
- shared queue/progress persistence

Android Auto and Wear OS should integrate with the same playback/session core rather than duplicating playback logic.

## Download Direction

Downloads should be built around:

- dedicated download data models
- durable background orchestration, likely WorkManager plus foreground service where required
- resumable file storage strategy
- integrity checks and disk accounting
- mapping between local files and remote library items

## Data Model Direction

Before playback and downloads ship, the app will need richer local models for:

- library item details
- tracks/files
- chapters
- playback progress
- bookmarks
- download state
- remote/local media source resolution

The current `Audiobook` list model plus network-fetched chapter detail is useful for the current catalog flow, but it is not sufficient as the central media model for the full product.
