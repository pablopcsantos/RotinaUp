package com.example.util

import com.example.data.model.UserSettings
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    /**
     * Generates a 16-byte cryptographically secure random salt in hex format.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes the given PIN with the provided salt using SHA-256.
     */
    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an entered PIN against stored hashed credentials.
     * Supports transparent fallback for legacy plain text PIN migration.
     */
    fun verifyPin(enteredPin: String, settings: UserSettings): Boolean {
        // Preferred: salted SHA-256 hash
        if (!settings.pinHash.isNullOrBlank() && !settings.pinSalt.isNullOrBlank()) {
            val computed = hashPin(enteredPin, settings.pinSalt)
            return computed == settings.pinHash
        }
        // Legacy fallback: plain text comparison for existing installs prior to migration
        if (!settings.pinCode.isNullOrBlank()) {
            return enteredPin == settings.pinCode
        }
        return false
    }

    /**
     * Creates a new UserSettings instance with a secure salted hash for the PIN,
     * removing any plain text PIN representation.
     */
    fun createHashedSettings(pin: String, settings: UserSettings): UserSettings {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        return settings.copy(
            requireAuth = true,
            pinHash = hash,
            pinSalt = salt,
            pinCode = null // Clear plain text
        )
    }
}
