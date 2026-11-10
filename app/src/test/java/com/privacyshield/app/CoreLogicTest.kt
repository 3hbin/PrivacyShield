package com.privacyshield.app

import com.privacyshield.app.data.PinCrypto
import com.privacyshield.app.data.VersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    @Test
    fun pinIsHashedAndNotStoredAsText() {
        val salt = PinCrypto.newSalt()
        val hash = PinCrypto.hash("1234", salt)
        assertNotEquals("1234", hash)
        assertEquals(hash, PinCrypto.hash("1234", salt))
        assertNotEquals(hash, PinCrypto.hash("9999", salt))
    }

    @Test
    fun versionCompareFindsNewerRelease() {
        assertTrue(VersionComparator.isNewer("1.0.0", "1.1.0"))
        assertTrue(VersionComparator.isNewer("1.1.0", "v1.2.0"))
        assertFalse(VersionComparator.isNewer("1.1.0", "1.1.0"))
        assertFalse(VersionComparator.isNewer("1.2.0", "1.1.0"))
    }

    @Test
    fun sha256IsStable() {
        val first = PinCrypto.sha256("apk".toByteArray())
        val second = PinCrypto.sha256("apk".toByteArray())
        assertEquals(first, second)
        assertEquals(64, first.length)
    }
}
