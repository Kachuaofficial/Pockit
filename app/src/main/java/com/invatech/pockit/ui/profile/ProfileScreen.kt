package com.invatech.pockit.ui.profile

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.ui.common.legal.LegalBottomSheet
import com.invatech.pockit.ui.common.legal.LegalDocType
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.ErrorRed
import com.invatech.pockit.ui.theme.ErrorSoft
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.LavenderDeep
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "1.1"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1"
            }
        } catch (_: Exception) {
            "1.1"
        }
    }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val displayName = currentUser?.displayName ?: "Kaizen"
    val email = currentUser?.email ?: "user@pockit.app"
    val userInitial = displayName.firstOrNull()?.uppercase() ?: "P"

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLegalSheet by remember { mutableStateOf(false) }
    var legalDocType by remember { mutableStateOf(LegalDocType.PRIVACY_POLICY) }
    var biometricsEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Hero Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Avatar with Gradient Halo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Lavender)
                        .border(3.dp, Mint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Mint,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pockit Member",
                            color = Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Bento Quick Summary Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoStatCard(
                    title = "Currency",
                    value = "₹ INR",
                    icon = Icons.Rounded.CurrencyRupee,
                    backgroundColor = Cloud,
                    modifier = Modifier.weight(1f)
                )
                BentoStatCard(
                    title = "Security",
                    value = "Active",
                    icon = Icons.Rounded.Security,
                    backgroundColor = Peach,
                    modifier = Modifier.weight(1f)
                )
                BentoStatCard(
                    title = "Plan",
                    value = "Pro",
                    icon = Icons.Rounded.Verified,
                    backgroundColor = Lavender,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Preferences Section
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "App Lock & Biometrics",
                        subtitle = "Require fingerprint / face to open",
                        checked = biometricsEnabled,
                        onCheckedChange = { biometricsEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Push Notifications",
                        subtitle = "Alerts on bill splits and updates",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }
        }

        // 4. Data & Support Section
        item {
            Text(
                text = "Data & Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SettingActionRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Export Transactions",
                        subtitle = "Download CSV spreadsheet",
                        onClick = {
                            Toast.makeText(context, "Exporting statement...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingActionRow(
                        icon = Icons.Rounded.HelpOutline,
                        title = "Help & Support",
                        subtitle = "FAQs and contact support",
                        onClick = {
                            Toast.makeText(context, "Pockit Support: hello@pockit.app", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }

        // 5. Legal & Policies Section
        item {
            Text(
                text = "Legal & Policies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SettingActionRow(
                        icon = Icons.Rounded.Shield,
                        title = "Privacy Policy",
                        subtitle = "Zero credential storage & encryption",
                        onClick = {
                            legalDocType = LegalDocType.PRIVACY_POLICY
                            showLegalSheet = true
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingActionRow(
                        icon = Icons.Rounded.Description,
                        title = "Terms & Conditions",
                        subtitle = "Service guidelines & debt disclaimers",
                        onClick = {
                            legalDocType = LegalDocType.TERMS_OF_SERVICE
                            showLegalSheet = true
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingActionRow(
                        icon = Icons.Rounded.Copyright,
                        title = "Copyright & Legal Notice",
                        subtitle = "Proprietary IP, licenses & trademarks",
                        onClick = {
                            legalDocType = LegalDocType.COPYRIGHTS
                            showLegalSheet = true
                        }
                    )
                }
            }
        }

        // 6. Sign Out Button
        item {
            Spacer(modifier = Modifier.height(10.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sign_out_scale"
            )

            Surface(
                color = ErrorSoft,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { showSignOutDialog = true }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = "Sign Out",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Pockit v$versionName · Production Build",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "© 2026 Pockit Technologies Inc. · All rights reserved",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = InkMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Legal Documents Bottom Sheet
    if (showLegalSheet) {
        LegalBottomSheet(
            initialDocType = legalDocType,
            onDismiss = { showLegalSheet = false }
        )
    }

    // Sign Out Confirmation Bottom Sheet (replacing old AlertDialog)
    if (showSignOutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ErrorSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Sign out of Pockit?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You will need to sign in again to access your wallet, expenses, and spaces.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                com.invatech.pockit.ui.space.components.PockitPrimaryButton(
                    text = "Sign out",
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                )

                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Ink.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Cloud),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Mint,
                uncheckedThumbColor = InkMuted,
                uncheckedTrackColor = Cloud
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "row_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Cloud),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
