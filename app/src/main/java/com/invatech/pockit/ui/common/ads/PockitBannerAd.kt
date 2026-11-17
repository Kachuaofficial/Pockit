package com.invatech.pockit.ui.common.ads

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.invatech.pockit.data.ads.AdConfig
import com.invatech.pockit.data.ads.AdManager

@Composable
fun PockitBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.BANNER_AD_UNIT_ID,
    placementKey: String = "banner"
) {
    val ready by AdManager.adsReady.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    val slots = LocalScreenAdSlots.current ?: rememberScreenAdSlots()
    val configuration = LocalConfiguration.current
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        var width = maxWidth.value.toInt()
        if (width < 120) return@BoxWithConstraints
        val size = remember(width, configuration.orientation, configuration.screenHeightDp, context) {
            AdSize.getInlineAdaptiveBannerAdSize(width, 60)
        }
        val slot = slots.banner(placementKey, adUnitId, size) ?: return@BoxWithConstraints
        DisposableEffect(slot) {
            slot.mount()
            onDispose { slot.unmount() }
        }
        key(slot) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(vertical = if (slot.failed) 0.dp else 12.dp)
                    .height(if (slot.failed) 0.dp else slot.heightDp.dp),
                factory = { ctx -> FrameLayout(ctx).apply {
                    (slot.view.parent as? ViewGroup)?.removeView(slot.view)
                    addView(slot.view, FrameLayout.LayoutParams(
                        (slot.widthDp * ctx.resources.displayMetrics.density).toInt(),
                        (slot.heightDp * ctx.resources.displayMetrics.density).toInt(), Gravity.CENTER))
                } },
                update = { container ->
                    slot.view.layoutParams = FrameLayout.LayoutParams(
                        (slot.widthDp * container.resources.displayMetrics.density).toInt(),
                        (slot.heightDp * container.resources.displayMetrics.density).toInt(), Gravity.CENTER)
                },
                // The screen owns the AdView. Recycling only detaches it; SDK refresh stops offscreen.
                onRelease = { it.removeAllViews() }
            )
        }
    }
}
