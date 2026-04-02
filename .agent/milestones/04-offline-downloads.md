# Milestone 4: Offline Downloads

## Goal

Enable users to download audiobooks onto the device for offline listening with durable, resumable behavior.

## Main Work

- Define download entities and local file ownership.
- Introduce download state model: queued, downloading, paused, completed, failed, removing.
- Build durable background orchestration.
- Support cancellation, retry, and cleanup.
- Resolve playback source between remote and local media.
- Track storage usage and integrity.
- Decide partial download behavior and redownload strategy.

## Important Notes

- Download architecture must survive process death.
- Local media must be represented in the data layer explicitly.
- Download logic should not be hidden inside UI code or ad hoc services.

## Exit Criteria

- Users can download supported audiobook content.
- Offline playback works when network is unavailable.
- Download state survives app restarts.

## Dependencies

- Playback core must exist first.

## Risks

- Underestimating data-model changes required for track/file-level media.
- Treating downloads as a UI enhancement instead of a storage and sync subsystem.
