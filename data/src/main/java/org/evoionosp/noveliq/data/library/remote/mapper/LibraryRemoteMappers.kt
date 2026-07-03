package org.evoionosp.noveliq.data.library.remote.mapper

import java.util.Locale
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.noveliq.data.network.UrlUtils
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
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
    val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
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

internal fun LibraryItemDto.toDetailEntity(
    baseUrl: String,
    fallbackLibraryId: String,
    refreshedAtMillis: Long
): AudiobookDetailEntity? {
    val summary = toEntity(
        baseUrl = baseUrl,
        fallbackLibraryId = fallbackLibraryId
    ) ?: return null

    return AudiobookDetailEntity(
        audiobookId = summary.id,
        libraryId = summary.libraryId,
        title = summary.title,
        author = summary.author,
        coverUrl = summary.coverUrl,
        series = summary.series,
        durationInSeconds = summary.durationInSeconds,
        description = media?.metadata?.description?.takeIf { it.isNotBlank() },
        refreshedAtMillis = refreshedAtMillis
    )
}

internal fun LibraryItemDto.toChapterEntities(audiobookId: String): List<AudiobookChapterEntity> {
    return media?.chapters.orEmpty().mapIndexed { index, chapter ->
        AudiobookChapterEntity(
            audiobookId = audiobookId,
            chapterIndex = index,
            title = chapter.title.orEmpty().ifBlank { "Chapter ${index + 1}" },
            startInSeconds = chapter.startInSeconds?.toLong() ?: 0L,
            endInSeconds = chapter.endInSeconds?.toLong()
        )
    }.sortedBy { it.startInSeconds }
        .mapIndexed { index, chapter -> chapter.copy(chapterIndex = index) }
}

internal fun LibraryItemDto.toTrackEntities(
    baseUrl: String,
    audiobookId: String
): List<AudiobookTrackEntity> {
    val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
    return media?.tracks.orEmpty()
        .filter { !it.contentUrl.isNullOrBlank() }
        .mapIndexed { fallbackIndex, track ->
            val trackIndex = track.index ?: fallbackIndex
            AudiobookTrackEntity(
                audiobookId = audiobookId,
                trackIndex = trackIndex,
                startOffsetInSeconds = track.startOffsetInSeconds?.toLong() ?: 0L,
                durationInSeconds = track.durationInSeconds?.toLong() ?: 0L,
                title = track.title.orEmpty().ifBlank { "Track ${fallbackIndex + 1}" },
                remoteUrl = normalizedBaseUrl + track.contentUrl.orEmpty().removePrefix("/"),
                mimeType = track.mimeType
            )
        }.sortedBy { it.trackIndex }
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
