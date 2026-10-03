// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable

private const val ITERATIONS = 210_000
private const val KEY_BITS = 256
private const val SALT_BYTES = 16
private const val IV_BYTES = 12
private const val TAG_BITS = 128

/** Data sealed with a password: everything but the password is needed to open it again. */
@Serializable
data class Sealed(val salt: String, val iv: String, val data: String)

/**
 * Password-based encryption for the sessions in a backup (T18d): AES-256-GCM with a key derived
 * by PBKDF2-HMAC-SHA256. The Keystore key cannot leave the phone, so a backup that carries
 * sessions to a new phone is protected by a password the user chooses instead.
 */
object BackupCrypto {
    private val random = SecureRandom()
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    fun seal(plain: ByteArray, password: CharArray): Sealed {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(TAG_BITS, iv))
        return Sealed(
            encoder.encodeToString(salt),
            encoder.encodeToString(iv),
            encoder.encodeToString(cipher.doFinal(plain))
        )
    }

    /** The original data, or null if the password is wrong or the data was altered. */
    fun open(sealed: Sealed, password: CharArray): ByteArray? {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(password, decoder.decode(sealed.salt)),
            GCMParameterSpec(TAG_BITS, decoder.decode(sealed.iv))
        )
        return try {
            cipher.doFinal(decoder.decode(sealed.data))
        } catch (_: AEADBadTagException) {
            null
        }
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA256"
        ).generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }
}
