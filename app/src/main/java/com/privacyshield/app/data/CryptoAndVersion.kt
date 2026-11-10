package com.privacyshield.app.data

import java.security.MessageDigest
import java.security.SecureRandom

object PinCrypto {
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(value: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$value".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

object VersionComparator {
    fun compare(current: String, remote: String): Int {
        val a = parts(current)
        val b = parts(remote)
        val size = maxOf(a.size, b.size)
        for (index in 0 until size) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    fun isNewer(current: String, remote: String): Boolean = compare(current, remote) < 0

    private fun parts(value: String): List<Int> {
        return value.trim().removePrefix("v").removePrefix("V")
            .substringBefore("-")
            .split('.')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
    }
}
