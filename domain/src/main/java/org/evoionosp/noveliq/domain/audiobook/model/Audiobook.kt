package org.evoionosp.noveliq.domain.audiobook.model

data class Audiobook(
    val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val series: String?,
    val durationInSeconds: Long?,
    /**
     * Absolute playback position across the whole book, in seconds. Only populated for
     * continue-listening items, which are the one place progress is observed.
     */
    val progressSeconds: Double? = null,
)
