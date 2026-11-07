// Copyright 2026 Subhra Das Gupta
// SPDX-License-Identifier: Apache-2.0

package xdm.core.network.http

import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Proxy choice for an individual download. INHERIT keeps using the current app-wide
 * setting; it is deliberately distinct from DIRECT (which bypasses a global proxy).
 *
 * SYSTEM means that the network client should consult the platform ProxySelector
 * for each request URL, rather than caching one proxy server for the entire download.
 */
enum class DownloadProxyMode {
    INHERIT,
    DIRECT,
    SYSTEM,
    HTTP,
    SOCKS,
}

/**
 * Per-download proxy configuration. Credentials are intentionally not included:
 * passwords must not be written to the task's on-disk metadata.
 */
data class DownloadProxySettings(
    val mode: DownloadProxyMode = DownloadProxyMode.INHERIT,
    val host: String = "",
    val port: Int = 8080,
    val username: String = "",
    /** Sensitive: never log or store as cleartext. */
    val password: String = "",
) {
    init {
        if (mode == DownloadProxyMode.HTTP || mode == DownloadProxyMode.SOCKS) {
            require(host.isNotBlank()) { "Explicit proxy requires a host" }
            require(port in 1..65535) { "Proxy port must be between 1 and 65535" }
        }
    }

    override fun toString(): String = "DownloadProxySettings(mode=$mode, host=$host, port=$port, username=$username, password=<redacted>)"

    /** Only explicit proxies have a fixed java.net.Proxy value. */
    fun explicitProxy(): Proxy? = when (mode) {
        DownloadProxyMode.HTTP -> Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port))
        DownloadProxyMode.SOCKS -> Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port))
        else -> null
    }
}
