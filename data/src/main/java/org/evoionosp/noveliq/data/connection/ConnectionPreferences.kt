package org.evoionosp.noveliq.data.connection

import android.content.Context
import android.content.SharedPreferences

/**
 * Builds the app-private file backing [ConnectionSettingsDataStore] in production.
 * Kept out of the store itself so the store stays a pure function of its
 * [SharedPreferences] and unit tests can supply their own instance.
 *
 * Headers may carry secrets (API keys): the file is MODE_PRIVATE and excluded from
 * backup, so secrets never leave the device via backup/restore.
 */
internal object ConnectionPreferences {
    fun create(context: Context): SharedPreferences =
        context.getSharedPreferences("connection_store", Context.MODE_PRIVATE)
}
