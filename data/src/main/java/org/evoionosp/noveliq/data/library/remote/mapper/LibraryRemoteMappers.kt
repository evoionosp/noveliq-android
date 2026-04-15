package org.evoionosp.noveliq.data.library.remote.mapper

import java.util.Locale
import org.evoionosp.noveliq.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.data.library.remote.dto.LibraryDto
import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemDto

internal fun LibraryDto.toEntity(isSelected: Boolean): LibraryEntity? {
    val idValue = id.orEmpty()
    if (idValue.isBlank()) return null
    return LibraryEntity(
        id = idValue,
        name = name.orEmpty().ifBlank { "Library" },
        displayOrder = displayOrder ?: Int.MAX_VALUE,
        isSelected = isSelected
    )
}

internal fun LibraryDto.isAudiobookLibrary(): Boolean {
    return mediaType.orEmpty().lowercase(Locale.US) == "book"
}

internal fun LibraryItemDto.toEntity(
    serviceFactory: AudiobookshelfLibraryServiceFactory,
    baseUrl: String,
    fallbackLibraryId: String
): AudiobookEntity? {
    if (mediaType.orEmpty().lowercase(Locale.US) != "book") {
        return null
    }

    val idValue = id.orEmpty()
    if (idValue.isBlank()) return null

    val metadata = media?.metadata
    val titleValue = metadata?.title.orEmpty().ifBlank { "Untitled" }
    val authorValue = metadata?.authorName
        .orEmpty()
        .toDisplayAuthorName()
        .ifBlank { "Unknown Author" }
    val normalizedBaseUrl = serviceFactory.normalizeBaseUrl(baseUrl)
    val coverUrl = "${normalizedBaseUrl}api/items/$idValue/cover?width=400&format=webp"

    return AudiobookEntity(
        id = idValue,
        libraryId = libraryId ?: fallbackLibraryId,
        title = titleValue,
        author = authorValue,
        coverUrl = coverUrl,
        series = metadata?.seriesName ?: metadata?.series?.firstOrNull()?.name,
        durationInSeconds = media?.durationInSeconds?.toLong()
    )
}

internal fun LibraryItemDto.toContinueListeningEntity(
    fallbackLibraryId: String
): ContinueListeningEntity? {
    val idValue = id.orEmpty()
    if (idValue.isBlank()) return null
    if (mediaType.orEmpty().lowercase(Locale.US) != "book") return null

    return ContinueListeningEntity(
        audiobookId = idValue,
        libraryId = libraryId ?: fallbackLibraryId,
        progressLastUpdateMillis = progressLastUpdateMillis ?: 0L
    )
}

internal fun LibraryItemDto.toChapterDomainList(): List<AudiobookChapter> {
    return media?.chapters.orEmpty().mapIndexed { index, chapter ->
        AudiobookChapter(
            title = chapter.title.orEmpty().ifBlank { "Chapter ${index + 1}" },
            startInSeconds = chapter.startInSeconds?.toLong() ?: 0L,
            endInSeconds = chapter.endInSeconds?.toLong()
        )
    }.sortedBy { it.startInSeconds }
}

private fun String.toDisplayAuthorName(): String {
    val trimmed = trim()
    val commaIndex = trimmed.indexOf(',')
    if (commaIndex <= 0 || commaIndex >= trimmed.lastIndex) {
        return trimmed
    }

    val lastName = trimmed.substring(0, commaIndex).trim()
    val firstName = trimmed.substring(commaIndex + 1).trim()
    if (firstName.isBlank() || lastName.isBlank()) {
        return trimmed
    }
    return "$firstName $lastName"
}
