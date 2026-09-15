> **Status: IMPLEMENTED (2026-09-15).** All six work units are done and verified
> (249 unit tests green, `assembleDebug`, lint, ktlint). Kept as the design
> record — see `../state.md` for current status.

## Goal

Design a single, ordered logout event flow: logging out stops playback, flushes
in-flight progress, and wipes the session, book caches, cover-art caches, user
data, and downloads — preserving only the last login server URL (so login can
still prefill it). Downloads don't exist yet, so the flow must include a seam
that future download code plugs into with zero changes to the logout path.

## Success Criteria

- Logging out while a book plays stops audio promptly, resets player UI, and a
  subsequent login — same or different server — never shows the previous user's
  books, covers, continue-listening, or progress.
- After logout, the only surviving persisted state is the last server URL (plus
  device-level appearance settings — see Key Decisions).
- All three logout paths (Settings, expired-session auto-logout, splash
  bootstrap) execute the same flow.
- Logout never strands the user: it completes and navigates to login even if an
  individual wipe step fails (fail-open).
- A future downloads feature integrates by implementing one interface; no
  logout-flow change required.

## Context And Current Facts

- Logout today is session-only: `SessionDataStore.clearSession()` removes the 6
  session keys and already preserves `last_server_url`
  (`data/.../session/SessionDataStore.kt:74-81`). Nothing else is cleared.
- Three logout call sites, all session-only: `PreferencesViewModel.logout()`
  (Settings), `HomeViewModel.clearExpiredSession()/expireSession()` (auto-logout
  on expired session), `SplashViewModel` (bootstrap). The latter two go through
  `ClearSessionUseCase`; Settings calls the store directly.
- Playback already stops on any logout via the `stopWhenSignedOut` session
  observer in `PlaybackConnection`, and `stopPlayback()` flushes progress to the
  server before stopping (`playback/.../PlaybackConnection.kt:111-117, 243-252`).
  The flush needs a valid token, so it must run *before* the session is
  cleared — today it runs after, via the observer, and likely fails silently.
- On-device inventory (verified by search this run):
  - Room `noveliq.db` v3: libraries, audiobooks, details/chapters/tracks,
    continue-listening, sync state — all server/user-scoped. Most DAOs already
    expose `deleteAll()`; `AudiobookDetailDao` has only per-book deletes.
  - Coil 2.7.0 (`gradle/libs.versions.toml`) disk + memory caches hold cover
    art fetched with a per-user Bearer header (`PlayerUiHelpers`) — per-user
    content. `MemoryCache.clear()` and `DiskCache.clear()` verified present on
    the project's own Coil sources jar.
  - `AppSettingsDataStore`: theme + dynamic color only — device preference.
  - Downloads: no code exists (case-insensitive grep over all modules is
    empty). No ExoPlayer download/media cache either.
  - No in-memory caches in the data repositories — a DB wipe fully covers
    catalog data.
- Every catalog-sync entry gates on `getValidSessionUseCase() ?: return`
  (`ObserveAndSyncCatalogUseCase`), so once the session is cleared no new sync
  write can start. Sync only triggers on connectivity-restore or library
  change, so an in-flight write racing a logout wipe is negligible — and
  ordering the wipe after the session clear closes it.
- Module direction constrains the design: `:playback` and `:data` both depend
  on `:domain`, and Coil lives in `:app`/`:presentation`. A domain logout
  orchestrator therefore cannot reference playback or Coil classes directly —
  it needs injected interfaces with Hilt-bound implementations in the owning
  modules.

## Constraints And Non-goals

- Stock behavior the user already approved stays: Material3 confirm dialog,
  title "Are you sure ?", Cancel + red Logout — only the description text
  becomes truthful about the wipe.
- Client-side only: no server token-revocation call. If the server ever offers
  revocation, it slots in as a new step-0; the ordering below already puts
  token-dependent work first.
- No downloads implementation — only the seam (interface + no-op) it will
  plug into.
- No migration or DB version bump: the wipe is data deletion, not schema
  change.

## Key Decisions

