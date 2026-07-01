package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.presentation.R

@Composable
internal fun CatalogTopControls(
    libraries: List<AudiobookLibrary>,
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
private fun LibraryDropdownTrigger(
    libraries: List<AudiobookLibrary>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    onLibrarySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (libraries.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = selectedLibraryName.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.library_dropdown_label)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            libraries.forEach { library ->
                DropdownMenuItem(
                    text = { Text(text = library.name) },
                    onClick = {
                        expanded = false
                        if (library.id != selectedLibraryId) {
                            onLibrarySelected(library.id)
                        }
                    }
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
