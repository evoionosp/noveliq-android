package org.evoionosp.noveliq.presentation.auth

sealed interface AuthUiEvent {
    data class ShowMessage(
        val messageResId: Int
    ) : AuthUiEvent
}
