package com.invatech.pockit.ui.common.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.invatech.pockit.data.ads.AdConfig
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Lavender

/**
 * Production-ready, non-intrusive AdMob Banner widget.
 * Follows Google Play "Clear Ad Attribution" policies with a subtle "Sponsored" indicator.
 * Collapses smoothly to zero height if no ad is available so the user never sees empty gaps.
 */
@Composable
fun PockitBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.BANNER_AD_UNIT_ID
) {
    var isAdLoaded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAdLoaded) Modifier.padding(vertical = 8.dp) else Modifier.height(0.dp)),
        shape = RoundedCornerShape(20.dp),
        color = if (isAdLoaded) CloudDeep.copy(alpha = 0.5f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isAdLoaded) Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    else Modifier.height(0.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAdLoaded) {
                // Clear policy-compliant ad disclosure tag
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Lavender)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sponsored",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // AndroidView MUST remain in the composition so loadAd() executes and onAdLoaded can fire.
            // When not loaded yet, its container is collapsed to height(0.dp).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isAdLoaded) Modifier else Modifier.height(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            this.adUnitId = adUnitId
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    Log.d("PockitBannerAd", "Banner ad loaded successfully!")
                                    isAdLoaded = true
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    super.onAdFailedToLoad(error)
                                    Log.w(
                                        "PockitBannerAd",
                                        "Banner ad failed to load: ${error.message} (code: ${error.code})"
                                    )
                                    isAdLoaded = false
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}
