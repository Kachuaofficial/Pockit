package com.invatech.pockit.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invatech.pockit.data.local.entity.NotificationEntity
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.common.swipe.SwipeToDeleteContainer
import com.invatech.pockit.ui.space.components.PockitTopBar
import com.invatech.pockit.ui.theme.IncomeGreen
import com.invatech.pockit.ui.theme.IncomeGreenContainer
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.LavenderContainerDark
import com.invatech.pockit.ui.theme.LavenderDark
import com.invatech.pockit.ui.theme.LavenderDeep
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.MintContainerDark
import com.invatech.pockit.ui.theme.MintDark
import com.invatech.pockit.ui.theme.MintDeep
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.PeachContainerDark
import com.invatech.pockit.ui.theme.PeachDark
import com.invatech.pockit.ui.theme.PeachDeep
import com.invatech.pockit.ui.theme.ShimmerDarkBase
import com.invatech.pockit.ui.theme.ShimmerDarkHighlight
import com.invatech.pockit.ui.theme.ShimmerLightBase
import com.invatech.pockit.ui.theme.ShimmerLightHighlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_ENTRY = "entry"
private const val CONTENT_TYPE_AD = "ad"
private const val RELATIVE_TIME_TICK_MS = 60_000L

private val CardShape = RoundedCornerShape(28.dp)

