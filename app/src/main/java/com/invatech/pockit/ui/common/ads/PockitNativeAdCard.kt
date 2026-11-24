package com.invatech.pockit.ui.common.ads

/*
 * Compose/Android-view host for native ad content.
 * Binds a screen-owned native slot and releases the attachment through composition lifecycle callbacks.
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.nativead.*
import com.invatech.pockit.R
import com.invatech.pockit.data.ads.AdConfig
import com.invatech.pockit.data.ads.AdManager

@Composable
fun PockitNativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.NATIVE_AD_UNIT_ID,
    placementKey: String = "native"
) {
    val online = com.invatech.pockit.ui.common.rememberOnline()
    val ready by AdManager.adsReady.collectAsState()
    if (!online || !ready || adUnitId.isBlank()) return
    val slots = LocalScreenAdSlots.current ?: rememberScreenAdSlots()
    val slot = slots.native(placementKey, adUnitId) ?: return
    DisposableEffect(slot) {
        slot.mount()
        onDispose { slot.unmount() }
    }
    LaunchedEffect(slot, slot.generation) { slot.ensureLoaded() }
    val ad = slot.ad ?: return
    Surface(modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(22.dp)) {
        key(ad) {
            AndroidView(modifier = Modifier.fillMaxWidth().padding(8.dp),
                onRelease = { it.removeAllViews() },
                factory = { ctx ->
                    val cached = slot.boundView ?: run {
                val view = LayoutInflater.from(ctx).inflate(R.layout.pockit_native_ad, null) as NativeAdView
                val headline = view.findViewById<TextView>(R.id.ad_headline)
                val body = view.findViewById<TextView>(R.id.ad_body)
                val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
                val icon = view.findViewById<ImageView>(R.id.ad_app_icon)
                val media = view.findViewById<MediaView>(R.id.ad_media)
                val cta = view.findViewById<Button>(R.id.ad_call_to_action)
                view.headlineView = headline
                view.bodyView = body
                view.advertiserView = advertiser
                view.iconView = icon
                view.callToActionView = cta
                headline.text = ad.headline
                body.text = ad.body
                body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
                advertiser.text = ad.advertiser
                advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
                cta.text = ad.callToAction
                cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
                icon.setImageDrawable(ad.icon?.drawable)
                icon.visibility = if (ad.icon == null) View.GONE else View.VISIBLE
                view.registerNativeAd(ad, media)
                slot.boundView = view
                view
                    }
                    FrameLayout(ctx).apply {
                        (cached.parent as? ViewGroup)?.removeView(cached)
                        addView(cached, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    }
                }
            )
        }
    }
}
