# Milestone 3: Playback Core

## Goal

Introduce a production-viable playback architecture that can be reused by phone, Android Auto, and Wear OS.

## Main Work

- Add Media3 player integration.
- Introduce `MediaSession` / `MediaSessionService`.
- Define playback queue model and queue source.
- Support play, pause, seek, skip, and resume.
- Persist playback position.
- Define how playback state is exposed to UI surfaces.
- Add notification and background playback behavior.

## Important Notes

- Playback should not live inside a screen ViewModel.
- The playback core should be surface-agnostic.
- The same playback architecture should power phone UI, notifications, Android Auto, and Wear.

## Exit Criteria

- User can start and control playback from the app.
- Playback continues correctly in background.
- Session and playback state are exposed through a shared architecture usable by other app surfaces.

## Dependencies

- Catalog expansion should provide enough item detail to resolve playable media.

## Risks

- Starting playback work without first deciding how remote media files are modeled.
- Coupling player APIs to Compose UI state too early.
