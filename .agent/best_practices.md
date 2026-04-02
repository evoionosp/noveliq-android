# Best Practices

## Architecture

- Keep `domain` as pure Kotlin. No `android.*` imports and no AndroidX runtime dependencies unless there is a deliberate, documented exception.
- Keep repository interfaces in domain and implementations in data.
- Keep the data layer as the single source of truth for app data.
- Do not inject Room, Retrofit, DataStore, or platform services directly into Compose UI or screen ViewModels unless there is a very strong reason.
- Prefer feature-oriented modules over one large presentation module as the app grows.
- Keep `app` thin. Avoid putting business logic in `Application` or activities.

## State Modeling

- Model auth state, bootstrap state, sync state, and feature UI state separately.
- Do not use one boolean to represent multiple unrelated states.
- Treat startup as a state machine, not a sequence of ad hoc checks.
- Prefer explicit sealed models when state transitions matter.
- Expose screen state as read-only `StateFlow` backed by private `MutableStateFlow`.
- Use `SharedFlow` for one-off events such as navigation, snackbar messages, and transient UI actions.
- Do not expose `MutableStateFlow` or `MutableSharedFlow` publicly.

## Navigation

- Use Navigation Compose as the default navigation approach for all new multi-screen work.
- Keep route arguments small and stable.
- Pass IDs through navigation, not large objects.
- Let screens and ViewModels resolve data from IDs rather than passing models through routes.

## Data and Sync

- Design local DB schema for incremental evolution.
- Add migrations once schema changes begin.
- Prefer offline-first reads backed by local storage where practical.
- Use stale-while-revalidate behavior where it fits the product: show cached data quickly, then refresh in background.
- Keep sync metadata explicit: last synced, stale state, failure reason, retry policy.
- Separate catalog sync from media download sync.
- Main-safe suspend functions are required in data and domain APIs.
- Prefer `Flow` for observable data changes and `suspend` functions for one-shot operations.

## Security

- Do not log tokens, credentials, or authenticated response bodies.
- Store session secrets in encrypted storage.
- Keep auth header handling centralized.
- Validate and normalize base URLs consistently.

## Playback and Download Readiness

- Build playback around Media3, not UI-specific abstractions.
- Build download orchestration as durable background work.
- Persist playback position and queue state centrally.
- Treat local/offline media as first-class data, not a later patch.

## ViewModels and Use Cases

- ViewModels should coordinate UI state, not contain networking or storage details.
- Keep use cases small and focused.
- Prefer one clear use case over leaking repository logic into presentation.
- If logic is reused by multiple surfaces, move it below presentation early.
- Launch ViewModel work in `viewModelScope`.
- Do not expose suspend functions from ViewModels to UI callers.

## UI

- Keep UI composables stateless where practical.
- Hoist screen state into ViewModels.
- Use lifecycle-aware state collection.
- Use `collectAsStateWithLifecycle()` for screen state in Compose.
- Keep one-off event collection out of recomposition-sensitive code paths.
- Avoid making phone UI assumptions that would block tablet, Auto, or Wear reuse.

## Coroutines

- Inject dispatchers instead of hardcoding `Dispatchers.IO` or `Dispatchers.Default` inside classes.
- Never use `GlobalScope`.
- Do not swallow `CancellationException` inside broad exception handlers.
- Use structured concurrency for parallel work.
- Use an application-scoped coroutine scope only for truly app-lifetime work, and prefer durable background APIs when the work must survive process death.

## Networking

- Keep Retrofit service definitions declarative. Use `@Path`, `@Query`, and `@Body` instead of manual URL construction when possible.
- Handle HTTP and connectivity failures in repositories so UI state stays simple.
- Use `Response<T>` when status code handling matters.
- Keep auth header creation centralized rather than repeating `Bearer` formatting throughout UI or feature code.

## Testing

- Unit test domain use cases first.
- Add repository tests around cache and sync behavior.
- Add tests for startup state decisions and selection/sync edge cases.
- When playback and downloads are introduced, treat those areas as high-test-priority.
- Prefer testing emitted state and events over implementation details.
- Use coroutine test utilities such as `runTest` and test dispatchers.
- Use Flow testing utilities such as Turbine where helpful.
- Use MockK or similarly capable mocking only where fakes are not the clearer choice.

## Project Hygiene

- Keep README and `.agent` docs updated as the architecture evolves.
- Record major architectural decisions before large refactors.
- Prefer small, targeted changes over broad rewrites without milestones.
