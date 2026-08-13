package com.pumpwatch.app.domain

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA256 password hashing — the password itself is never stored. */
object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    data class HashResult(val hashBase64: String, val saltBase64: String)

    fun hash(password: String): HashResult {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashWithSalt(password, salt)
        return HashResult(
            hashBase64 = Base64.getEncoder().encodeToString(hash),
            saltBase64 = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun matches(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val salt = Base64.getDecoder().decode(saltBase64)
        val computed = hashWithSalt(password, salt)
        val expected = Base64.getDecoder().decode(expectedHashBase64)
        return computed.contentEquals(expected)
    }

    private fun hashWithSalt(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
