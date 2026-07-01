package org.evoionosp.noveliq.domain.audiobook.model

data class AudiobookDetail(
    val audiobook: Audiobook,
    val description: String?,
    val chapters: List<AudiobookChapter>,
    val tracks: List<AudiobookTrack>,
    val refreshedAtMillis: Long
)
