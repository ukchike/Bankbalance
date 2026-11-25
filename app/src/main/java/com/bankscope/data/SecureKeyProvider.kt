package com.bankscope.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Generates and stores the local database's encryption passphrase. The passphrase itself is
 * random bytes with no meaning on its own; what protects it is that it's kept only in
 * [EncryptedSharedPreferences], which wraps its values with a key that never leaves the Android
 * Keystore (hardware-backed where the device supports it). Losing the Keystore entry (e.g. app
 * data wipe) makes the encrypted database file permanently unreadable, same as losing any other
 * at-rest encryption key — there is no recovery path, by design.
 */
class SecureKeyProvider(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Returns the database passphrase, generating and persisting a new random one on first run. */
    fun getOrCreateDatabasePassphrase(): CharArray {
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing.toCharArray()

        val randomBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val generated = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated.toCharArray()
    }

    private companion object {
        const val PREFS_NAME = "bankbalance_secure_prefs"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
