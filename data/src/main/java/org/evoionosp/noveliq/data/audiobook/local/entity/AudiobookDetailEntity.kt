package org.evoionosp.noveliq.data.audiobook.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audiobook_details",
    indices = [Index(value = ["libraryId"])]
)
data class AudiobookDetailEntity(
    @PrimaryKey val audiobookId: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val series: String?,
    val durationInSeconds: Long?,
    val description: String?,
    val refreshedAtMillis: Long
)
