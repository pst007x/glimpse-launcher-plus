package com.glimpsemedia.glimpselauncher.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** The padlock PIN: 4 to 8 digits, kept only as a salted, slow hash. The digits themselves are never saved. */
object Pin {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isValid(pin: String): Boolean = pin.length in 4..8 && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    /** Base64 of PBKDF2-HMAC-SHA256 over the PIN. */
    fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }

    /** True when [pin] gives [hash] with the base64 [salt]; the comparison takes the same time whatever the digits. */
    fun matches(pin: String, hash: String, salt: String): Boolean {
        if (!isValid(pin)) return false
        val saltBytes = try { Base64.getDecoder().decode(salt) } catch (_: IllegalArgumentException) { return false }
        return MessageDigest.isEqual(hash(pin, saltBytes).toByteArray(), hash.toByteArray())
    }
}

/** Five wrong PINs in a row close the pad for 30 seconds; the count then starts again. */
class PinTries(private val now: () -> Long) {
    private var failures = 0
    private var blockedUntil = 0L

    fun blockedForMs(): Long = (blockedUntil - now()).coerceAtLeast(0)

    fun failed() {
        failures++
        if (failures >= MAX_TRIES) {
            blockedUntil = now() + BLOCK_MS
            failures = 0
        }
    }

    fun succeeded() {
        failures = 0
    }

    companion object {
        const val MAX_TRIES = 5
        const val BLOCK_MS = 30_000L
    }
}
