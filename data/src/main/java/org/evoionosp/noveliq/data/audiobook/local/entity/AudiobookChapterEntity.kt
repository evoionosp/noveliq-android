package org.evoionosp.noveliq.data.audiobook.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "audiobook_chapters",
    primaryKeys = ["audiobookId", "chapterIndex"],
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
data class AudiobookChapterEntity(
    val audiobookId: String,
    val chapterIndex: Int,
    val title: String,
    val startInSeconds: Long,
    val endInSeconds: Long?
)
