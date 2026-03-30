package org.evoionosp.noveliq.data.audiobook.local.mapper

import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook

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
