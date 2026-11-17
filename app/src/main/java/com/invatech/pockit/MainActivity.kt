package com.invatech.pockit

import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import com.invatech.pockit.ui.common.motion.MotionSettings
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.invatech.pockit.data.update.InAppUpdateManager
import com.invatech.pockit.ui.auth.AuthViewModel
import com.invatech.pockit.ui.auth.LoginScreen
import com.invatech.pockit.ui.common.security.AppLockScreen
import com.invatech.pockit.ui.navigation.PockitApp
import com.invatech.pockit.ui.splash.AnimatedSplashScreen
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PockitTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var consentManager: ConsentManager
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var inAppUpdateManager: InAppUpdateManager
    private var pendingRoute by mutableStateOf<String?>(null)
    private var pendingOpenAddExpense by mutableStateOf(false)

    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User cancelled or update failed
        }
    }

    override fun onResume() {
        super.onResume()
        if (::consentManager.isInitialized) consentManager.requestConsentAndInit { AdManager.initialize(this) }
        MotionSettings.refresh(this)
        com.invatech.pockit.data.notification.PockitEngagementNotificationManager.recordAppUse(this)
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onResumeCheck(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onDestroy()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("navigate_to")?.let { route ->
            pendingRoute = route
        }
        if (intent.getStringExtra("action") == "quick_add" || intent.getBooleanExtra("open_add_transaction", false)) {
            pendingOpenAddExpense = true
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
        if (intent.getStringExtra("action") == "quick_add" || intent.getBooleanExtra("open_add_transaction", false)) {
            pendingOpenAddExpense = true
        }
        NotificationHelper.createNotificationChannel(this)
        biometricAuthManager = BiometricAuthManager(this)

        // Check for Google Play In-App Updates
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdate(this, updateResultLauncher)

        // Initialize Google AdMob & UMP GDPR consent flow
        consentManager = ConsentManager(this)
        consentManager.requestConsentAndInit {
            AdManager.initialize(this)
        }

        setContent(parent = window.decorView.createLifecycleAwareWindowRecomposer(MotionSettings.durationScale, lifecycle)) {
            val onboardingPrefs = remember { getSharedPreferences("pockit_onboarding", 0) }
            var onboarded by remember { mutableStateOf(onboardingPrefs.getBoolean("liquid_v2", false)) }
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

            val isUpdateDownloaded by inAppUpdateManager.isUpdateDownloaded.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

            // Dismiss the static system splash immediately so our animated splash screen takes over smoothly
            splashScreen.setKeepOnScreenCondition { false }

            PockitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(
                            targetState = showSplash,
                            animationSpec = tween(
                                durationMillis = com.invatech.pockit.ui.common.motion.M3Duration.Medium3,
                                easing = com.invatech.pockit.ui.common.motion.M3Easing.Emphasized
                            ),
                            label = "splash_crossfade"
                        ) { isSplashVisible ->
                            if (isSplashVisible) {
                                AnimatedSplashScreen(
                                    isCheckingAuth = uiState.isCheckingAuth,
                                    onSplashFinished = {
                                        showSplash = false
                                    }
                                )
                            } else {
                                Crossfade(
                                    targetState = uiState.isSignedIn to isAppLocked,
                                    animationSpec = tween(
                                        durationMillis = com.invatech.pockit.ui.common.motion.M3Duration.Medium3,
                                        easing = com.invatech.pockit.ui.common.motion.M3Easing.Emphasized
                                    ),
                                    label = "auth_screen_crossfade"
                                ) { (isSignedIn, isLocked) ->
                                    when {
                                        !onboarded -> {
                                            com.invatech.pockit.ui.finance.OnboardingScreen {
                                                onboardingPrefs.edit().putBoolean("liquid_v2", true).apply()
                                                onboarded = true
                                            }
                                        }
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
                                                openAddExpense = pendingOpenAddExpense,
                                                onRouteHandled = { pendingRoute = null },
                                                onAddExpenseHandled = { pendingOpenAddExpense = false },
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

                        // Play Store In-App Update Ready Banner
                        if (isUpdateDownloaded && !showSplash) {
                            Surface(
                                color = CocoaEspresso,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Update ready. Restart to apply.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { inAppUpdateManager.completeUpdate() }
                                    ) {
                                        Text(
                                            text = "RESTART",
                                            fontWeight = FontWeight.Bold,
                                            color = PastelPeach
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
