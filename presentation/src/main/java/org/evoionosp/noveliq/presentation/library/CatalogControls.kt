package org.evoionosp.noveliq.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.presentation.common.model.LibraryUiModel
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.presentation.R

@Composable
internal fun CatalogTopControls(
    libraries: List<LibraryUiModel>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    syncStatus: SyncStatus,
    onLibrarySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (libraries.isNotEmpty()) {
            LibraryDropdownTrigger(
                libraries = libraries,
                selectedLibraryId = selectedLibraryId,
                selectedLibraryName = selectedLibraryName,
                onLibrarySelected = onLibrarySelected,
                modifier = Modifier.weight(1f)
            )
        }
        SyncStatusLabel(
            syncStatus = syncStatus,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
internal fun LibraryDropdown(
    libraries: List<LibraryUiModel>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    syncStatus: SyncStatus,
    onLibrarySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (libraries.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val syncStatusLabel = when (syncStatus) {
        SyncStatus.Idle -> stringResource(R.string.home_synced)
        SyncStatus.Syncing -> stringResource(R.string.home_syncing)
        is SyncStatus.Success -> "Up to date"
        is SyncStatus.Stale -> stringResource(R.string.home_showing_cached)
        is SyncStatus.Failed -> stringResource(R.string.home_sync_failed)
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.onSizeChanged { size ->
                buttonWidth = with(density) { size.width.toDp() }
            }
        ) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectedLibraryName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = syncStatusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.library_dropdown_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(buttonWidth),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            libraries.forEach { library ->
                val isSelected = library.id == selectedLibraryId
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = library.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        if (library.id != selectedLibraryId) {
                            onLibrarySelected(library.id)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LibraryDropdownTrigger(
    libraries: List<LibraryUiModel>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    onLibrarySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (libraries.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.onSizeChanged { size ->
                buttonWidth = with(density) { size.width.toDp() }
            }
        ) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = selectedLibraryName.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.library_dropdown_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(buttonWidth),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            libraries.forEach { library ->
                val isSelected = library.id == selectedLibraryId
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = library.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        if (library.id != selectedLibraryId) {
                            onLibrarySelected(library.id)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SyncStatusLabel(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    val label = when (syncStatus) {
        SyncStatus.Idle -> stringResource(R.string.home_synced)
        SyncStatus.Syncing -> stringResource(R.string.home_syncing)
        is SyncStatus.Success -> stringResource(R.string.home_synced)
        is SyncStatus.Stale -> stringResource(R.string.home_showing_cached)
        is SyncStatus.Failed -> stringResource(R.string.home_sync_failed)
    }

    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
