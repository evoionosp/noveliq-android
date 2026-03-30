package org.evoionosp.noveliq.data.audiobook.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audiobooks",
    indices = [
        Index(value = ["libraryId"]),
        Index(value = ["libraryId", "title"])
    ]
)
data class AudiobookEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val series: String?,
    val durationInSeconds: Long?
)
