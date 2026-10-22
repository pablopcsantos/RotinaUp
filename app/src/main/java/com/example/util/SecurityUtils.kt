package com.example.util

import android.content.Context
import com.example.data.model.UserSettings
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val PBKDF2_ITERATIONS = 100000
    const val MIN_SAFE_ITERATIONS = 1000
    const val MAX_SAFE_ITERATIONS = 500000
    private const val KEY_LENGTH = 256
    private const val HASH_HEX_LENGTH = KEY_LENGTH / 4
    private const val PREFS_NAME = "rotinaup_security_prefs"
    private const val PREF_FAILED_ATTEMPTS = "failed_pin_attempts"
    private const val PREF_LOCKOUT_UNTIL = "pin_lockout_until_ms"
    private const val PREF_LOCKOUT_ELAPSED_UNTIL = "pin_lockout_elapsed_until_ms"
    private const val PREF_LOCKOUT_WALL_UNTIL = "pin_lockout_wall_until_ms"

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
     * Hashes the given PIN using PBKDF2WithHmacSHA256 with 100,000 iterations and salt.
     * Output format: PBKDF2$<iterations>$<hexEncodedKey>
     */
    fun hashPinPbkdf2(pin: String, salt: String, iterations: Int = PBKDF2_ITERATIONS): String {
        val safeIterations = iterations.coerceIn(MIN_SAFE_ITERATIONS, MAX_SAFE_ITERATIONS)
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), safeIterations, KEY_LENGTH)
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val hashBytes = factory.generateSecret(spec).encoded
            val hex = hashBytes.joinToString("") { "%02x".format(it) }
            return "PBKDF2\$safeIterations\$hex"
        } finally {
            // Clear the mutable password copy held by PBEKeySpec as soon as derivation finishes.
            spec.clearPassword()
        }
    }

    /**
     * Legacy SHA-256 hashing maintained strictly for backward-compatibility upgrade.
     */
    fun hashPinSha256(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an entered PIN against stored hashed credentials.
     * Uses constant-time comparison to prevent timing attacks.
     * Validates iteration parameters within safe operational bounds.
     */
    fun verifyPin(enteredPin: String, settings: UserSettings): Boolean {
        val storedHash = settings.pinHash
        val salt = settings.pinSalt

        // Modern: PBKDF2 salted derivation
        if (!storedHash.isNullOrBlank() && storedHash.startsWith("PBKDF2$") && !salt.isNullOrBlank()) {
            val parts = storedHash.split("$")
            if (parts.size == 3) {
                val iterations = parts[1].toIntOrNull()
                val storedKey = parts[2]
                // Reject malformed hashes and out-of-bounds iteration parameters before deriving a key.
                if (
                    iterations == null ||
                    iterations !in MIN_SAFE_ITERATIONS..MAX_SAFE_ITERATIONS ||
                    storedKey.length != HASH_HEX_LENGTH ||
                    !storedKey.matches(Regex("[0-9a-fA-F]+"))
                ) {
                    return false
                }
                val computed = hashPinPbkdf2(enteredPin, salt, iterations)
                return constantTimeEquals(computed, storedHash)
            }
            return false
        }

        // Backward compatibility: legacy SHA-256 hash
        if (!storedHash.isNullOrBlank() && !salt.isNullOrBlank()) {
            val computed = hashPinSha256(enteredPin, salt)
            return constantTimeEquals(computed, storedHash)
        }

        // Backward compatibility: legacy plain-text PIN
        if (!settings.pinCode.isNullOrBlank()) {
            return constantTimeEquals(enteredPin, settings.pinCode)
        }

        return false
    }

    /**
     * Checks if credentials require upgrade (e.g. plain text, legacy SHA-256, or PBKDF2 with outdated iterations).
     */
    fun needsUpgrade(settings: UserSettings): Boolean {
        if (!settings.pinCode.isNullOrBlank()) return true
        val hash = settings.pinHash ?: return false
        if (!hash.startsWith("PBKDF2$")) return true

        val parts = hash.split("$")
        if (parts.size != 3) return true

        val iterations = parts[1].toIntOrNull() ?: return true
        val encodedKey = parts[2]
        if (
            iterations !in MIN_SAFE_ITERATIONS..MAX_SAFE_ITERATIONS ||
            encodedKey.length != HASH_HEX_LENGTH ||
            !encodedKey.matches(Regex("[0-9a-fA-F]+"))
        ) {
            return true
        }

        return iterations < PBKDF2_ITERATIONS
    }

    /**
     * Constant-time string comparison to prevent timing side-channel attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    /**
     * Creates a new UserSettings instance with a secure PBKDF2 salted hash for the PIN,
     * removing any legacy plain-text PIN representation.
     */
    fun createHashedSettings(pin: String, settings: UserSettings): UserSettings {
        val salt = generateSalt()
        val hash = hashPinPbkdf2(pin, salt)
        return settings.copy(
            requireAuth = true,
            pinHash = hash,
            pinSalt = salt,
            pinCode = null // Clear plain text
        )
    }

    // ==================== PERSISTENT LOCKOUT RATE LIMITING ====================

    fun getFailedAttempts(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_FAILED_ATTEMPTS, 0)
    }

    fun getLockoutRemainingSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val elapsedUntil = prefs.getLong(PREF_LOCKOUT_ELAPSED_UNTIL, 0L)
        val wallUntil = prefs.getLong(PREF_LOCKOUT_WALL_UNTIL, prefs.getLong(PREF_LOCKOUT_UNTIL, 0L))

        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()

        val elapsedRemaining = if (elapsedUntil > nowElapsed) ((elapsedUntil - nowElapsed) + 999) / 1000 else 0
        val wallRemaining = if (wallUntil > nowWall) ((wallUntil - nowWall) + 999) / 1000 else 0

        return maxOf(elapsedRemaining.toInt(), wallRemaining.toInt()).coerceAtLeast(0)
    }

    fun recordFailedAttempt(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentFailed = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(PREF_FAILED_ATTEMPTS, currentFailed)

        var lockoutSec = 0
        if (currentFailed >= 3) {
            lockoutSec = when (currentFailed) {
                3 -> 15
                4 -> 30
                5 -> 60
                else -> 300
            }
            val elapsedUntil = android.os.SystemClock.elapsedRealtime() + (lockoutSec * 1000L)
            val wallUntil = System.currentTimeMillis() + (lockoutSec * 1000L)
            editor.putLong(PREF_LOCKOUT_ELAPSED_UNTIL, elapsedUntil)
            editor.putLong(PREF_LOCKOUT_WALL_UNTIL, wallUntil)
        }
        editor.commit()
        return lockoutSec
    }

    fun resetFailedAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .remove(PREF_LOCKOUT_ELAPSED_UNTIL)
            .remove(PREF_LOCKOUT_WALL_UNTIL)
            .remove(PREF_LOCKOUT_UNTIL)
            .commit()
    }
}
