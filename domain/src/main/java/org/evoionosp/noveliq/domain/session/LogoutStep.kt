package org.evoionosp.noveliq.domain.session

/**
 * The ordered steps of [usecase.LogoutUserUseCase]. The order is load-bearing:
 * playback stops first so its progress flush still has a valid token, and the
 * session is cleared before the caches are wiped so no server-gated writer
 * (catalog sync, token refresh) can repopulate anything mid-logout.
 */
enum class LogoutStep {
    STOP_PLAYBACK,
    CLEAR_SESSION,
    DELETE_DOWNLOADS,
    CLEAR_COVER_ART_CACHE,
    CLEAR_CATALOG_CACHE,
}
