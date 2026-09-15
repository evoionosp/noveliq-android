package org.evoionosp.noveliq.domain.session

/**
 * The on-device catalog (libraries, books, details, continue-listening, sync
 * state). All of it is server/user-scoped, so logout drops every table.
 * Implemented in `:data`, which owns the database.
 */
fun interface LocalCatalogCleaner {
    suspend fun clear()
}
