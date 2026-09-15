# Milestone 6: Wear OS

## Goal

Provide a practical Wear OS companion experience for quick control and lightweight browsing.

## Main Work

- Decide whether Wear is controller-first, playback-capable, or both.
- Add Wear-specific UI flows for current playback, queue, and recent items.
- Reuse session and playback state from shared modules.
- Define sync expectations between phone and watch.
- Evaluate whether offline-on-watch is in scope or deferred.

## Important Notes

- Wear should reuse shared business and playback logic as much as possible.
- The UX should be intentionally smaller and faster than phone.
- Do not mirror the full phone catalog blindly.

## Exit Criteria

- Wear can control or access playback through a dedicated Wear experience.
- Shared architecture supports Wear without substantial duplication.

## Dependencies

- Playback core.
- Clear decision on whether Wear supports standalone/offline behavior.

## Risks

- Starting Wear UI before product scope is defined.
- Creating watch-specific logic that forks from the main playback model.
