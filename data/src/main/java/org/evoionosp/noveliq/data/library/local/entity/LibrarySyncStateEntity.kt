package org.evoionosp.noveliq.data.library.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_sync_state")
data class LibrarySyncStateEntity(
    @PrimaryKey val libraryId: String,
    val status: String,
    val lastSyncedAtMillis: Long?,
    val error: String?
)
