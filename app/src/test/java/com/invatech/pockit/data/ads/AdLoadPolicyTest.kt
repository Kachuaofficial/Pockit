package com.invatech.pockit.data.ads

import org.junit.Assert.*
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError.ErrorCode
import org.junit.Test

class AdLoadPolicyTest {
    @Test fun transientFailuresBackOffAndStop() {
        for (code in listOf(ErrorCode.INTERNAL_ERROR, ErrorCode.NETWORK_ERROR, ErrorCode.TIMEOUT)) {
            assertEquals(30_000L, AdLoadPolicy.retryDelayMillis(code, 1))
            assertEquals(60_000L, AdLoadPolicy.retryDelayMillis(code, 2))
            assertNull(AdLoadPolicy.retryDelayMillis(code, 3))
            assertNull(AdLoadPolicy.retryDelayMillis(code, 0))
        }
    }
    @Test fun terminalFailuresDoNotGenerateRequestLoops() {
        for (code in ErrorCode.values().filter { it !in listOf(ErrorCode.INTERNAL_ERROR, ErrorCode.NETWORK_ERROR, ErrorCode.TIMEOUT) }) {
            assertNull(AdLoadPolicy.retryDelayMillis(code, 1))
        }
    }
    @Test fun cacheExpiresAtTheBoundary() {
        val loaded = 1000L
        assertFalse(AdLoadPolicy.isCacheExpired(loaded, loaded + AdLoadPolicy.CACHE_TTL_MS - 1))
        assertTrue(AdLoadPolicy.isCacheExpired(loaded, loaded + AdLoadPolicy.CACHE_TTL_MS))
    }
    @Test fun deepSleepDoesNotExtendAdLifetime() {
        assertTrue(AdLoadPolicy.isCacheExpired(1000L, 1000L + 8 * 60 * 60_000L))
    }
    @Test fun unexpectedClockRollbackDiscardsCacheSafely() {
        assertTrue(AdLoadPolicy.isCacheExpired(1000L, 999L))
    }
}
