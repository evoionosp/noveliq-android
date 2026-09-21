package org.evoionosp.noveliq.presentation.settings

import android.security.KeyChain
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.evoionosp.noveliq.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConnectionScreen(
    onBackClick: () -> Unit,
    miniTrailPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: ConnectionSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var headerDialog by remember { mutableStateOf<HeaderDialogState>(HeaderDialogState.Hidden) }
    var showBypassConfirm by remember { mutableStateOf(false) }
    var showUserAgentSheet by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = SETTINGS_MIN_BAR_HEIGHT + statusBarHeight
    val maxTopBarHeight = SETTINGS_MAX_HEADER_HEIGHT

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f -
                ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(
                    0f,
                    1f,
                )
        }
    }

    val nestedScrollConnection =
        remember(listState, minTopBarHeightPx, maxTopBarHeightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val delta = available.y
                    val isScrollingDown = delta < 0

                    if (!isScrollingDown &&
                        (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
                    ) {
                        return Offset.Zero
                    }

                    val previousHeight = topBarHeight.value
                    val newHeight =
                        (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                    val consumed = newHeight - previousHeight

                    if (consumed.roundToInt() != 0) {
                        coroutineScope.launch {
                            topBarHeight.snapTo(newHeight)
                        }
                    }

                    val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                    return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
                }
            }
        }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val shouldExpand =
                topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand =
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

            val targetValue =
                if (shouldExpand && canExpand) {
                    maxTopBarHeightPx
                } else {
                    minTopBarHeightPx
                }

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(
                        targetValue,
                        spring(stiffness = Spring.StiffnessMedium),
                    )
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
    ) {
        val headerHeightDp = with(density) { topBarHeight.value.toDp() }
        val navigationBarHeight =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = headerHeightDp + 8.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = navigationBarHeight + 24.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.server_connection_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                ConnectionPreferenceGroup(
                    title = stringResource(R.string.server_connection_headers),
                ) {
                    if (uiState.headers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.server_connection_headers_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        )
                    } else {
                        uiState.headers.forEachIndexed { index, header ->
                            if (index > 0) {
                                SettingsDivider()
                            }
                            HeaderRow(
                                header = header,
                                onClick = { headerDialog = HeaderDialogState.Edit(header) },
                                onDelete = { viewModel.removeHeader(header.id) },
                            )
                        }
                    }
                    SettingsDivider()
                    FilledTonalButton(
                        onClick = { headerDialog = HeaderDialogState.Add },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.server_connection_add_header))
                    }
                }
            }

            item {
                ConnectionPreferenceGroup(
                    title = stringResource(R.string.server_connection_security),
                ) {
                    ConnectionSwitchRow(
                        label = stringResource(R.string.server_connection_bypass_ssl),
                        description = stringResource(R.string.server_connection_bypass_ssl_desc),
                        checked = uiState.bypassSsl,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showBypassConfirm = true
                            } else {
                                viewModel.setBypassSsl(false)
                            }
                        },
                    )
                    SettingsDivider()
                    val certAlias = uiState.clientCertificateAlias
                    ConnectionNavigationRow(
                        label = stringResource(R.string.server_connection_client_cert),
                        description =
                            certAlias
                                ?: stringResource(R.string.server_connection_client_cert_none),
                        trailing = {
                            if (certAlias != null) {
                                IconButton(
                                    onClick = { viewModel.setClientCertificateAlias(null) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription =
                                            stringResource(
                                                R.string.server_connection_client_cert_remove,
                                            ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            val host = activity ?: return@ConnectionNavigationRow
                            KeyChain.choosePrivateKeyAlias(
                                host,
                                { alias ->
                                    if (alias != null) {
                                        viewModel.setClientCertificateAlias(alias)
                                    } else {
                                        // Null means the user cancelled — or the device
                                        // has no client certificates, in which case
                                        // Android may return without showing any UI.
                                        // Either way, say so instead of going silent.
                                        // The callback can arrive off the main thread.
                                        host.runOnUiThread {
                                            Toast
                                                .makeText(
                                                    host,
                                                    host.getString(
                                                        R.string.server_connection_client_cert_not_chosen,
                                                    ),
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                        }
                                    }
                                },
                                null,
                                null,
                                null,
                                -1,
                                certAlias,
                            )
                        },
                    )
                }
            }

            item {
                ConnectionPreferenceGroup(
                    title = stringResource(R.string.server_connection_user_agent),
                ) {
                    ConnectionNavigationRow(
                        label = stringResource(R.string.server_connection_user_agent),
                        description = uiState.userAgent,
                        descriptionMaxLines = 2,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showUserAgentSheet = true },
                    )
                }
            }
        }
        CollapsingSettingsHeader(
            title = stringResource(R.string.preferences_server_connection),
            collapseFraction = collapseFraction,
            headerHeight = headerHeightDp,
            onBackClick = onBackClick,
        )
    }

    when (val dialog = headerDialog) {
        HeaderDialogState.Hidden -> {
            Unit
        }

        is HeaderDialogState.Add -> {
            HeaderEditorDialog(
                initial = null,
                headers = uiState.headers,
                onDismiss = { headerDialog = HeaderDialogState.Hidden },
                onSave = {
                    viewModel.saveHeader(it)
                    headerDialog = HeaderDialogState.Hidden
                },
            )
        }

        is HeaderDialogState.Edit -> {
            HeaderEditorDialog(
                initial = dialog.header,
                headers = uiState.headers,
                onDismiss = { headerDialog = HeaderDialogState.Hidden },
                onSave = {
                    viewModel.saveHeader(it)
                    headerDialog = HeaderDialogState.Hidden
                },
            )
        }
    }

    if (showBypassConfirm) {
        AlertDialog(
            onDismissRequest = { showBypassConfirm = false },
            title = { Text(text = stringResource(R.string.server_connection_bypass_ssl_confirm_title)) },
            text = { Text(text = stringResource(R.string.server_connection_bypass_ssl_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setBypassSsl(true)
                        showBypassConfirm = false
                    },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(text = stringResource(R.string.server_connection_bypass_ssl_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBypassConfirm = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showUserAgentSheet) {
        UserAgentSheet(
            current = uiState.userAgent,
            onSave = {
                viewModel.setUserAgent(it)
                showUserAgentSheet = false
            },
            onReset = {
                viewModel.resetUserAgent()
                showUserAgentSheet = false
            },
            onDismiss = { showUserAgentSheet = false },
        )
    }
}

private sealed interface HeaderDialogState {
    data object Hidden : HeaderDialogState

    data object Add : HeaderDialogState

    data class Edit(
        val header: ServerRequestHeader,
    ) : HeaderDialogState
}

@Composable
private fun HeaderEditorDialog(
    initial: ServerRequestHeader?,
    headers: List<ServerRequestHeader>,
    onDismiss: () -> Unit,
    onSave: (ServerRequestHeader) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember(initial) { mutableStateOf(initial?.value.orEmpty()) }

    // Validate the cleaned row: storage drops invalid characters and blank
    // rows, so the dialog refuses to save anything that would vanish.
    val cleaned = remember(name, value) { ServerRequestHeader(name, value).clean() }
    val duplicate =
        remember(cleaned.name, headers, initial) {
            headers.any { it.name == cleaned.name && it.id != initial?.id }
        }
    val nameError =
        when {
            cleaned.name.isEmpty() -> stringResource(R.string.server_connection_header_name_required)
            duplicate -> stringResource(R.string.server_connection_header_duplicate)
            else -> null
        }
    val valueError =
        if (cleaned.value.isEmpty()) {
            stringResource(R.string.server_connection_header_value_required)
        } else {
            null
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (initial == null) {
                            R.string.server_connection_add_header
                        } else {
                            R.string.server_connection_edit_header
                        },
                    ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(R.string.server_connection_header_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(text = it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(R.string.server_connection_header_value)) },
                    singleLine = true,
                    isError = valueError != null,
                    supportingText = valueError?.let { { Text(text = it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(initial?.copy(name = name, value = value) ?: ServerRequestHeader(name, value))
                },
                enabled = nameError == null && valueError == null,
            ) {
                Text(text = stringResource(R.string.server_connection_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserAgentSheet(
    current: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.server_connection_user_agent),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.server_connection_user_agent_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(text = stringResource(R.string.server_connection_user_agent)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onReset) {
                    Text(text = stringResource(R.string.server_connection_user_agent_reset))
                }
                FilledTonalButton(onClick = { onSave(draft) }) {
                    Text(text = stringResource(R.string.server_connection_save))
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    header: ServerRequestHeader,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = header.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.server_connection_delete_header),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConnectionPreferenceGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )

        Surface(
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun ConnectionSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConnectionNavigationRow(
    label: String,
    description: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    descriptionMaxLines: Int = 1,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = descriptionMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
