package com.invatech.pockit.data.ads

/*
 * Pure cache-age and bounded retry policy for ad slots.
 * Only transient load failures receive retry delays, making policy independently unit-testable.
 */

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/** Only transient failures retry, with bounded backoff; never refresh a loaded ad manually. */
internal object AdLoadPolicy {
    const val CACHE_TTL_MS = 55 * 60_000L
    // elapsedRealtime includes deep sleep; Handler delays alone do not.
    fun isCacheExpired(loadedAt: Long, now: Long): Boolean = now < loadedAt || now - loadedAt >= CACHE_TTL_MS

    /**
     * Returns 30s/60s backoff only for the first two transient error attempts. Null means do not
     * retry, including permanent failures or exhausted attempts.
     */
    fun retryDelayMillis(errorCode: LoadAdError.ErrorCode, attempts: Int): Long? {
        val transient = errorCode == LoadAdError.ErrorCode.INTERNAL_ERROR ||
            errorCode == LoadAdError.ErrorCode.NETWORK_ERROR || errorCode == LoadAdError.ErrorCode.TIMEOUT
        return if (transient && attempts in 1..2) 30_000L * attempts else null
    }
}
