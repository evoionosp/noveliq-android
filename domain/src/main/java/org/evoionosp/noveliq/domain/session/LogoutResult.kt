package org.evoionosp.noveliq.domain.session

/**
 * Outcome of [usecase.LogoutUserUseCase]. Logout is fail-open: every step runs
 * even when an earlier one throws, so [failedSteps] lists the steps that threw
 * (in execution order) purely for logging. An empty list means fully clean.
 */
data class LogoutResult(
    val failedSteps: List<LogoutStep>,
) {
    val isFullyClean: Boolean get() = failedSteps.isEmpty()
}
