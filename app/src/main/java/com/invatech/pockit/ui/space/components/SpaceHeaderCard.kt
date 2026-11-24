package com.invatech.pockit.ui.space.components

/*
 * Space identity header with rename and invitation callbacks supplied by the detail screen.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Ink

@Composable
fun SpaceHeaderCard(
    space: Space,
    onInvite: () -> Unit,
    onRename: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(space.accent)
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(com.invatech.pockit.ui.theme.PockitElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = space.emoji, style = MaterialTheme.typography.displaySmall)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(com.invatech.pockit.ui.theme.PockitElevated)
                            .clickable(onClick = onRename),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Rename Space",
                            tint = Ink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${space.members.size} members",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Total spent",
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.65f)
        )
        Text(
            text = formatMoney(space.totalSpent),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Ink
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(
                members = space.members,
                borderColor = space.accent,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(com.invatech.pockit.ui.theme.PockitOrange)
                    .clickable(onClick = onInvite)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = com.invatech.pockit.ui.theme.PockitBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Invite",
                    color = com.invatech.pockit.ui.theme.PockitBackground,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
