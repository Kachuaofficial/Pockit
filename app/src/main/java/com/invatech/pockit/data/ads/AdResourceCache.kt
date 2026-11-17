package com.invatech.pockit.data.ads

internal interface CachedAdResource {
    val inUse: Boolean
    val isClosed: Boolean
    fun close()
}

/** Bounded, screen-owned LRU. An ad currently attached to a view is never evicted. Main thread only. */
internal class AdResourceCache<K, V : CachedAdResource>(private val capacity: Int) {
    private val entries = LinkedHashMap<K, V>(capacity, 0.75f, true)
    init { require(capacity > 0) }
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
