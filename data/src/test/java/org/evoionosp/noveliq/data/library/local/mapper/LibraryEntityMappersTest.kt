package org.evoionosp.noveliq.data.library.local.mapper

import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryEntityMappersTest {
    @Test
    fun `entity toDomain preserves identity name and selection`() {
        val library =
            LibraryEntity(
                id = "lib1",
                name = "Audiobooks",
                displayOrder = 3,
                isSelected = true,
            ).toDomain()

        assertEquals("lib1", library.id)
        assertEquals("Audiobooks", library.name)
        assertTrue(library.isSelected)
    }

    @Test
    fun `entity toDomain maps unselected libraries`() {
        val library =
            LibraryEntity(
                id = "lib2",
                name = "Podcasts",
                displayOrder = 1,
                isSelected = false,
            ).toDomain()

        assertFalse(library.isSelected)
    }
}
