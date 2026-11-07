// Copyright 2026 Subhra Das Gupta
// SPDX-License-Identifier: Apache-2.0

package xdm.core.network.http

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Transitional obfuscation for personal builds. The fixed key is embedded in the binary:
 * it cannot protect saved credentials from anyone who can inspect the application.
 *
 * Always uses a fresh, random 96-bit nonce. Stored values are versioned for migration
 * to an operating-system credential vault.
 */
object ProxyPasswordCipher {
    private val key = SecretKeySpec(
        MessageDigest.getInstance("SHA-256")
            .digest("xdm-private-fork-proxy-credential-key-v1".toByteArray(StandardCharsets.UTF_8)),
        "AES"
    )
    private val random = SecureRandom()

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val nonce = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        return "v1:" + Base64.getEncoder().encodeToString(nonce + cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8)))
    }

    fun decrypt(saved: String): String {
        if (saved.isEmpty()) return ""
        if (!saved.startsWith("v1:")) return ""
        return runCatching {
            val all = Base64.getDecoder().decode(saved.removePrefix("v1:"))
            require(all.size > 28) { "Invalid encrypted proxy credential" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, all.copyOfRange(0, 12)))
            String(cipher.doFinal(all.copyOfRange(12, all.size)), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }
}
