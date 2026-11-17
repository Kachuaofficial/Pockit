package com.invatech.pockit.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.SpaceAccents
import com.invatech.pockit.ui.theme.Ink

@Composable
fun MemberAvatar(
    member: Member,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderColor: Color = Color.Transparent
) {
    val color = SpaceAccents[member.id.hashCode().mod(SpaceAccents.size)]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        com.invatech.pockit.ui.common.UserAvatar(member.id, member.name,
            Modifier.size(size).padding(2.dp))
    }
}

/** Overlapping avatars, e.g. for a space card. */
@Composable
fun AvatarStack(
    members: List<Member>,
    modifier: Modifier = Modifier,
    max: Int = 4,
    size: Dp = 34.dp,
    borderColor: Color = Color.White
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        members.take(max).forEach { member ->
            MemberAvatar(member = member, size = size, borderColor = borderColor)
        }
        if (members.size > max) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${members.size - max}",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
