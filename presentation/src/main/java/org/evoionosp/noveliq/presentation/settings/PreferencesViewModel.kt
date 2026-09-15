package org.evoionosp.noveliq.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.session.usecase.LogoutUserUseCase

@HiltViewModel
class PreferencesViewModel
    @Inject
    constructor(
        private val logoutUserUseCase: LogoutUserUseCase,
    ) : ViewModel() {
        private val _isLoggingOut = MutableStateFlow(false)
        val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

        fun logout(onComplete: () -> Unit) {
            if (_isLoggingOut.value) return
            viewModelScope.launch {
                _isLoggingOut.value = true
                try {
                    logoutUserUseCase()
                } finally {
                    _isLoggingOut.value = false
                }
                onComplete()
            }
        }
    }
