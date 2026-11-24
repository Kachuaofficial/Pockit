package com.invatech.pockit.ui.commerce

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.invatech.pockit.data.ads.*
import com.invatech.pockit.ui.common.rememberOnline
import com.invatech.pockit.ui.finance.PageHeading
import com.invatech.pockit.ui.home.components.CinematicCard
import com.invatech.pockit.ui.theme.*
import java.text.DateFormat
import java.util.Date

@Composable
fun PerksScreen(onBack: () -> Unit, focus: RewardedFeature? = null) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val controller = remember { RewardedFeatureController() }
    val ad by controller.state.collectAsState()
    val adsReady by AdManager.adsReady.collectAsState()
    val online = rememberOnline()
    val accessVersion by RewardedFeatureAccess.updates.collectAsState()

    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(adsReady, online) { if (adsReady && online) controller.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageHeading("Feature access", "Unlock selected tools for 24 hours by viewing a rewarded ad.", onBack)
        }
        item {
            Surface(
                color = PastelLilac,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, PockitPurple.copy(alpha = .2f))
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(color = PastelYellow, shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.padding(12.dp).size(28.dp), tint = PockitPurple)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ad-supported access", style = MaterialTheme.typography.titleLarge)
                        Text("No subscription is required. Access begins after the rewarded ad is completed.", color = PockitSecondaryText)
                    }
                }
            }
        }
        if (!online) item {
            CinematicCard {
                Text("You’re offline", style = MaterialTheme.typography.titleLarge)
                Text("An internet connection is required to load ads. Core tracking and saved records remain available offline.", color = PockitSecondaryText)
            }
        }
        items(RewardedFeature.entries.sortedBy { if (it == focus) 0 else 1 }, key = { it.name }) { feature ->
            val expiry = remember(accessVersion, feature) { RewardedFeatureAccess.expiresAt(feature) }
            val unlocked = remember(accessVersion, feature) { RewardedFeatureAccess.isUnlocked(feature) }
            CinematicCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle,
                        null,
                        Modifier.size(28.dp),
                        tint = if (unlocked) PockitGreen else PockitOrangeDeep
                    )
                    Column(Modifier.weight(1f)) {
                        Text(feature.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (unlocked) "Active until ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(expiry))}" else "Access duration: 24 hours", color = if (unlocked) PockitGreen else PockitPurple)
                    }
                }
                Text(feature.description, color = PockitSecondaryText)
                Button(
                    onClick = {
                        if (ad.ready && activity != null) controller.show(activity, feature) else controller.load()
                    },
                    enabled = online && adsReady && !ad.loading && !ad.showing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(when {
                        ad.showing -> "Ad in progress…"
                        ad.loading -> "Loading ad…"
                        ad.ready -> if (unlocked) "Watch ad · extend by 24 hours" else "Watch ad · unlock for 24 hours"
                        !adsReady -> "Rewarded ads unavailable"
                        else -> "Load rewarded ad"
                    })
                }
            }
        }
        ad.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        ad.message?.let { message -> item { Text(message, color = PockitGreen, style = MaterialTheme.typography.titleMedium) } }
        item {
            Text(
                "Rewarded ads are optional. Closing an ad before completion does not unlock access. Banner and native ads support the free app.",
                color = PockitMutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RewardedFeatureDialog(feature: RewardedFeature, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = PockitBackground) {
            Box(Modifier.safeDrawingPadding()) { PerksScreen(onBack = onDismiss, focus = feature) }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
