package org.evoionosp.noveliq.domain.audiobook.model

data class AudiobookChapter(
    val title: String,
    val startInSeconds: Long,
    val endInSeconds: Long?
)
