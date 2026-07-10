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
    val durationLabel: String
)

/**
 * UI model for AudiobookLibrary, decoupled from domain model.
 */
data class LibraryUiModel(
    val id: String,
    val name: String,
    val isSelected: Boolean
)

/**
 * UI model for Author grid item.
 */
data class AuthorUiModel(
    val name: String,
    val bookCount: Int,
    val photoUrl: String?
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
        durationLabel = durationInSeconds?.toDurationLabel() ?: ""
    )
}

/**
 * Converts UI model back to domain model.
 * Note: This creates a minimal domain model with only the fields available in UI model.
 * For full domain model, fetch from repository using the ID.
 */
fun AudiobookUiModel.toDomain(): Audiobook {
    return Audiobook(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = null,
        durationInSeconds = durationInSeconds
    )
}

/**
 * Converts domain AudiobookLibrary to UI model.
 */
fun AudiobookLibrary.toUiModel(): LibraryUiModel {
    return LibraryUiModel(
        id = id,
        name = name,
        isSelected = isSelected
    )
}

/**
 * Parses author string into list of author names.
 */
internal fun String.toAuthorNames(): List<String> {
    return split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Unknown Author") }
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
