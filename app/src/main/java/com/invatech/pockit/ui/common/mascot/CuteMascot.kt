package com.invatech.pockit.ui.common.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.characters.Mascot
import com.invatech.pockit.ui.characters.MascotMood
import com.invatech.pockit.ui.common.motion.kawaiiFloating
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MascotPose {
    SLEEPING,
    HAPPY_WAVING,
    THINKING,
    COZY_PEEK;

    fun toMood(): MascotMood = when (this) {
        SLEEPING -> MascotMood.Sleepy
        HAPPY_WAVING -> MascotMood.Happy
        THINKING -> MascotMood.Thinking
        COZY_PEEK -> MascotMood.Wink
    }
}

/**
 * Universal mascot component for Pockit powered by Pip from characters/Pip.kt.
 * Supports all 10 Pip moods (Happy, Excited, Love, Wink, Surprised, Thinking, Sleepy, Sad, Angry, Cool).
 */
@Composable
fun PockitMascot(
    pose: MascotPose = MascotPose.HAPPY_WAVING,
    mood: MascotMood? = null,
    size: Dp = 64.dp,
    animated: Boolean = true,
    accessory: MascotAccessory? = null,
    fur: MascotFur? = null,
    modifier: Modifier = Modifier
) {
    // Characters are disabled for the professional app experience.
}

/**
 * Interactive virtual pet version: tap to pet Pip with haptics, squish bounce,
 * heart burst, love expression, and cute motivational speech bubble.
 */
@Composable
fun InteractivePockitMascot(
    modifier: Modifier = Modifier,
    pose: MascotPose = MascotPose.HAPPY_WAVING,
    mood: MascotMood? = null,
    size: Dp = 68.dp,
    accessory: MascotAccessory? = null,
    fur: MascotFur? = null,
    onPet: () -> Unit = {}
) {
    // Characters are disabled for the professional app experience.
}
