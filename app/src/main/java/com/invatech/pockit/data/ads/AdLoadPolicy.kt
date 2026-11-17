package com.invatech.pockit.data.ads

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/** Only transient failures retry, with bounded backoff; never refresh a loaded ad manually. */
internal object AdLoadPolicy {
    const val CACHE_TTL_MS = 55 * 60_000L
    // elapsedRealtime includes deep sleep; Handler delays alone do not.
    fun isCacheExpired(loadedAt: Long, now: Long): Boolean = now < loadedAt || now - loadedAt >= CACHE_TTL_MS

    fun retryDelayMillis(errorCode: LoadAdError.ErrorCode, attempts: Int): Long? {
        val transient = errorCode == LoadAdError.ErrorCode.INTERNAL_ERROR ||
            errorCode == LoadAdError.ErrorCode.NETWORK_ERROR || errorCode == LoadAdError.ErrorCode.TIMEOUT
        return if (transient && attempts in 1..2) 30_000L * attempts else null
    }
}
