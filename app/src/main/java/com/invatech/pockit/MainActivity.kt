package com.invatech.pockit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.ads.AdManager
import com.invatech.pockit.data.ads.ConsentManager
import com.invatech.pockit.data.biometric.BiometricAuthManager
import com.invatech.pockit.data.notification.FcmTokenManager
import com.invatech.pockit.data.notification.NotificationHelper
import com.invatech.pockit.ui.auth.AuthViewModel
import com.invatech.pockit.ui.auth.LoginScreen
import com.invatech.pockit.ui.common.security.AppLockScreen
import com.invatech.pockit.ui.navigation.PockitApp
import com.invatech.pockit.ui.theme.PockitTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("navigate_to")?.let { route ->
            pendingRoute = route
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        lifecycleScope.launch {
            FcmTokenManager.syncCurrentToken()
        }
    }

    private fun checkNotificationPermissionAndSyncToken() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                lifecycleScope.launch {
                    FcmTokenManager.syncCurrentToken()
                }
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            lifecycleScope.launch {
                FcmTokenManager.syncCurrentToken()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingRoute = intent.getStringExtra("navigate_to")
        NotificationHelper.createNotificationChannel(this)
        biometricAuthManager = BiometricAuthManager(this)

        // Initialize Google AdMob & UMP GDPR consent flow
        val consentManager = ConsentManager(this)
        consentManager.requestConsentAndInit {
            AdManager.initialize(this)
        }

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsState()

            var isAppLocked by remember {
                mutableStateOf(biometricAuthManager.isBiometricEnabled && biometricAuthManager.canAuthenticate())
            }

            // Sync FCM push token when user is logged in
            LaunchedEffect(uiState.isSignedIn) {
                if (uiState.isSignedIn) {
                    checkNotificationPermissionAndSyncToken()
                }
            }

            // Keep the production splash screen active while checking initial auth status
            // to eliminate any cold-start white flash or blank screen
            splashScreen.setKeepOnScreenCondition {
                uiState.isCheckingAuth
            }

            PockitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = uiState.isSignedIn to isAppLocked,
                        animationSpec = tween(
                            durationMillis = com.invatech.pockit.ui.common.motion.M3Duration.Medium3,
                            easing = com.invatech.pockit.ui.common.motion.M3Easing.Emphasized
                        ),
                        label = "auth_screen_crossfade"
                    ) { (isSignedIn, isLocked) ->
                        when {
                            !isSignedIn -> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = {
                                        isAppLocked = biometricAuthManager.isBiometricEnabled && biometricAuthManager.canAuthenticate()
                                        checkNotificationPermissionAndSyncToken()
                                    }
                                )
                            }
                            isLocked -> {
                                AppLockScreen(
                                    onUnlockRequest = {
                                        biometricAuthManager.promptBiometric(
                                            activity = this@MainActivity,
                                            onSuccess = {
                                                isAppLocked = false
                                            }
                                        )
                                    }
                                )
                            }
                            else -> {
                                PockitApp(
                                    initialRoute = pendingRoute,
                                    onRouteHandled = { pendingRoute = null },
                                    onSignOut = {
                                        isAppLocked = true
                                        authViewModel.signOut()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
