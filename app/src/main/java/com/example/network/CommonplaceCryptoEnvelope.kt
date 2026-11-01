package com.example.network

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticated AES-256-GCM Folio Envelope cipher (`CPF1:` wire format) for sealing and unsealing
 * Commonplace quote folios, marginalia bundles, and private salon packets.
 *
 * Wire format:
 *   `CPF1:` + Base64UrlNoPadding( 12-byte salt || 12-byte IV || ciphertext+128-bit-GCM-tag )
 */
object CommonplaceCryptoEnvelope {
    const val ENVELOPE_PREFIX = "CPF1:"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val SALT_LENGTH_BYTES = 12
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    fun sealFolioEnvelope(plaintext: String, passphrase: String): Result<String> {
        if (passphrase.isBlank()) {
            return Result.failure(IllegalArgumentException("Passphrase must not be blank"))
        }
        return try {
            val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val secretKey = deriveAesKey(passphrase, salt)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteBuffer.allocate(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + ciphertext.size)
                .put(salt)
                .put(iv)
                .put(ciphertext)
                .array()

            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(combined)
            Result.success("$ENVELOPE_PREFIX$encoded")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openFolioEnvelope(envelope: String, passphrase: String): Result<String> {
        val trimmed = envelope.trim()
        if (!trimmed.startsWith(ENVELOPE_PREFIX)) {
            return Result.failure(IllegalArgumentException("Invalid envelope header: expected $ENVELOPE_PREFIX"))
        }
        if (passphrase.isBlank()) {
            return Result.failure(IllegalArgumentException("Passphrase must not be blank"))
        }
        return try {
            val b64Payload = trimmed.removePrefix(ENVELOPE_PREFIX)
            val combined = Base64.getUrlDecoder().decode(b64Payload)
            if (combined.size <= SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16) {
                return Result.failure(IllegalArgumentException("Envelope payload truncated or malformed"))
            }

            val salt = combined.copyOfRange(0, SALT_LENGTH_BYTES)
            val iv = combined.copyOfRange(SALT_LENGTH_BYTES, SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(SALT_LENGTH_BYTES + IV_LENGTH_BYTES, combined.size)

            val secretKey = deriveAesKey(passphrase, salt)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            Result.success(String(plainBytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
