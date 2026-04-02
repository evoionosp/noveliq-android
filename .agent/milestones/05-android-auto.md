# Milestone 5: Android Auto

## Goal

Expose Noveliq through Android Auto using the same underlying playback and browsing architecture as the phone app.

## Main Work

- Choose Android Auto integration path based on the playback architecture.
- Expose browse tree / media catalog suitable for driving context.
- Integrate transport controls with shared playback session.
- Keep Auto interactions simple, safe, and playback-focused.
- Validate login/session assumptions for in-car usage.

## Important Notes

- Android Auto should not introduce a second playback stack.
- Browse content must be intentionally simplified.
- Offline availability will be especially valuable in car usage.

## Exit Criteria

- User can browse appropriate audiobook content in Auto.
- User can start and control playback in Auto.
- Auto uses the shared playback/session architecture rather than custom one-off logic.

## Dependencies

- Playback core.
- Preferably offline support for better in-car reliability.

## Risks

- Designing Auto before the media session model is stable.
