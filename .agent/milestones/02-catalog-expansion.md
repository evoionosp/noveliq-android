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
- The detail screen now exposes the `Play` action as the next natural entry point for playback work.
- The Home dashboard now reads Continue Listening from a Room-backed cache populated from Audiobookshelf personalized shelves.

## Next Work Inside This Milestone

- Polish the main browsing shell across the `Home`, `Library`, and `Authors` root destinations.
- Make the `Home` dashboard sections feel production-ready before adding more server-driven shelf types.
- Decide whether more detail data should be cached locally.
- Improve the detail model so playback does not depend on ad hoc network fetches.
- Keep preparing the detail screen to become the playback entry surface.
