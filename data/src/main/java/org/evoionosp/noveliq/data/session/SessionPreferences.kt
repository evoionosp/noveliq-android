package org.evoionosp.noveliq.data.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Builds the app-private file backing [SessionDataStore] in production. Kept out of
 * the store itself so the store stays a pure function of its [SharedPreferences] and
 * unit tests can supply their own instance.
 *
 * The file holds auth tokens: it is MODE_PRIVATE and excluded from backup, so tokens
 * never leave the device via backup/restore.
 */
internal object SessionPreferences {
    fun create(context: Context): SharedPreferences =
        context.getSharedPreferences("session_store", Context.MODE_PRIVATE)
}
