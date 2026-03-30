package org.evoionosp.noveliq.data.library.remote.mapper

import java.util.Locale
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
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
    val authorValue = metadata?.authorName.orEmpty().ifBlank { "Unknown Author" }
    val normalizedBaseUrl = serviceFactory.normalizeBaseUrl(baseUrl)
    val coverUrl = "${normalizedBaseUrl}api/items/$idValue/cover?width=400&format=webp"

    return AudiobookEntity(
        id = idValue,
        libraryId = libraryId ?: fallbackLibraryId,
        title = titleValue,
        author = authorValue,
        coverUrl = coverUrl,
        series = metadata?.series?.firstOrNull()?.name,
        durationInSeconds = media?.durationInSeconds?.toLong()
    )
}
