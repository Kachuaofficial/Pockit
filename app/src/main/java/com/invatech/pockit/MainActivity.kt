package com.invatech.pockit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.ui.auth.AuthViewModel
import com.invatech.pockit.ui.auth.LoginScreen
import com.invatech.pockit.ui.navigation.PockitApp
import com.invatech.pockit.ui.theme.PockitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsState()

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
                        targetState = uiState.isSignedIn,
                        animationSpec = tween(durationMillis = 350),
                        label = "auth_screen_crossfade"
                    ) { isSignedIn ->
                        if (isSignedIn) {
                            PockitApp(
                                onSignOut = { authViewModel.signOut() }
                            )
                        } else {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    // State update will trigger crossfade
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
