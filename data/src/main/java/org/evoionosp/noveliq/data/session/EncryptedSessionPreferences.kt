package org.evoionosp.noveliq.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Builds the encrypted file backing [SessionDataStore] in production. Kept out of the
 * store itself so the store stays a pure function of its [SharedPreferences] and unit
 * tests never touch the AndroidKeyStore.
 */
internal object EncryptedSessionPreferences {
    fun create(context: Context): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        return EncryptedSharedPreferences.create(
            context,
            "encrypted_session_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
