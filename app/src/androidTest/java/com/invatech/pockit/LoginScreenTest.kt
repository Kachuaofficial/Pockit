package com.invatech.pockit.ui.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.invatech.pockit.ui.theme.PockitTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun signInAndLegalActionsAreAccessible() {
        var signIn = 0
        var terms = false
        var privacy = false
        compose.setContent { PockitTheme { LoginContent(false, null, { signIn++ }, { terms = true }, { privacy = true }) } }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        compose.onNodeWithText("Money sorted.\nLife, a little lighter.").assertIsDisplayed()
        compose.onNodeWithText("Continue with Google").assertIsDisplayed().performClick()
        compose.onNodeWithText("Terms").performClick()
        compose.onNodeWithText("Privacy Policy").performClick()
        compose.runOnIdle { assertEquals(1, signIn); assertTrue(terms); assertTrue(privacy) }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "auth-screen.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun signingInDisablesRepeatedRequests() {
        compose.setContent { PockitTheme { LoginContent(true, null, { fail("Must not sign in twice") }, {}, {}) } }
        compose.onNodeWithText("Signing in…").assertIsDisplayed().assertIsNotEnabled()
    }
}
