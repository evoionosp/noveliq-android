# Milestone 2: Catalog Expansion

Status: in progress

## Goal

Evolve from a basic list of audiobooks into a richer browsing foundation that can later feed playback and downloads.

## Main Work

- Add real app navigation with Navigation Compose.
- Introduce book details route and state.
- Expand domain/data models to represent richer item metadata.
- Decide what data belongs in summary models versus detail models.
- Improve search and filtering structure.
- Add refresh and retry behavior with clearer user feedback.
- Define catalog sync behavior more explicitly.
- Pass IDs through routes and resolve detail data from repositories/ViewModels.

## Important Notes

- The catalog model should not be limited to what the home screen needs today.
- Avoid shaping the media model around only the first UI list.
- Keep future playback requirements in mind when designing detail data.

## Exit Criteria

- The app supports navigating from library list to book detail context.
- Catalog state is represented clearly across loading, stale, offline, and error conditions.
- Models are rich enough to support playback work without immediate replacement.
- Route contracts remain small and stable rather than passing complex objects between screens.

## Dependencies

- Milestone 1 foundation cleanup.

## Risks

- Building detail flows on top of models that still only represent list items.

## Started In Repo

- Navigation from the home catalog to an audiobook detail screen is implemented.
- Route arguments use IDs rather than passing large models.
- The detail screen shows cached summary metadata from the local catalog model.
- Chapters are fetched from the Audiobookshelf item-detail API and shown at the bottom of the detail screen.
- The detail screen exposes a `Play` action that is now wired to the Media3 playback core (see Milestone 3).
- The Home dashboard now reads Continue Listening from a Room-backed cache populated from Audiobookshelf personalized shelves.
- Expanded item detail is now persisted in Room, including detail metadata, chapters, and ordered remote audio tracks from Audiobookshelf item detail responses.
- The detail screen observes the cached expanded detail model and refreshes it through the repository, rather than treating chapters as network-only UI state.
- Home/catalog UI has been decomposed from one large `HomeScreen.kt` file into dedicated screen and component files.
- Audiobook detail and now-playing UI have also been split into focused screen/component/helper files.
- Root app navigation has been extracted from `MainActivity` into `presentation.navigation`, including route constants, transition helpers, root bottom navigation, scaffold state, and the NavHost.

## Next Work Inside This Milestone

- Polish the main browsing shell across the `Home`, `Library`, and `Authors` root destinations.
- Make the `Home` dashboard sections feel production-ready before adding more server-driven shelf types.
- Add search/filter affordances for the library surface.
- Decide whether author/series detail should become first-class routes.

## Milestone 2.5: Playback-Ready Detail Cache

Status: implemented as a focused bridge slice

Completed:

- Added pure domain models for expanded audiobook detail and ordered tracks.
- Added Room entities and DAO support for cached audiobook detail, chapters, and tracks.
- Added a schema migration from database version 2 to 3.
- Expanded Audiobookshelf item-detail DTO mapping for `metadata.description` and `media.tracks`.
- Replaced network-only chapter loading with repository-owned detail refresh into Room.

Outcome:

- The cached detail/track model became the source that the Milestone 3 player resolves playable media from.

Remaining playback concerns (now tracked in Milestone 3):

- Define playback queue/session models using cached tracks.
- Decide how to represent playback progress and resume position.
- Decide whether the player continues to use Audiobookshelf remote track URLs directly or starts a server playback session first.
