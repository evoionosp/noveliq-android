package org.evoionosp.noveliq.data.library.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Audiobookshelf media progress returned from `GET /api/me/progress/{itemId}`.
 * All times are absolute across the whole book, in seconds.
 */
data class MediaProgressDto(
    @SerializedName("currentTime")
    val currentTime: Double? = null,
    @SerializedName("duration")
    val duration: Double? = null,
    @SerializedName("progress")
    val progress: Double? = null,
    @SerializedName("isFinished")
    val isFinished: Boolean? = null
)

/**
 * Request body for `PATCH /api/me/progress/{itemId}`.
 */
data class UpdateProgressRequestDto(
    @SerializedName("currentTime")
    val currentTime: Double,
    @SerializedName("duration")
    val duration: Double?,
    @SerializedName("progress")
    val progress: Double,
    @SerializedName("isFinished")
    val isFinished: Boolean
)
