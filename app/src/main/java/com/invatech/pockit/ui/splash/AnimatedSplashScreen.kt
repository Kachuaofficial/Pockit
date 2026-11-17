package com.invatech.pockit.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.invatech.pockit.R
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.WarmCream
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(isCheckingAuth: Boolean, onSplashFinished: () -> Unit) {
    val context = LocalContext.current
    val appIconBitmap = remember(context) {
        try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                ?: context.packageManager.getApplicationIcon(context.packageName)
            drawable.toBitmap(width = 256, height = 256).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    val scaleAnim = remember { Animatable(0.76f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(isCheckingAuth) {
        if (!isCheckingAuth) {
            delay(350)
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                this.scaleX = scaleAnim.value
                this.scaleY = scaleAnim.value
                this.alpha = alphaAnim.value
            }
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color(0x33000000),
                        ambientColor = Color(0x1F000000)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "Pockit Logo",
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pockit",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CocoaEspresso
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "A clearer picture of your money",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = InkMuted
            )

            if (isCheckingAuth) {
                Spacer(modifier = Modifier.height(28.dp))
                ExpressiveLoadingIndicator(size = 32.dp)
            }
        }
    }
}