private enum class ScreenContent { LOADING, ERROR, EMPTY, CONTENT }

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * @param showDebugActions pass `BuildConfig.DEBUG` to expose the "Simulate a Notification"
 * button in the empty state. Off by default so it never ships to users.
 */
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSpace: (String) -> Unit = {},
    showDebugActions: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val now = rememberMinuteTicker()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val latestOnNavigateToSpace by rememberUpdatedState(onNavigateToSpace)

    NotificationEventEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    // A dialog restored after process death (or left open while the list empties) must not linger.
    LaunchedEffect(uiState.totalCount) {
        if (uiState.totalCount == 0) showClearDialog = false
    }

    val onNotificationClick = remember(viewModel) {
        { item: NotificationEntity ->
            if (!item.isRead) viewModel.markAsRead(item.id)
            val target = item.targetId
            if (item.isSpaceType() && !target.isNullOrBlank()) {
                latestOnNavigateToSpace(target)
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PockitTopBar(
                title = "Notifications",
                onBack = onBack,
                actions = {
                    AnimatedVisibility(
                        visible = uiState.unreadCount > 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.markAllAsRead()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = colors.primary
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = uiState.totalCount > 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear all notifications",
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                color = com.invatech.pockit.ui.theme.PockitLavender,
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your updates", style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface)
                    Text(if (uiState.unreadCount > 0) "${uiState.unreadCount} unread · Stay on top of shared expenses."
                        else "You're all caught up on unread updates.",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                }
            }

            NotificationFilterRow(
                selected = uiState.selectedFilter,
                unreadCount = uiState.unreadCount,
                onSelect = { filter ->
                    if (filter != uiState.selectedFilter) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setFilter(filter)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            val content = when {
                uiState.isLoading -> ScreenContent.LOADING
                uiState.hasError -> ScreenContent.ERROR
                uiState.visibleCount == 0 -> ScreenContent.EMPTY
                else -> ScreenContent.CONTENT
            }

            Crossfade(
                targetState = content,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(durationMillis = 220),
                label = "notifications_content"
            ) { state ->
                when (state) {
                    ScreenContent.LOADING -> NotificationsSkeleton()
                    ScreenContent.ERROR -> NotificationsErrorState(onRetry = viewModel::retry)
                    ScreenContent.EMPTY -> NotificationsEmptyState(
                        selectedFilter = uiState.selectedFilter,
                        showDebugAction = showDebugActions,
                        onSendTestNotification = viewModel::sendTestNotification
                    )
                    ScreenContent.CONTENT -> NotificationsList(
                        listItems = uiState.listItems,
                        filter = uiState.selectedFilter,
                        now = now,
                        onItemClick = onNotificationClick,
                        onItemDelete = viewModel::deleteNotification
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = colors.inverseSurface,
                contentColor = colors.inverseOnSurface,
                actionColor = colors.inversePrimary
            )
        }
    }

    if (showClearDialog && uiState.totalCount > 0) {
        ClearAllDialog(
            count = uiState.totalCount,
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.clearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun NotificationEventEffect(
    viewModel: NotificationViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            // Own coroutine per event so a long-lived snackbar never blocks the collector.
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                when (event) {
                    is NotificationEvent.ItemRemoved -> {
                        val result = snackbarHostState.showSnackbar(
                            message = "Notification removed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete(event.id)
                        }
                    }
                    NotificationEvent.AllMarkedRead ->
                        snackbarHostState.showSnackbar("All notifications marked as read")
                    NotificationEvent.AllCleared ->
                        snackbarHostState.showSnackbar("All notifications cleared")
                    is NotificationEvent.Error ->
                        snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
}

/** Emits the current time once a minute so "5m ago" labels stay fresh without a refresh. */
@Composable
private fun rememberMinuteTicker(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(RELATIVE_TIME_TICK_MS)
        value = System.currentTimeMillis()
    }
}

// ---------------------------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------------------------

@Composable
private fun NotificationsList(
    listItems: List<NotificationListItem>,
    filter: NotificationFilter,
    now: State<Long>,
    onItemClick: (NotificationEntity) -> Unit,
    onItemDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Switching tabs should always start from the newest item.
    LaunchedEffect(filter) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            items = listItems,
            key = { _, item -> item.key },
            contentType = { _, it ->
                when (it) {
                    is NotificationListItem.Header -> CONTENT_TYPE_HEADER
                    is NotificationListItem.Entry -> CONTENT_TYPE_ENTRY
                }
            }
        ) { index, listItem ->
            when (listItem) {
                is NotificationListItem.Header -> NotificationSectionHeader(
                    label = listItem.section.label,
                    modifier = Modifier.animateItem()
                )
                is NotificationListItem.Entry -> {
                    val notification = listItem.notification
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SwipeToDeleteContainer(
                            itemKey = notification.id,
                            shape = CardShape,
                            requireConfirmation = false,
                            onDelete = { onItemDelete(notification.id) },
                            modifier = Modifier.animateItem()
                        ) {
                            NotificationCardItem(
                                item = notification,
                                onClick = { onItemClick(notification) },
                                nowMillis = now.value
                            )
                        }


                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp)
            .semantics { heading() }
    )
}

// ---------------------------------------------------------------------------------------------
// Card
// ---------------------------------------------------------------------------------------------

@Composable
fun NotificationCardItem(
    item: NotificationEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis()
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val style = remember(item.type, isDark) { notificationStyleFor(item.type, isDark) }
    val unread = !item.isRead
    val isNavigable = style.kind == NotificationKind.SPACE && !item.targetId.isNullOrBlank()

    // Opaque on purpose: a translucent card would let the swipe-to-delete background bleed through.
    val readContainer = remember(colors.surfaceVariant, colors.background) {
        colors.surfaceVariant.copy(alpha = 0.5f).compositeOver(colors.background)
    }
    val containerColor by animateColorAsState(
        targetValue = if (unread) com.invatech.pockit.ui.theme.PockitTeal else colors.surface,
        animationSpec = M3Spring.fastEffects(),
        label = "card_container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (unread) colors.primary.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = M3Spring.fastEffects(),
        label = "card_border"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (unread) "Unread" else "Read" },
        shape = CardShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(style.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (unread) colors.onSurface else colors.onSurface.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(
                        visible = unread,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }

                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        // onSurface (not the pastel tint) keeps text contrast above AA on the chip.
                        color = colors.onSurface.copy(alpha = 0.78f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(style.containerColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = formatRelativeTime(item.timestamp, nowMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (isNavigable) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(20.dp)
                )
            }
        }
    }
}

private enum class NotificationKind { SPACE, SETTLE, EXPENSE, GENERAL }

@Immutable
private data class NotificationTypeStyle(
    val kind: NotificationKind,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconTint: Color
)

private fun notificationStyleFor(type: String, isDark: Boolean): NotificationTypeStyle = when {
    type.contains("space", ignoreCase = true) -> NotificationTypeStyle(
        kind = NotificationKind.SPACE,
        label = "Space",
        icon = Icons.Rounded.RocketLaunch,
        containerColor = if (isDark) LavenderContainerDark else Lavender,
        iconTint = if (isDark) LavenderDark else LavenderDeep
    )
    type.contains("settle", ignoreCase = true) -> NotificationTypeStyle(
        kind = NotificationKind.SETTLE,
        label = "Settled",
        icon = Icons.Rounded.CheckCircle,
        containerColor = if (isDark) IncomeGreen.copy(alpha = 0.2f) else IncomeGreenContainer,
        iconTint = IncomeGreen
    )
    type.contains("friend", ignoreCase = true) ||
            type.contains("udhar", ignoreCase = true) ||
            type.contains("expense", ignoreCase = true) -> NotificationTypeStyle(
        kind = NotificationKind.EXPENSE,
        label = "Expense",
        icon = Icons.Rounded.AccountBalanceWallet,
        containerColor = if (isDark) MintContainerDark else Mint,
        iconTint = if (isDark) MintDark else MintDeep
    )
    else -> NotificationTypeStyle(
        kind = NotificationKind.GENERAL,
        label = "Update",
        icon = Icons.Rounded.NotificationsActive,
        containerColor = if (isDark) PeachContainerDark else Peach,
        iconTint = if (isDark) PeachDark else PeachDeep
    )
}

// ---------------------------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------------------------

@Composable
private fun NotificationFilterRow(
    selected: NotificationFilter,
    unreadCount: Int,
    onSelect: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    // Horizontally scrollable so large font sizes / small screens never truncate a label.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationFilter.entries.forEach { filter ->
            NotificationFilterPill(
                label = filter.label,
                selected = selected == filter,
                badgeCount = if (filter == NotificationFilter.UNREAD) unreadCount else 0,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun NotificationFilterPill(
    label: String,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = M3Spring.touchFeedback(),
        label = "pill_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceVariant,
        animationSpec = M3Spring.fastEffects(),
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = M3Spring.fastEffects(),
        label = "pill_text"
    )

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .animateContentSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
            if (badgeCount > 0) {
                CountBadge(count = badgeCount, selected = selected)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(if (selected) colors.onPrimary else colors.primary)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.primary else colors.onPrimary
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Loading skeleton
// ---------------------------------------------------------------------------------------------

@Composable
private fun NotificationsSkeleton(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val base = if (isDark) ShimmerDarkBase else ShimmerLightBase
    val highlight = if (isDark) ShimmerDarkHighlight else ShimmerLightHighlight

    val transition = rememberInfiniteTransition(label = "notifications_shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .semantics { contentDescription = "Loading notifications" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(colors.surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shimmer(progress, base, highlight, CircleShape)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .shimmer(progress, base, highlight, RoundedCornerShape(7.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .shimmer(progress, base, highlight, RoundedCornerShape(6.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .shimmer(progress, base, highlight, RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}

/** Reads [progress] in the draw phase only, so the shimmer never triggers recomposition. */
private fun Modifier.shimmer(
    progress: State<Float>,
    base: Color,
    highlight: Color,
    shape: Shape
): Modifier = this
    .clip(shape)
    .drawBehind {
        val x = progress.value * 3000f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x - 1000f, 0f),
                end = Offset(x, 0f)
            )
        )
    }

// ---------------------------------------------------------------------------------------------
// Empty / error states
// ---------------------------------------------------------------------------------------------

private data class EmptyContent(
    val icon: ImageVector,
    val title: String,
    val message: String
)

private fun emptyContentFor(filter: NotificationFilter): EmptyContent = when (filter) {
    NotificationFilter.ALL -> EmptyContent(
        icon = Icons.Rounded.NotificationsNone,
        title = "All caught up! 🎉",
        message = "When your friends add an expense, settle up, or split a bill in Spaces, you'll see notifications right here."
    )
    NotificationFilter.UNREAD -> EmptyContent(
        icon = Icons.Rounded.DoneAll,
        title = "You're all read",
        message = "No unread notifications. New activity will show up here the moment it arrives."
    )
    NotificationFilter.EXPENSES -> EmptyContent(
        icon = Icons.Rounded.AccountBalanceWallet,
        title = "No expense updates",
        message = "Udhar, new expenses and settle-ups from your friends will appear here."
    )
    NotificationFilter.SPACES -> EmptyContent(
        icon = Icons.Rounded.RocketLaunch,
        title = "No Space activity",
        message = "Updates from your shared Spaces will land here."
    )
}

@Composable
private fun NotificationsEmptyState(
    selectedFilter: NotificationFilter,
    showDebugAction: Boolean,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = remember(selectedFilter) { emptyContentFor(selectedFilter) }
    NotificationStateMessage(
        icon = content.icon,
        title = content.title,
        message = content.message,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        if (showDebugAction) {
            Button(
                onClick = onSendTestNotification,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate a Notification", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NotificationsErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    NotificationStateMessage(
        icon = Icons.Rounded.ErrorOutline,
        title = "Couldn't load notifications",
        message = "Something went wrong while loading your notifications. Please try again.",
        containerColor = colors.errorContainer,
        iconTint = colors.error,
        modifier = modifier
    ) {
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Text("Try again", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NotificationStateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val iconScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "state_icon_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(com.invatech.pockit.ui.theme.PockitLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            actions()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------------------------

@Composable
private fun ClearAllDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = colors.error
                )
            }
        },
        title = {
            Text(
                text = "Clear all notifications?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = if (count == 1) {
                    "This will permanently remove your notification. This can't be undone."
                } else {
                    "This will permanently remove all $count notifications. This can't be undone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Clear All", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colors.surface
    )
}

// ---------------------------------------------------------------------------------------------
// Time formatting
// ---------------------------------------------------------------------------------------------

internal fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - timestamp
    if (diff < 0) return "Just now"

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
