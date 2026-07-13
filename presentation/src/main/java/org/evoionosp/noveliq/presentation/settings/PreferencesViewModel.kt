package org.evoionosp.noveliq.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.session.SessionStore

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val sessionStore: SessionStore
) : ViewModel() {
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionStore.clearSession()
            onComplete()
        }
    }
}
