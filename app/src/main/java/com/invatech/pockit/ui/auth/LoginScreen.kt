package com.invatech.pockit.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.data.auth.GoogleAuthClient
import com.invatech.pockit.ui.common.legal.LegalBottomSheet
import com.invatech.pockit.ui.common.legal.LegalDocType
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.LavenderDeep
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.Peach
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val googleAuthClient = remember(context) {
        GoogleAuthClient(context)
    }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var appeared by remember { mutableStateOf(false) }
    var showLegalSheet by remember { mutableStateOf(false) }
    var legalDocType by remember { mutableStateOf(LegalDocType.TERMS_OF_SERVICE) }
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---------- Hero ----------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp)
        ) {
            HeroCard(appeared = appeared)
        }

        // ---------- Bottom sheet ----------
        AnimatedVisibility(
            visible = appeared,
            enter = fadeIn(tween(500)) + slideInVertically(tween(600)) { it / 3 }
        ) {
            Surface(
                color = Ink,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your money,\nbeautifully simple.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Track spending, set goals and see where every rupee goes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ExpressiveLoadingIndicator(
                                    size = 32.dp,
                                    customColor = MintDeep,
                                    showHalo = true
                                )
                                Text(
                                    text = "Signing in...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.clearError()

                                    try {
                                        val idToken = googleAuthClient.signIn()
                                        viewModel.signInWithGoogleToken(idToken)
                                    } catch (exception: Exception) {
                                        // Account picker cancellation or sign-in error
                                        viewModel.clearError()
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Mint,
                                contentColor = Ink
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Ink,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    uiState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "By continuing you agree to our ",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                            Text(
                                text = "Terms",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = Mint,
                                modifier = Modifier.clickable {
                                    legalDocType = LegalDocType.TERMS_OF_SERVICE
                                    showLegalSheet = true
                                }
                            )
                            Text(
                                text = " & ",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = Mint,
                                modifier = Modifier.clickable {
                                    legalDocType = LegalDocType.PRIVACY_POLICY
                                    showLegalSheet = true
                                }
                            )
                        }

                        Text(
                            text = "© 2026 Pockit Technologies Inc.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = InkMuted.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showLegalSheet) {
        LegalBottomSheet(
            initialDocType = legalDocType,
            onDismiss = { showLegalSheet = false }
        )
    }

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onLoginSuccess()
        }
    }
}

@Composable
private fun HeroCard(appeared: Boolean) {
    Surface(
        color = Mint,
        shape = RoundedCornerShape(36.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {

            // Brand row
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Mint,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "POCKIT",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp
                )
            }

            // Floating chips
            Chip(
                text = "+ 6.2%",
                background = Lavender,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 4.dp)
            )
            Chip(
                text = "₹ 12,788",
                background = Peach,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-6).dp)
            )

            // Mini bar chart
            MiniBars(
                appeared = appeared,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, modifier: Modifier = Modifier) {
    Surface(
        color = background,
        shape = CircleShape,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun MiniBars(appeared: Boolean, modifier: Modifier = Modifier) {
    val targets = listOf(0.4f, 0.65f, 0.5f, 0.95f, 0.6f, 0.8f, 0.55f)
    val colors = listOf(Ink, Ink, LavenderDeep, Ink, Ink, MintDeep, Ink)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        targets.forEachIndexed { index, target ->
            val fraction by animateFloatAsState(
                targetValue = if (appeared) target else 0.05f,
                animationSpec = tween(durationMillis = 700, delayMillis = index * 90),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(colors[index])
            )
        }
    }
}