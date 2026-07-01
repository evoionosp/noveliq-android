package org.evoionosp.noveliq.data.audiobook.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "continue_listening_items",
    indices = [
        Index(value = ["libraryId"]),
        Index(value = ["libraryId", "progressLastUpdateMillis"])
    ]
)
data class ContinueListeningEntity(
    @PrimaryKey val audiobookId: String,
    val libraryId: String,
    val progressLastUpdateMillis: Long
)
