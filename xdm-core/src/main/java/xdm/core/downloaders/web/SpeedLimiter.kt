// Copyright 2026 Subhra Das Gupta
// SPDX-License-Identifier: Apache-2.0

package xdm.core.downloaders.web

import xdm.core.CoreConfig
import xdm.core.util.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

/**
 * Aggregate, per-download speed limiter. All segment threads report the cumulative
 * downloaded-byte count; the budget is shared instead of being reset by each thread.
 */
class SpeedLimiter(val config: CoreConfig) {
    private val wake = CountDownLatch(1)
    private val disabled = AtomicBoolean(false)
    private var baselineNs = 0L
    private var baselineBytes = 0L
    private var maxDownloaded = 0L
    private var activeLimit = 0

    /**
     * Use a cumulative deadline on a monotonic clock. Resetting lastTick after each sleep
     * previously let simultaneous segments repeatedly consume a fresh allowance, causing
     * a 2 MB / 512 KB/s download to finish in about 2 seconds instead of 4 seconds.
     */
    @Synchronized
    fun throttleIfNeeded(downloaded: Long) {
        if (disabled.get()) return
        val limit = getSpeedLimit()
        if (limit <= 0) {
            activeLimit = 0
            baselineNs = 0L
            baselineBytes = 0L
            maxDownloaded = 0L
            return
        }

        maxDownloaded = maxOf(maxDownloaded, downloaded)
        if (baselineNs == 0L || activeLimit != limit) {
            activeLimit = limit
            baselineNs = System.nanoTime()
            baselineBytes = maxDownloaded
            return
        }

        val bytesSinceBaseline = (maxDownloaded - baselineBytes).coerceAtLeast(0L)
        val expectedNs = bytesSinceBaseline.toDouble() * 1_000_000_000.0 / (limit.toDouble() * 1024.0)
        val elapsedNs = (System.nanoTime() - baselineNs).coerceAtLeast(0L)
        val remainingNs = expectedNs - elapsedNs
        if (remainingNs <= 0.0) return

        try {
            // Wait is interruptible by disable() and therefore by download cancellation.
            wake.await(ceil(remainingNs).toLong(), TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            Logger.error(e)
        }
    }

    fun disable() {
        disabled.set(true)
        wake.countDown()
    }

    private fun getSpeedLimit(): Int =
        if (config.speedLimiterEnabled && config.speedLimit > 0) config.speedLimit else 0
}
