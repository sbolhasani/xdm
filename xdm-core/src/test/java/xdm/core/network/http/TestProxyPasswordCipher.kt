// Copyright 2026 Subhra Das Gupta
// SPDX-License-Identifier: Apache-2.0

package xdm.core.network.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TestProxyPasswordCipher {
    @Test
    fun roundTripUsesRandomNonceAndDoesNotStorePlaintext() {
        val secret = "secret-سلام-123"
        val first = ProxyPasswordCipher.encrypt(secret)
        val second = ProxyPasswordCipher.encrypt(secret)
        assertFalse(first.contains(secret))
        assertNotEquals(first, second)
        assertEquals(secret, ProxyPasswordCipher.decrypt(first))
        assertEquals(secret, ProxyPasswordCipher.decrypt(second))
    }

    @Test
    fun corruptedCiphertextIsRejected() {
        assertEquals("", ProxyPasswordCipher.decrypt("invalid"))
        assertEquals("", ProxyPasswordCipher.decrypt("v1:xyz"))
        assertEquals("", ProxyPasswordCipher.decrypt(""))
    }
}
