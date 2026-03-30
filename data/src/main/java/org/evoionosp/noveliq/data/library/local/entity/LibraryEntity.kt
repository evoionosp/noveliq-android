package org.evoionosp.noveliq.data.library.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val displayOrder: Int,
    val isSelected: Boolean
)
