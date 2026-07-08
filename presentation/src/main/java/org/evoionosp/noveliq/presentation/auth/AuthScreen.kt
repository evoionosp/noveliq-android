package org.evoionosp.noveliq.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.home.homeBackgroundBrush
import org.evoionosp.noveliq.presentation.navigation.LocalSnackbarHostState
import org.evoionosp.noveliq.presentation.navigation.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = LocalSnackbarHostState.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AuthUiEvent.ShowMessage -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                val message = context.getString(event.messageResId)
                if (message.isNotBlank()) {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
            .background(homeBackgroundBrush())
            .verticalScroll(scrollState)
    ) {
        TopAppBar(
            title = { },
            actions = {
                FilledTonalIconButton(onClick = onOpenSettings, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_icon_desc)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (state.serverStatus) {
                null -> {
                    WelcomeCard()
                    ConnectToServerCard(
                        state = state,
                        onProtocolChange = viewModel::onProtocolChange,
                        onUrlChange = viewModel::onBaseUrlChange,
                        onSubmit = {
                            keyboardController?.hide()
                            viewModel.checkLoginSetup()
                        },
                    )
                }
                else -> {
                    ServerStatusCard(state, onEdit = {
                        viewModel.clearServerState()
                    })
                }
            }

            if (state.showLoginFields) {
                Card(
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_credentials_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.username_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp)
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.password_label)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(22.dp)
                        )
                        Button(
                            onClick = viewModel::login,
                            enabled = !state.isChecking && !state.isLoggingIn,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isLoggingIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.logging_in))
                                } else {
                                    Text(stringResource(R.string.login_button))
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.auth_server_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
