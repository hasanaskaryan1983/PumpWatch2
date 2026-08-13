package com.pumpwatch.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pumpwatch.app.domain.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ownerAuthDataStore by preferencesDataStore(name = "owner_auth")

/**
 * Fully local, fully offline login gate for the app's owner. No server, no
 * Gmail, nothing leaves the device. The password itself is never stored —
 * only a salted PBKDF2 hash.
 *
 * Uses plain DataStore (not EncryptedSharedPreferences): that library is
 * deprecated upstream and known to throw keyset-corruption errors on some
 * OEM devices, which is a much worse failure mode for a solo-developer app
 * than losing at-rest encryption on a value that was never plaintext anyway.
 *
 * There is no recovery flow: this is intentional (no server means nothing to
 * reset a password against). If the owner forgets it, the only way back in
 * is clearing the app's data / reinstalling, which wipes this store and
 * requires setting a new username/password from scratch.
 */
class OwnerAuthStore(private val context: Context) {

    private object Keys {
        val USERNAME = stringPreferencesKey("owner_username")
        val HASH = stringPreferencesKey("owner_password_hash")
        val SALT = stringPreferencesKey("owner_password_salt")
    }

    val hasCredentialsFlow: Flow<Boolean> = context.ownerAuthDataStore.data.map { prefs ->
        prefs[Keys.USERNAME] != null && prefs[Keys.HASH] != null && prefs[Keys.SALT] != null
    }

    suspend fun hasCredentials(): Boolean = hasCredentialsFlow.first()

    suspend fun setCredentials(username: String, password: String) {
        val result = PasswordHasher.hash(password)
        context.ownerAuthDataStore.edit { prefs ->
            prefs[Keys.USERNAME] = username.trim()
            prefs[Keys.HASH] = result.hashBase64
            prefs[Keys.SALT] = result.saltBase64
        }
    }

    suspend fun verify(username: String, password: String): Boolean {
        val prefs = context.ownerAuthDataStore.data.first()
        val storedUsername = prefs[Keys.USERNAME] ?: return false
        val storedHash = prefs[Keys.HASH] ?: return false
        val storedSalt = prefs[Keys.SALT] ?: return false
        if (username.trim() != storedUsername) return false
        return PasswordHasher.matches(password, storedSalt, storedHash)
    }
}
