package com.invatech.pockit.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.theme.*

@Composable
fun PockitBottomBar(currentRoute: String?, onNavigate: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val motion by MotionSettings.enabled.collectAsState()
    Surface(modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth(),
        color = PockitCharcoal, shape = RoundedCornerShape(36.dp)) {
        Row(Modifier.selectableGroup().padding(horizontal = 8.dp, vertical = 8.dp)) {
            bottomBarScreens.forEach { screen ->
                val selected = currentRoute == screen.route
                val tint by animateColorAsState(if (selected) PockitLavender else Color.Transparent,
                    tween(if (motion) 220 else 0), label = "tab_color")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(26.dp))
                    .selectable(selected, role = Role.Tab, onClick = { if (!selected) onNavigate(screen) })
                    .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(36.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(screen.icon, null, Modifier.size(21.dp), tint = if (selected) PockitCharcoal else Color(0xFFC5C6CA))
                    }
                    Text(screen.label, style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color(0xFFFFFBEF) else Color(0xFFC5C6CA))
                }
            }
        }
    }
}
