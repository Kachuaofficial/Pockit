package com.invatech.pockit.ui.home.components

/* Shared background still used by Spaces and connected-friend screens. */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink

/** Draws the shared light pastel atmosphere used by Spaces and Friends. */
@Composable
fun RecordDoodleBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = PastelPink.copy(alpha = 0.42f),
                radius = size.width * 0.46f,
                center = Offset(size.width, size.height * 0.02f)
            )
            drawCircle(
                color = PastelPeach.copy(alpha = 0.34f),
                radius = size.width * 0.36f,
                center = Offset(-size.width * 0.02f, size.height * 0.22f)
            )
        }
        content()
    }
}
