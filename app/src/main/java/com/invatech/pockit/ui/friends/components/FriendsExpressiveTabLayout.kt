package com.invatech.pockit.ui.friends.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.motion.MotionSettings

enum class FriendsTabType(val title: String) {
    PERSONAL("Personal udhar"), CONNECTED("Connected")
}

@Composable
fun FriendsExpressiveTabLayout(
    selectedTab: FriendsTabType,
    onTabSelected: (FriendsTabType) -> Unit,
    personalCount: Int = 0,
    connectedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val motion by MotionSettings.enabled.collectAsState()
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / 2
            val indicator by animateDpAsState(
                targetValue = if (selectedTab == FriendsTabType.PERSONAL) 0.dp else tabWidth,
                animationSpec = if (motion) spring(dampingRatio = 1f, stiffness = 500f) else snap(),
                label = "friends_tab_underline"
            )
            Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.outlineVariant))
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                FriendsTabType.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val count = if (tab == FriendsTabType.PERSONAL) personalCount else connectedCount
                    Row(
                        Modifier.weight(1f).selectable(selected, role = Role.Tab,
                            onClick = { if (!selected) onTabSelected(tab) })
                            .heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tab.title, style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (count > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(if (count > 99) "99+" else count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomStart).offset(x = indicator)
                .width(tabWidth).padding(horizontal = 20.dp).height(3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
        }
        FriendsDistinctionBanner(selectedTab, Modifier.padding(top = 10.dp, bottom = 4.dp))
    }
}

@Composable
fun FriendsDistinctionBanner(selectedTab: FriendsTabType, modifier: Modifier = Modifier) {
    Text(
        if (selectedTab == FriendsTabType.PERSONAL) "Just a name. Your friend doesn't need the app."
        else "Connect by QR to track expenses together.",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
