package org.evoionosp.noveliq.data.library.local.mapper

import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary

internal fun LibraryEntity.toDomain(): AudiobookLibrary {
    return AudiobookLibrary(
        id = id,
        name = name,
        isSelected = isSelected
    )
}
