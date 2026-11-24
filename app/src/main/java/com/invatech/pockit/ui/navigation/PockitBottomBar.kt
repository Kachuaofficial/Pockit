package com.invatech.pockit.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.theme.*

@Composable
fun PockitBottomBar(currentRoute: String?, onNavigate: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val motion by MotionSettings.enabled.collectAsState()
    Surface(modifier.navigationBarsPadding().padding(horizontal=16.dp,vertical=8.dp).fillMaxWidth(),
        color=PockitSurface,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,PockitBorder)) {
        Row(Modifier.selectableGroup().padding(5.dp)) {
            bottomBarScreens.forEach { screen ->
                val selected=currentRoute==screen.route
                val tint by animateColorAsState(if(selected) PockitOrange else PockitSecondaryText,
                    tween(if(motion) 200 else 0),label="navigation_tint")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                    .background(if(selected) Brush.radialGradient(listOf(PockitOrange.copy(alpha=.19f),Color.Transparent),center=Offset(70f,70f),radius=150f)
                        else Brush.linearGradient(listOf(Color.Transparent,Color.Transparent)))
                    .selectable(selected,role=Role.Tab,onClick={if(!selected) onNavigate(screen)})
                    .padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Icon(screen.icon,null,Modifier.size(24.dp),tint=tint)
                    Text(screen.label,fontSize=10.sp,lineHeight=14.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal,
                        color=tint,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
