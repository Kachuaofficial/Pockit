package com.invatech.pockit.data.ads

/*
 * Bounded screen-owned least-recently-used cache for ad SDK resources.
 * Evicts only idle resources so a view still displaying an ad is not destroyed by cache pressure.
 */

internal interface CachedAdResource {
    val inUse: Boolean
    val isClosed: Boolean
    fun close()
}

/** Bounded, screen-owned LRU. An ad currently attached to a view is never evicted. Main thread only. */
internal class AdResourceCache<K, V : CachedAdResource>(private val capacity: Int) {
    private val entries = LinkedHashMap<K, V>(capacity, 0.75f, true)
    init { require(capacity > 0) }
    /**
     * Reuses an open cached resource or evicts the least recently used idle entry before
     * allocating one. If capacity is full and every entry is in use, returns null instead of
     * destroying an attached ad.
     */
    fun getOrCreate(key: K, factory: () -> V): V? {
        entries[key]?.let { if (!it.isClosed) return it else entries.remove(key) }
        if (entries.size >= capacity) {
            val idle = entries.entries.firstOrNull { !it.value.inUse } ?: return null
            entries.remove(idle.key)
            idle.value.close()
        }
        return factory().also { entries[key] = it }
    }
    fun evictIdle() {
        val idle = entries.filterValues { !it.inUse }.keys.toList()
        idle.forEach { entries.remove(it)?.close() }
    }
    fun clear() { entries.values.forEach { it.close() }; entries.clear() }
}
