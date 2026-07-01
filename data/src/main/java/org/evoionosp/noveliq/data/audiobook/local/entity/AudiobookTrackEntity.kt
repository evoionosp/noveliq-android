package org.evoionosp.noveliq.data.audiobook.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "audiobook_tracks",
    primaryKeys = ["audiobookId", "trackIndex"],
    foreignKeys = [
        ForeignKey(
            entity = AudiobookDetailEntity::class,
            parentColumns = ["audiobookId"],
            childColumns = ["audiobookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["audiobookId"])]
)
data class AudiobookTrackEntity(
    val audiobookId: String,
    val trackIndex: Int,
    val startOffsetInSeconds: Long,
    val durationInSeconds: Long,
    val title: String,
    val remoteUrl: String,
    val mimeType: String?
)
