package com.invatech.pockit.data.ads

/*
 * Unit tests for cache capacity, idle eviction, visible-resource retention and cleanup.
 */

import org.junit.Assert.*
import org.junit.Test

class AdResourceCacheTest {
    private class Resource(override var inUse: Boolean = false) : CachedAdResource {
        var closeCount = 0
        override val isClosed get() = closeCount > 0
        override fun close() { closeCount++ }
    }
    @Test fun scrollBackReusesResourceWithoutAnotherFactoryCall() {
        val cache = AdResourceCache<String, Resource>(2)
        var loads = 0
        val first = cache.getOrCreate("home.native") { loads++; Resource() }
        repeat(20) { assertSame(first, cache.getOrCreate("home.native") { loads++; Resource() }) }
        assertEquals(1, loads)
    }
    @Test fun evictsLeastRecentlyUsedIdleResource() {
        val cache = AdResourceCache<String, Resource>(2)
        val first = cache.getOrCreate("a") { Resource() }!!
        val second = cache.getOrCreate("b") { Resource() }!!
        cache.getOrCreate("a") { error("must reuse") }
        cache.getOrCreate("c") { Resource() }
        assertEquals(0, first.closeCount)
        assertEquals(1, second.closeCount)
    }
    @Test fun neverEvictsAttachedAdsOrStartsAnotherLoadAtCapacity() {
        val cache = AdResourceCache<String, Resource>(1)
        val first = cache.getOrCreate("a") { Resource(inUse = true) }!!
        assertNull(cache.getOrCreate("b") { error("no spare capacity") })
        assertEquals(0, first.closeCount)
        first.inUse = false
        cache.getOrCreate("b") { Resource() }
        assertEquals(1, first.closeCount)
    }
    @Test fun privacyAndScreenDisposalClearEverythingOnce() {
        val cache = AdResourceCache<String, Resource>(2)
        val active = cache.getOrCreate("a") { Resource(inUse = true) }!!
        val idle = cache.getOrCreate("b") { Resource() }!!
        cache.evictIdle()
        assertEquals(1, idle.closeCount)
        assertEquals(0, active.closeCount)
        cache.clear(); cache.clear()
        assertEquals(1, active.closeCount)
        assertEquals(1, idle.closeCount)
    }
    @Test fun closedResourceIsNeverReused() {
        val cache = AdResourceCache<String, Resource>(1)
        val first = cache.getOrCreate("a") { Resource() }!!
        first.close()
        assertNotSame(first, cache.getOrCreate("a") { Resource() })
    }
}
