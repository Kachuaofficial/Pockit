package com.invatech.pockit.ui.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.loading.ShimmerBox
import com.invatech.pockit.ui.space.components.CreateSpaceSheet
import com.invatech.pockit.ui.space.components.JoinSpaceSheet
import com.invatech.pockit.ui.space.components.SpaceActionPill
import com.invatech.pockit.ui.space.components.SpaceCard
import com.invatech.pockit.ui.space.components.SpacesEmptyState
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint

@Composable
fun SpaceScreen(
    viewModel: SpaceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onOpenSpace: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spaces by viewModel.spaces.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Spaces",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Split expenses with friends, trips and roommates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpaceActionPill(
                    label = "Create",
                    icon = Icons.Rounded.Add,
                    container = Mint,
                    onClick = { showCreate = true },
                    modifier = Modifier.weight(1f)
                )
                SpaceActionPill(
                    label = "Join",
                    icon = Icons.Rounded.Link,
                    container = Lavender,
                    onClick = { showJoin = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isLoading && spaces.isEmpty()) {
            // High quality expressive shimmer skeletons during initial load
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(2) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row {
                                    ShimmerBox(
                                        modifier = Modifier.padding(end = 14.dp),
                                        shape = CircleShape
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        ShimmerBox(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .height(20.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ShimmerBox(
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(14.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Row {
                                    ShimmerBox(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(30.dp),
                                        shape = CircleShape
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    ShimmerBox(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(30.dp),
                                        shape = CircleShape
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (spaces.isEmpty()) {
            item {
                SpacesEmptyState()
            }
        } else {
            items(spaces, key = { it.id }) { space ->
                SpaceCard(
                    space = space,
                    onClick = { onOpenSpace(space.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (showCreate) {
        CreateSpaceSheet(
            onDismiss = { showCreate = false },
            onCreate = { name, emoji, themeIndex, type ->
                viewModel.createSpace(name, emoji, themeIndex, type) { id ->
                    showCreate = false
                    onOpenSpace(id)
                }
            }
        )
    }

    if (showJoin) {
        JoinSpaceSheet(
            onDismiss = { showJoin = false },
            onJoin = viewModel::joinSpace,
            onJoined = { id ->
                showJoin = false
                onOpenSpace(id)
            }
        )
    }
}
