# Milestone 3: Playback Core

## Goal

Introduce a production-viable playback architecture that can be reused by phone, Android Auto, and Wear OS.

## Main Work

- [x] Add Media3 player integration.
- [x] Introduce `MediaSession` / `MediaSessionService`.
- [x] Extract playback core into the `:playback` module (`PlaybackService`, `PlaybackConnection`, `PlaybackState`).
- [ ] Define playback queue model and queue source.
- [x] Support play, pause, seek, skip, and resume.
- [x] Sync playback progress with the server (fetch on play for resume; save every 15s while playing plus on pause, seek, chapter jump, track switch, and playback end).
- [x] Stop playback on logout: the logout flow awaits a final progress flush before stopping, with the session observer kept as an idempotent backstop.
- [ ] Persist playback position locally (server-only today; saves are skipped while offline).
- [x] Chapter navigation (next/previous chapter, play-from-chapter) via the domain `PlaybackPositionCalculator`.
- [x] Playback speed control (0.5x–4x) with a speed sheet.
- [ ] Sleep timer (footer action is a placeholder with no handler).
- [x] Define how playback state is exposed to UI surfaces.
- [x] Add notification and background playback behavior.

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
