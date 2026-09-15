package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import org.evoionosp.noveliq.domain.session.CoverArtCache
import org.evoionosp.noveliq.domain.session.DownloadStore
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner
import org.evoionosp.noveliq.domain.session.LogoutResult
import org.evoionosp.noveliq.domain.session.LogoutStep
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler
import org.evoionosp.noveliq.domain.session.SessionStore

/**
 * Signs the user out: stops playback (flushing progress while the token is
 * still valid), clears the session, then wipes downloads, cover-art caches,
 * and the catalog database. Only the last server URL survives, inside the
 * session store, so login can prefill it.
 *
 * Every logout path (settings, expired-session auto-logout, splash bootstrap)
 * funnels through here so caches can never be left behind for the next user.
 *
 * Fail-open: each step runs even when an earlier one throws, so a broken wipe
 * can never strand the user signed in — failures are reported in
 * [LogoutResult.failedSteps] for logging. Coroutine cancellation is the one
 * exception: it propagates rather than being recorded, so a cancelled logout
 * stays cancelled.
 */
class LogoutUserUseCase
    @Inject
    constructor(
        private val playerLogoutHandler: PlayerLogoutHandler,
        private val sessionStore: SessionStore,
        private val downloadStore: DownloadStore,
        private val coverArtCache: CoverArtCache,
        private val localCatalogCleaner: LocalCatalogCleaner,
    ) {
        suspend operator fun invoke(): LogoutResult {
            // Order matters: flush-then-stop keeps its token, and clearing the
            // session before the wipes stops every server-gated writer (catalog
            // sync, token refresh) from repopulating anything mid-logout.
            val failedSteps = mutableListOf<LogoutStep>()
            runStep(LogoutStep.STOP_PLAYBACK, failedSteps) { playerLogoutHandler.onLogout() }
            runStep(LogoutStep.CLEAR_SESSION, failedSteps) { sessionStore.clearSession() }
            runStep(LogoutStep.DELETE_DOWNLOADS, failedSteps) { downloadStore.deleteAll() }
            runStep(LogoutStep.CLEAR_COVER_ART_CACHE, failedSteps) { coverArtCache.clear() }
            runStep(LogoutStep.CLEAR_CATALOG_CACHE, failedSteps) { localCatalogCleaner.clear() }
            return LogoutResult(failedSteps)
        }

        private suspend fun runStep(
            step: LogoutStep,
            failedSteps: MutableList<LogoutStep>,
            action: suspend () -> Unit,
        ) {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failedSteps += step
            }
        }
    }
