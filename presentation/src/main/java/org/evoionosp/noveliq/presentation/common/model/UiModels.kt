package org.evoionosp.noveliq.presentation.common.model

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary

/**
 * UI model for Audiobook, decoupled from domain model.
 * Contains only the fields needed by the UI, with UI-specific formatting.
 */
data class AudiobookUiModel(
    val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val authorNames: List<String>,
    val coverUrl: String,
    val durationInSeconds: Long?,
    val durationLabel: String,
    val progressSeconds: Double?,
    val timeLeftLabel: String?,
)

/**
 * UI model for AudiobookLibrary, decoupled from domain model.
 */
data class LibraryUiModel(
    val id: String,
    val name: String,
    val isSelected: Boolean,
)

/**
 * UI model for Author grid item.
 */
data class AuthorUiModel(
    val name: String,
    val bookCount: Int,
    val photoUrl: String?,
)

/**
 * Converts domain Audiobook to UI model.
 */
fun Audiobook.toUiModel(): AudiobookUiModel {
    val names = author.toAuthorNames()
    return AudiobookUiModel(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        authorNames = names,
        coverUrl = coverUrl,
        durationInSeconds = durationInSeconds,
        durationLabel = durationInSeconds?.toDurationLabel() ?: "",
        progressSeconds = progressSeconds,
        timeLeftLabel = timeLeftLabel(durationInSeconds, progressSeconds),
    )
}

/**
 * Converts UI model back to domain model.
 * Note: This creates a minimal domain model with only the fields available in UI model.
 * For full domain model, fetch from repository using the ID.
 */
fun AudiobookUiModel.toDomain(): Audiobook =
    Audiobook(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = null,
        durationInSeconds = durationInSeconds,
        progressSeconds = progressSeconds,
    )

/**
 * Converts domain AudiobookLibrary to UI model.
 */
fun AudiobookLibrary.toUiModel(): LibraryUiModel =
    LibraryUiModel(
        id = id,
        name = name,
        isSelected = isSelected,
    )

/**
 * Parses author string into list of author names.
 */
internal fun String.toAuthorNames(): List<String> =
    split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Unknown Author") }

/**
 * Formats the remaining listening time at 1x speed ("5h 30m left", "45m left").
 * Null when the remaining time is unknown (no duration or no progress) or the book is
 * finished — callers fall back to the author line then.
 */
internal fun timeLeftLabel(
    durationInSeconds: Long?,
    progressSeconds: Double?,
): String? {
    if (durationInSeconds == null || progressSeconds == null) return null
    val remainingSeconds = durationInSeconds - progressSeconds
    if (remainingSeconds <= 0) return null
    val totalMinutes = Math.round(remainingSeconds / 60.0).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "%dh %02dm left".format(hours, minutes)
    } else {
        "%dm left".format(totalMinutes)
    }
}

/**
 * Formats seconds into a duration label (H:MM:SS or M:SS).
 */
internal fun Long.toDurationLabel(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}
