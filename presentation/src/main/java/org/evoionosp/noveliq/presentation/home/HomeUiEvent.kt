package org.evoionosp.noveliq.presentation.home

sealed interface HomeUiEvent {
    data class ShowMessage(
        val messageResId: Int
    ) : HomeUiEvent
}
