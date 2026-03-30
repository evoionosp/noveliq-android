package org.evoionosp.noveliq.domain.audiobook.model

data class Audiobook(
    val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val series: String?,
    val durationInSeconds: Long?
)
