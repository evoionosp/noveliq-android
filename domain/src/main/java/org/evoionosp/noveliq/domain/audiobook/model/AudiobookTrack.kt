package org.evoionosp.noveliq.domain.audiobook.model

data class AudiobookTrack(
    val index: Int,
    val startOffsetInSeconds: Long,
    val durationInSeconds: Long,
    val title: String,
    val remoteUrl: String,
    val mimeType: String?
)
