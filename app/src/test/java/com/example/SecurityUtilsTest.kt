package com.example

import com.example.data.model.UserSettings
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `test hashPin produces consistent hash for same pin and salt`() {
        val pin = "1234"
        val salt = "a1b2c3d4e5f60718"
        val hash1 = SecurityUtils.hashPin(pin, salt)
        val hash2 = SecurityUtils.hashPin(pin, salt)

        assertNotNull(hash1)
        assertTrue(hash1.length >= 32)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `test hashPin produces different hashes for different salts`() {
        val pin = "1234"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()
        assertNotEquals(salt1, salt2)

        val hash1 = SecurityUtils.hashPin(pin, salt1)
        val hash2 = SecurityUtils.hashPin(pin, salt2)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `test verifyPin with hashed credentials`() {
        val pin = "4321"
        val baseSettings = UserSettings()
        val securedSettings = SecurityUtils.createHashedSettings(pin, baseSettings)

        // Plain text PIN must be completely cleared
        assertNull(securedSettings.pinCode)
        assertNotNull(securedSettings.pinHash)
        assertNotNull(securedSettings.pinSalt)
        assertTrue(securedSettings.requireAuth)

        // Verification with correct PIN
        assertTrue(SecurityUtils.verifyPin("4321", securedSettings))

        // Verification with incorrect PIN
        assertFalse(SecurityUtils.verifyPin("0000", securedSettings))
        assertFalse(SecurityUtils.verifyPin("432", securedSettings))
        assertFalse(SecurityUtils.verifyPin("", securedSettings))
    }

    @Test
    fun `test verifyPin with legacy plain text fallback`() {
        val legacySettings = UserSettings(
            requireAuth = true,
            pinCode = "9876",
            pinHash = null,
            pinSalt = null
        )

        // Legacy plain text check succeeds
        assertTrue(SecurityUtils.verifyPin("9876", legacySettings))
        assertFalse(SecurityUtils.verifyPin("1234", legacySettings))
    }
}
