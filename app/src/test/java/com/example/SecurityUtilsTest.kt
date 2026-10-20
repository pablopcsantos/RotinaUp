package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserSettings
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SecurityUtils.resetFailedAttempts(context)
    }

    @Test
    fun `test hashPinPbkdf2 produces PBKDF2 formatted hash`() {
        val pin = "1234"
        val salt = "a1b2c3d4e5f60718"
        val hash = SecurityUtils.hashPinPbkdf2(pin, salt)

        assertNotNull(hash)
        assertTrue("Hash must follow PBKDF2 format", hash.startsWith("PBKDF2$10000$"))
        val parts = hash.split("$")
        assertEquals(3, parts.size)
        assertEquals("PBKDF2", parts[0])
        assertEquals("10000", parts[1])
        assertTrue("Key length must be at least 64 hex characters (256 bits)", parts[2].length >= 64)
    }

    @Test
    fun `test verifyPin with modern PBKDF2 hashed credentials`() {
        val pin = "4321"
        val baseSettings = UserSettings()
        val securedSettings = SecurityUtils.createHashedSettings(pin, baseSettings)

        // Plain text PIN must be completely cleared
        assertNull(securedSettings.pinCode)
        assertNotNull(securedSettings.pinHash)
        assertTrue(securedSettings.pinHash!!.startsWith("PBKDF2$"))
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
    fun `test verifyPin with legacy SHA-256 fallback`() {
        val pin = "5555"
        val salt = SecurityUtils.generateSalt()
        val sha256Hash = SecurityUtils.hashPinSha256(pin, salt)

        val legacySha256Settings = UserSettings(
            requireAuth = true,
            pinHash = sha256Hash,
            pinSalt = salt,
            pinCode = null
        )

        // Legacy SHA-256 check succeeds
        assertTrue(SecurityUtils.verifyPin("5555", legacySha256Settings))
        assertFalse(SecurityUtils.verifyPin("1234", legacySha256Settings))
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

    @Test
    fun `test persistent lockout rate limiting after repeated failures`() {
        assertEquals(0, SecurityUtils.getFailedAttempts(context))
        assertEquals(0, SecurityUtils.getLockoutRemainingSeconds(context))

        // 1st failed attempt
        SecurityUtils.recordFailedAttempt(context)
        assertEquals(1, SecurityUtils.getFailedAttempts(context))
        assertEquals(0, SecurityUtils.getLockoutRemainingSeconds(context))

        // 2nd failed attempt
        SecurityUtils.recordFailedAttempt(context)
        assertEquals(2, SecurityUtils.getFailedAttempts(context))
        assertEquals(0, SecurityUtils.getLockoutRemainingSeconds(context))

        // 3rd failed attempt -> triggers lockout!
        val lockoutSec = SecurityUtils.recordFailedAttempt(context)
        assertEquals(3, SecurityUtils.getFailedAttempts(context))
        assertTrue("3 failures should lock out for at least 15s", lockoutSec >= 15)
        assertTrue(SecurityUtils.getLockoutRemainingSeconds(context) > 0)

        // Reset on success
        SecurityUtils.resetFailedAttempts(context)
        assertEquals(0, SecurityUtils.getFailedAttempts(context))
        assertEquals(0, SecurityUtils.getLockoutRemainingSeconds(context))
    }
}
