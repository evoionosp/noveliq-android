package org.evoionosp.noveliq.presentation.navigation

import org.evoionosp.noveliq.presentation.bookdetails.BookDetailsUiState
import org.evoionosp.noveliq.presentation.bookdetails.resolveDetailsProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailsRouteTest {
    @Test
    fun `details route embeds both ids`() {
        assertEquals(
            "book_details/lib1/book9",
            bookDetailsRoute(libraryId = "lib1", audiobookId = "book9"),
        )
    }

    @Test
    fun `details route matches the registered template`() {
        val template = AppRoute.BookDetails.route.replace(Regex("\\{[^}]+\\}"), "([^/]+)")
        assertTrue(bookDetailsRoute("lib1", "book9").matches(Regex(template)))
    }

    @Test
    fun `details progress is live while the book is playing`() {
        assertEquals(42.0, resolveDetailsProgress(true, 42.0, 10.0), 0.0)
    }

    @Test
    fun `details progress is saved while another book is playing`() {
        assertEquals(10.0, resolveDetailsProgress(false, 42.0, 10.0), 0.0)
    }

    @Test
    fun `details book progress is the resolved fraction`() {
        assertEquals(
            0.5f,
            BookDetailsUiState(progressSeconds = 30.0, totalSeconds = 60.0).bookProgress,
            0.0f,
        )
    }

    @Test
    fun `details book progress is zero without a total`() {
        assertEquals(
            0.0f,
            BookDetailsUiState(progressSeconds = 30.0, totalSeconds = 0.0).bookProgress,
            0.0f,
        )
    }
}
