package org.evoionosp.noveliq.data.audiobook.local.mapper

import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack

internal fun AudiobookEntity.toDomain(): Audiobook {
    return Audiobook(
        id = id,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = series,
        durationInSeconds = durationInSeconds
    )
}

internal fun AudiobookDetailEntity.toAudiobook(): Audiobook {
    return Audiobook(
        id = audiobookId,
        libraryId = libraryId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        series = series,
        durationInSeconds = durationInSeconds
    )
}

internal fun AudiobookChapterEntity.toDomain(): AudiobookChapter {
    return AudiobookChapter(
        title = title,
        startInSeconds = startInSeconds,
        endInSeconds = endInSeconds
    )
}

internal fun AudiobookTrackEntity.toDomain(): AudiobookTrack {
    return AudiobookTrack(
        index = trackIndex,
        startOffsetInSeconds = startOffsetInSeconds,
        durationInSeconds = durationInSeconds,
        title = title,
        remoteUrl = remoteUrl,
        mimeType = mimeType
    )
}

internal fun AudiobookDetailEntity.toDomain(
    chapters: List<AudiobookChapterEntity>,
    tracks: List<AudiobookTrackEntity>
): AudiobookDetail {
    return AudiobookDetail(
        audiobook = toAudiobook(),
        description = description,
        chapters = chapters.map { it.toDomain() },
        tracks = tracks.map { it.toDomain() },
        refreshedAtMillis = refreshedAtMillis
    )
}
