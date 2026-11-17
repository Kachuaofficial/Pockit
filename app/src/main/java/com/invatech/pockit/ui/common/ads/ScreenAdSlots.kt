package com.invatech.pockit.ui.common.ads

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.banner.*
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.*
import com.invatech.pockit.data.ads.*

internal val LocalScreenAdSlots = staticCompositionLocalOf<ScreenAdSlots?> { null }

/** Owned above LazyColumn: item recycling releases a view lease, not the underlying loaded ad. */
@Composable
internal fun rememberScreenAdSlots(): ScreenAdSlots {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val allowed by AdManager.adsReady.collectAsState()
    val slots = remember(context, lifecycle, allowed) { ScreenAdSlots(context, lifecycle) }
    DisposableEffect(slots) { onDispose { slots.close() } }
    return slots
}

internal class ScreenAdSlots(private val context: Context, private val lifecycle: Lifecycle) {
    private val capacity = if ((context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice) 1 else 2
    private val banners = AdResourceCache<String, BannerSlot>(capacity)
    private val natives = AdResourceCache<String, NativeSlot>(capacity)
    private val callbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        override fun onLowMemory() { banners.evictIdle(); natives.evictIdle() }
        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory()
        }
    }
    init { context.applicationContext.registerComponentCallbacks(callbacks) }
    fun banner(key: String, unit: String, size: AdSize): BannerSlot? =
        banners.getOrCreate("$key:$unit:${size.width}:${size.height}") { BannerSlot(context, lifecycle, unit, size) }
    fun native(key: String, unit: String): NativeSlot? =
        natives.getOrCreate("$key:$unit") { NativeSlot(lifecycle, unit) }
    fun close() {
        context.applicationContext.unregisterComponentCallbacks(callbacks)
        banners.clear(); natives.clear()
    }
}

internal class BannerSlot(context: Context, private val lifecycle: Lifecycle, unit: String, size: AdSize) : CachedAdResource {
    val view = AdView(context)
    var widthDp by mutableIntStateOf(size.width)
        private set
    var heightDp by mutableIntStateOf(if (size.height > 0) size.height else 60)
        private set
    var failed by mutableStateOf(false)
        private set
    private var attached = false
    private var requested = false
    private var loadedAt = 0L
    private var attempts = 0
    private var nextAttemptAt = Long.MAX_VALUE
    private val handler = Handler(Looper.getMainLooper())
    private var idleExpiry: Runnable? = null
    override var isClosed = false
        private set
    override val inUse get() = attached
    private val request = BannerAdRequest.Builder(unit, size).build()
    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START && attached) mount()
        if (event == Lifecycle.Event.ON_STOP) view.visibility = View.INVISIBLE
    }
    init { lifecycle.addObserver(observer) }
    private fun updateSize(ad: BannerAd) {
        val actual = ad.getAdSize()
        if (actual.width > 0) widthDp = actual.width
        if (actual.height > 0) heightDp = actual.height
        Log.d("PockitAds", "banner size=${widthDp}x${heightDp}dp")
    }
    fun mount() {
        if (isClosed) return
        if (loadedAt != 0L && AdLoadPolicy.isCacheExpired(loadedAt, SystemClock.elapsedRealtime())) {
            failed = true
            close()
            return
        }
        attached = true
        idleExpiry?.let(handler::removeCallbacks)
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        view.visibility = View.VISIBLE
        if (requested && failed && SystemClock.elapsedRealtime() >= nextAttemptAt) {
            requested = false
            failed = false
        }
        if (requested) {
            Log.d("PockitAds", "banner slot reused without a new request")
            return
        }
        requested = true
        attempts++
        view.loadAd(request, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                handler.post {
                    if (isClosed) { ad.destroy(); return@post }
                    updateSize(ad)
                    ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            handler.post { if (!isClosed) {
                                view.getBannerAd()?.let { updateSize(it) }
                                loadedAt = SystemClock.elapsedRealtime()
                                failed = false
                            } }
                        }
                        override fun onAdFailedToRefresh(adError: LoadAdError) {
                            if (!isClosed) AdManager.reportLoadFailure("banner refresh", adError)
                        }
                    }
                    loadedAt = SystemClock.elapsedRealtime()
                    attempts = 0
                    nextAttemptAt = Long.MAX_VALUE
                    failed = false
                    Log.d("PockitAds", "banner loaded; responseId=${ad.getResponseInfo().responseId}")
                }
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                handler.post { if (!isClosed) {
                    failed = true
                    val delay = AdLoadPolicy.retryDelayMillis(adError.code, attempts)
                    nextAttemptAt = SystemClock.elapsedRealtime() + (delay ?: 5 * 60_000L)
                    if (delay == null) attempts = 0
                    AdManager.reportLoadFailure("banner", adError)
                } }
            }
        })
    }
    fun unmount() {
        if (isClosed) return
        attached = false
        view.visibility = View.INVISIBLE
        // SDK refresh runs only while visible. No timer issues new banner requests.
        idleExpiry?.let(handler::removeCallbacks)
        val age = if (loadedAt == 0L) 0L else SystemClock.elapsedRealtime() - loadedAt
        val remaining = (AdLoadPolicy.CACHE_TTL_MS - age).coerceIn(0L, 5 * 60_000L)
        idleExpiry = Runnable { if (!attached) close() }.also { handler.postDelayed(it, remaining) }
    }
    override fun close() {
        if (isClosed) return
        isClosed = true
        attached = false
        idleExpiry?.let(handler::removeCallbacks)
        idleExpiry = null
        lifecycle.removeObserver(observer)
        view.destroy()
    }
}

