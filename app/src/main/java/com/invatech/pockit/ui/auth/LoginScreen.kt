package com.invatech.pockit.ui.auth

/*
 * Google sign-in interface with loading/error feedback and links to the legal documents.
 * Obtains the Google token through GoogleAuthClient, then delegates Firebase sign-in to AuthViewModel.
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.invatech.pockit.R
import com.invatech.pockit.data.auth.GoogleAuthClient
import com.invatech.pockit.ui.common.legal.LegalBottomSheet
import com.invatech.pockit.ui.common.legal.LegalDocType
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.theme.CocoaSoft
import com.invatech.pockit.ui.theme.PastelLilac
import com.invatech.pockit.ui.theme.PastelMint
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PockitCharcoal
import com.invatech.pockit.ui.theme.WarmCream
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val client = remember(context) { GoogleAuthClient(context) }
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    var legal by remember { mutableStateOf<LegalDocType?>(null) }

    LoginContent(
        loading = state.isLoading || picking,
        error = pickerError ?: state.errorMessage,
        onSignIn = {
            if (!picking && !state.isLoading) {
                scope.launch {
                    picking = true
                    pickerError = null
                    viewModel.clearError()
                    try {
                        viewModel.signInWithGoogleToken(client.signIn())
                    } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
                        // User cancelled Google selection sheet - silent ignore
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (exception: Exception) {
                        // Log only the exception type; provider messages can contain account identifiers.
                        android.util.Log.w("GoogleAuth", "Credential request failed: " + exception.javaClass.simpleName)
                        pickerError = com.invatech.pockit.data.auth.googleSignInErrorMessage(exception)
                    } finally {
                        picking = false
                    }
                }
            }
        },
        onTerms = { legal = LegalDocType.TERMS_OF_SERVICE },
        onPrivacy = { legal = LegalDocType.PRIVACY_POLICY }
    )

    legal?.let { LegalBottomSheet(initialDocType = it, onDismiss = { legal = null }) }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onLoginSuccess()
    }
}

@Composable
internal fun LoginContent(
    loading: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatching { ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap(120, 120)?.asImageBitmap() }.getOrNull()
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(WarmCream).safeDrawingPadding()) {
        val compact = maxHeight < 700.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = if (compact) 16.dp else 28.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (appIcon != null) Image(appIcon, null, Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
                else Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(32.dp), tint = com.invatech.pockit.ui.theme.PockitOrange)
                Text("Pockit", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = com.invatech.pockit.ui.theme.PockitText)
            }
            Spacer(Modifier.height(if (compact) 24.dp else 40.dp))
            Text("Money sorted.\nLife, a little lighter.", fontSize = if (compact) 32.sp else 40.sp,
                lineHeight = if (compact) 37.sp else 45.sp, letterSpacing = (-1.5).sp,
                fontWeight = FontWeight.ExtraBold, color = com.invatech.pockit.ui.theme.PockitText)
            Spacer(Modifier.height(12.dp))
            Text("Track your spending, split with friends,\nand make room for your next big plan.",
                style = MaterialTheme.typography.bodyMedium, color = CocoaSoft)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (maxHeight >= 70.dp) {
                    val artworkScale = (maxHeight.value / 285f).coerceAtMost(1f)
                    Box(Modifier.fillMaxWidth().requiredHeight(255.dp).graphicsLayer {
                        scaleX = artworkScale; scaleY = artworkScale
                    }) { LoginArtwork() }
                }
            }
            Text("YOUR EVERYDAY MONEY, IN ONE PLACE", style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp, color = CocoaSoft)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onSignIn, enabled = !loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.invatech.pockit.ui.theme.PockitOrange, contentColor = WarmCream,
                    disabledContainerColor = PockitCharcoal.copy(alpha = .8f), disabledContentColor = com.invatech.pockit.ui.theme.PockitMutedText),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = com.invatech.pockit.ui.theme.PockitMutedText, strokeWidth = 2.dp)
                else Surface(color = Color.White, shape = CircleShape) {
                    Icon(painterResource(R.drawable.ic_google_logo), null, Modifier.padding(5.dp).size(20.dp), tint = Color.Unspecified)
                }
                Spacer(Modifier.width(12.dp))
                Text(if (loading) "Signing in…" else "Continue with Google", style = MaterialTheme.typography.titleSmall)
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(10.dp))
            Text("Sign in to sync your records across devices.", style = MaterialTheme.typography.bodySmall,
                color = CocoaSoft, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTerms) { Text("Terms", color = CocoaSoft) }
                Text("·", color = CocoaSoft)
                TextButton(onClick = onPrivacy) { Text("Privacy Policy", color = CocoaSoft) }
            }
        }
    }
}

@Composable
private fun LoginArtwork() {
    val motion by MotionSettings.enabled.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val active = motion && lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val drift = if (active) {
        rememberInfiniteTransition(label = "login_float").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "login_drift"
        )
    } else rememberUpdatedState(0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative warm ambient shapes
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = PastelPeach.copy(alpha = 0.55f),
                radius = size.minDimension * 0.44f,
                center = Offset(size.width * 0.20f, size.height * 0.42f)
            )
            drawCircle(
                color = PastelMint.copy(alpha = 0.55f),
                radius = size.minDimension * 0.38f,
                center = Offset(size.width * 0.82f, size.height * 0.32f)
            )
        }

        // Main Financial Overview Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    rotationZ = -3.5f + (drift.value * 1.2f)
                    translationY = drift.value * 6.dp.toPx()
                }
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x28000000),
                    ambientColor = Color(0x12000000)
                ),
            color = Color(0xFF19191E), // Deep Emerald Teal
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONTHLY PLAN",
                        color = Color(0xFFB6B6BF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2D2119))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF850F))
                        )
                        Text(
                            text = "On track",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFB454)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "₹24,850",
                        color = com.invatech.pockit.ui.theme.PockitText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Safe to spend · 8 days remaining",
                        color = Color(0xFFB6B6BF),
                        fontSize = 12.5.sp
                    )
                }

                // Mini progress spending bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(36.dp)
                ) {
                    val barHeights = listOf(16, 26, 20, 34, 28, 36, 30)
                    barHeights.forEachIndexed { i, h ->
                        val isLast = i == barHeights.lastIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h.dp)
                                .background(
                                    if (isLast) Color(0xFFFFB672) else Color(0xFFFF850F).copy(alpha = 0.85f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }

        // Floating Micro Toast Card on top right
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 4.dp)
                .graphicsLayer {
                    rotationZ = 4f
                    translationY = drift.value * -5.dp.toPx()
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color(0x30000000)
                ),
            color = PastelLilac,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF333338))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFFB454)
                )
                Text(
                    text = "Chai with team · ₹40 split",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F7)
                )
            }
        }
    }
}
