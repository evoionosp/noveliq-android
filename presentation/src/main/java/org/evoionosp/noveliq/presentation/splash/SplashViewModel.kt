package org.evoionosp.noveliq.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.common.session.SessionDataStore
import org.evoionosp.noveliq.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.noveliq.domain.library.usecase.BootstrapHomeCatalogUseCase

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val bootstrapHomeCatalogUseCase: BootstrapHomeCatalogUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionDataStore.session.collect { session ->
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = null,
                            isCatalogReady = false
                        )
                    }
                    return@collect
                }

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        session = session,
                        isCatalogReady = false
                    )
                }

                val bootstrapResult = bootstrapHomeCatalogUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        isCatalogReady = bootstrapResult is BootstrapHomeCatalogResult.Success
                    )
                }
            }
        }
    }
}