internal class NativeSlot(private val lifecycle: Lifecycle, private val unit: String) : CachedAdResource {
    var ad by mutableStateOf<NativeAd?>(null)
        private set
    var generation by mutableIntStateOf(0)
        private set
    var boundView: NativeAdView? = null
    private var attached = false
    private var loadedAt = 0L
    private var loading = false
    private var wanted = false
    private var attempts = 0
    private var nextAttemptAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var retry: Runnable? = null
    private var expiry: Runnable? = null
    override var isClosed = false
        private set
    override val inUse get() = attached
    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            expireIfStale()
            if (wanted) ensureLoaded()
        }
    }
    init { lifecycle.addObserver(observer) }
    fun mount() {
        expireIfStale()
        attached = true
        if (ad != null) Log.d("PockitAds", "native slot reused without a new request")
        ensureLoaded()
    }
    fun unmount() { attached = false }
    fun ensureLoaded() {
        expireIfStale()
        if (isClosed || loading || ad != null) return
        wanted = true
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (SystemClock.elapsedRealtime() < nextAttemptAt) return
        loading = true
        wanted = false
        attempts++
        NativeAdLoader.load(NativeAdRequest.Builder(unit, listOf(NativeAd.NativeAdType.NATIVE)).build(),
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    handler.post {
                        if (isClosed) { nativeAd.destroy(); return@post }
                        loading = false
                        attempts = 0
                        nextAttemptAt = 0L
                        loadedAt = SystemClock.elapsedRealtime()
                        ad = nativeAd
                        Log.d("PockitAds", "native loaded; responseId=${nativeAd.getResponseInfo().responseId}")
                        expiry?.let(handler::removeCallbacks)
                        expiry = Runnable { expireAd() }
                            .also { handler.postDelayed(it, AdLoadPolicy.CACHE_TTL_MS) }
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    handler.post {
                        if (isClosed) return@post
                        loading = false
                        AdManager.reportLoadFailure("native", adError)
                        val delay = AdLoadPolicy.retryDelayMillis(adError.code, attempts)
                        nextAttemptAt = SystemClock.elapsedRealtime() + (delay ?: 5 * 60_000L)
                        if (delay != null) {
                            retry?.let(handler::removeCallbacks)
                            retry = Runnable { ensureLoaded() }.also { handler.postDelayed(it, delay) }
                        } else attempts = 0
                    }
                }
            })
    }
    private fun expireIfStale() {
        if (ad != null && AdLoadPolicy.isCacheExpired(loadedAt, SystemClock.elapsedRealtime())) expireAd()
    }
    private fun expireAd() {
        if (ad == null) return
        expiry?.let(handler::removeCallbacks)
        expiry = null
        boundView?.destroy()
        boundView = null
        ad?.destroy()
        ad = null
        loadedAt = 0L
        generation++
    }
    override fun close() {
        if (isClosed) return
        isClosed = true
        retry?.let(handler::removeCallbacks)
        expiry?.let(handler::removeCallbacks)
        lifecycle.removeObserver(observer)
        boundView?.destroy()
        boundView = null
        ad?.destroy()
        ad = null
    }
}

/** Called only near the upcoming ad slot, never by a background worker or on unrelated pages. */
@Composable
fun PreloadPockitNativeAd(placementKey: String) {
    val ready by AdManager.adsReady.collectAsState()
    if (!ready) return
    val slots = LocalScreenAdSlots.current ?: rememberScreenAdSlots()
    val slot = slots.native(placementKey, AdConfig.NATIVE_AD_UNIT_ID) ?: return
    LaunchedEffect(slot, slot.generation) { slot.ensureLoaded() }
}
