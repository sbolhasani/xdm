// SPDX-License-Identifier: Apache-2.0

package xdm.core.network

/**
 * Proxy policy for an individual download. INHERIT uses the current application-wide policy;
 * SYSTEM consults the operating system selector for the requested destination.
 *
 * Credentials are deliberately not part of this persisted model.
 */
enum class ProxyMode {
    INHERIT,
    DIRECT,
    SYSTEM,
    HTTP,
    SOCKS,
}

/**
 * Host and port are needed only by HTTP and SOCKS. Other policies do not require an endpoint.
 * A missing per-download preference is represented by INHERIT for old download records.
 */
data class ProxyConfiguration(
    val mode: ProxyMode = ProxyMode.INHERIT,
    val host: String = "",
    val port: Int = 0,
) {
    init {
        if (mode == ProxyMode.HTTP || mode == ProxyMode.SOCKS) {
            require(host.isNotBlank()) { "A manual proxy needs a host" }
            require(port in 1..65535) { "A manual proxy needs a port in 1..65535" }
        }
    }

    /** Resolve inheritance once, while creating the connection, not when persisting the task. */
    fun effective(global: ProxyConfiguration): ProxyConfiguration =
        if (mode == ProxyMode.INHERIT) {
            require(global.mode != ProxyMode.INHERIT) { "Global proxy must be explicit" }
            global
        } else {
            this
        }
}