1. **One orchestrator: `LogoutUserUseCase` in `domain/session/usecase`.**
   All three call sites route through it. Rejected: an `:app`-module
   coordinator (less unit-testable, breaks the use-case convention) and
   scattering clear calls across ViewModels (how today's divergence happened).
2. **Fixed step order** (each step is an injected interface, see Recommended
   Approach):
   1. Stop playback + flush progress to server (needs the token).
   2. Clear session tokens (kills all server-gated writers: sync, refresh).
   3. Delete downloads (no-op seam for now).
   4. Clear Coil memory + disk caches.
   5. Wipe Room user tables.
   
   Session-after-flush/before-wipe is the load-bearing ordering: the progress
   flush keeps its token, and clearing the session before the wipe makes the
   wipe race-free against catalog sync.
3. **Fail-open with a result.** Each step runs best-effort (try/catch); the
   session clear is guaranteed. The use case returns a `LogoutResult` listing
   failed steps for logging; navigation to login always proceeds.
4. **Keep the `stopWhenSignedOut` observer as a backstop.** It is idempotent
   and already tested; any future path that clears the session directly still
   stops audio.
5. **DB wipe via `RoomDatabase.clearAllTables()`.** It covers future tables by
   default, which matters for a privacy-sensitive wipe; per-DAO `deleteAll`
   would silently miss each new table until logout code is touched. Exact
   signature is confirmed when the code compiles in the build step; fallback
   is the existing per-DAO `deleteAll()` methods plus a new
   `AudiobookDetailDao.deleteAll()`.
6. **Theme settings and last server URL survive.** Theme/dynamic-color are
   device-level, not user data — standard behavior is to keep them across
   logout. (Reversible: say so now if logout should reset theme too.)
7. **`ClearSessionUseCase` is removed** once all call sites move; a
   session-only clear stops existing as a public path so caches can't be left
   behind again.
8. **Settings logout shows progress.** `PreferencesViewModel` exposes a
   logout state (Idle/LoggingOut); the dialog disables its buttons and shows
   an indeterminate indicator while the flow runs.

## Recommended Approach

New domain API (all in `:domain`, no Android/framework imports):

- `LogoutUserUseCase(operator invoke(): LogoutResult)` — runs steps 1–5 in
  order, catching per-step failures, guaranteeing step 2.
- `LogoutResult(val failedSteps: List<LogoutStep>)` — empty means fully clean.
- Four small interfaces the use case depends on:
  - `PlayerLogoutHandler.onLogout()` — impl in `:playback`, delegates to the
    existing `PlaybackConnection.stopPlayback()` (flush-then-stop reuse
    as-is) while the session is still valid.
  - `DownloadStore.deleteAll()` — no-op impl in `:data` for now; the future
    downloads feature replaces the impl.
  - `CoverArtCache.clear()` — impl in `:app` next to `NoveliqApplication`
    (the module that owns the Coil `ImageLoader` and the Coil dependency),
    clearing `imageLoader.memoryCache` and `imageLoader.diskCache` on an IO
    dispatcher.
  - `LocalCatalogCleaner.clear()` — impl in `:data`, wrapping the
    `NoveliqDatabase` singleton already provided by `LocalDataModule`.

Call-site rewiring:

- `PreferencesViewModel.logout()` → inject `LogoutUserUseCase`, drive the new
  progress state, then `onComplete()` to login as today.
- `HomeViewModel.clearExpiredSession()/expireSession()` → same use case. This
  also fixes a latent bug: expired-session auto-logout currently leaves the
  full catalog cache, which a different user logging in next could briefly
  see.
- `SplashViewModel` → same use case; additionally wipes stale cache from a
  previous install state at bootstrap.
- Update the confirm-dialog description strings to truthfully list the wipe
  (cached books, covers, downloads when supported); keep title and buttons
  exactly as approved.

## Work Plan

1. **Domain orchestrator + contracts.** Add `LogoutUserUseCase`,
   `LogoutResult`/`LogoutStep`, and the four interfaces under
   `domain/.../session/`. Unit tests with fakes: verifies step order,
   per-step fail-open (each step throwing still runs the rest and still
   clears the session), and that the download seam is invoked.
2. **Data implementations.** `LocalCatalogCleaner` over `NoveliqDatabase`,
   no-op `DownloadStore`; Hilt bindings. Test the cleaner against an
   in-memory Room database following existing data-test patterns.
3. **Playback implementation.** `PlayerLogoutHandler` delegating to
   `PlaybackConnection.stopPlayback()`; Hilt binding; unit test that logout
   stops the controller and resets state.
4. **App Coil implementation.** `CoverArtCache` impl using the application
   `ImageLoader`; Hilt binding. (No JVM unit test expected — needs Android;
   covered by build + manual check.)
5. **Presentation rewiring + strings.** Move all three ViewModels to
   `LogoutUserUseCase`, add logout progress state to Settings, update dialog
   copy, delete `ClearSessionUseCase`, update/extend ViewModel tests.
6. **Full validation** (see Validation Plan).

## Validation Plan

- `JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true" ./gradlew
  testDebugUnitTest assembleDebug` — BUILD SUCCESSFUL, 0 failures (suite is
  ~228 tests today plus the new logout tests).
- ktlint on touched modules (repo's configured gate) — clean.
- New-test red/green proof for the orchestrator: fail-open and ordering tests
  must fail against a naive implementation (e.g. steps skipped on first
  exception) and pass on the real one.
- Manual pass (highest-risk validation — the Coil wipe has no JVM test):
  1. Play a book, log out from Settings → audio stops, player UI clears,
     login appears with server prefilled.
  2. Log back in (ideally a different server/user) → no stale books, covers,
     or continue-listening; catalog refills fresh.
  3. Log out offline/airplane mode → logout still completes to login.
  4. Expired-session auto-logout → cache wiped, next login starts clean.
  5. Confirm theme setting survives logout.

## Risks / Rollback

- **In-flight sync write landing after the wipe.** Negligible (requires a
  connectivity flip or library change mid-logout) and self-healing (next
  login refreshes). Accepted, no epoch counter.
- **`clearAllTables()` must not run inside a transaction** — the impl calls it
  directly on the database, never from a DAO transaction. Room enforces this
  at runtime; the in-memory test covers it.
- **Coil disk clear off the main thread** — impl dispatches to IO; a strict
  manual check on a low-end device covers jank.
- Rollback is per-unit revert: each work unit is independently revertable;
  units 1–4 are additive (new API + impls) and inert until unit 5 rewires the
  call sites.

## Open Questions

None — all material facts were verified in the repo (logout paths, Room
schema/DAOs, Coil version and clear APIs on the project's classpath, sync
gating, module dependencies).
