package com.invatech.pockit.ui.space.components

/*
 * Collects a join code and displays the repository join result with local loading/error state.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.JoinResult
import com.invatech.pockit.ui.theme.ErrorRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSpaceSheet(
    onDismiss: () -> Unit,
    onJoin: suspend (String) -> JoinResult,
    onJoined: (spaceId: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Join with code",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Ask your friend for the 6-letter invite code of their space.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SoftTextField(
                value = code,
                onValueChange = { input ->
                    code = input.uppercase().filter { it.isLetterOrDigit() }.take(8)
                    error = null
                },
                placeholder = "Invite code (e.g. GOA202)",
                capitalization = KeyboardCapitalization.Characters
            )

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically()
            ) {
                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed
                    )
                }
            }

            PockitPrimaryButton(
                text = if (isJoining) "Connecting to space..." else "Join space",
                enabled = code.length >= 4 && !isJoining,
                onClick = {
                    scope.launch {
                        isJoining = true
                        error = null
                        when (val result = onJoin(code)) {
                            is JoinResult.Success -> {
                                isJoining = false
                                onJoined(result.spaceId)
                            }
                            JoinResult.NotFound -> {
                                isJoining = false
                                error = "No space found with code '$code'."
                            }
                            JoinResult.AlreadyMember -> {
                                isJoining = false
                                error = "You're already a member of this space!"
                            }
                        }
                    }
                }
            )
        }
    }
}
