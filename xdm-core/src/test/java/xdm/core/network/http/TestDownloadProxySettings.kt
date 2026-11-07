// Copyright 2026 Subhra Das Gupta
// SPDX-License-Identifier: Apache-2.0

package xdm.core.network.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Proxy

class TestDownloadProxySettings {
    @Test
    fun inheritedAndDirectModesDoNotCreateExplicitProxy() {
        assertNull(DownloadProxySettings().explicitProxy())
        assertNull(DownloadProxySettings(DownloadProxyMode.DIRECT).explicitProxy())
        assertNull(DownloadProxySettings(DownloadProxyMode.SYSTEM).explicitProxy())
    }

    @Test
    fun httpAndSocksModesRetainEndpointAndType() {
        for ((mode, type) in listOf(
            DownloadProxyMode.HTTP to Proxy.Type.HTTP,
            DownloadProxyMode.SOCKS to Proxy.Type.SOCKS,
        )) {
            val proxy = DownloadProxySettings(mode, "proxy.example", 3128).explicitProxy()!!
            assertEquals(type, proxy.type())
            val address = proxy.address() as InetSocketAddress
            assertEquals("proxy.example", address.hostString)
            assertEquals(3128, address.port)
            assertEquals(true, address.isUnresolved)
        }
    }

    @Test
    fun explicitModeRejectsInvalidEndpoint() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadProxySettings(DownloadProxyMode.HTTP, "", 8080)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DownloadProxySettings(DownloadProxyMode.SOCKS, "localhost", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DownloadProxySettings(DownloadProxyMode.HTTP, "localhost", 65536)
        }
    }
}
