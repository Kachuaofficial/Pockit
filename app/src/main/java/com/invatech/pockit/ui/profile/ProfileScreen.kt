package com.invatech.pockit.ui.profile

import android.app.Activity
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.runtime.collectAsState
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
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.FragmentActivity
import com.invatech.pockit.data.biometric.BiometricAuthManager
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
import com.invatech.pockit.ui.theme.PalettePicker
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.PockitPalette
import com.invatech.pockit.ui.theme.ThemeManager
import com.invatech.pockit.ui.theme.ThemeMode

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
                ).versionName ?: "1.6"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.6"
            }
        } catch (_: Exception) {
            "1.6"
        }
    }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val displayName = currentUser?.displayName?.ifBlank { null } ?: "User"
    val email = currentUser?.email ?: ""
    val userInitial = displayName.firstOrNull()?.uppercase() ?: "P"

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLegalSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var legalDocType by remember { mutableStateOf(LegalDocType.PRIVACY_POLICY) }
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val currentPalette by ThemeManager.palette.collectAsState()
    val biometricManager = remember { BiometricAuthManager(context) }
    var biometricsEnabled by remember { mutableStateOf(biometricManager.isBiometricEnabled) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Hero Profile Header with Initial Avatar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Initial-based Profile Circle Avatar matching current theme
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted
                )
            }
        }

        // 2. Preferences Section
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
                        subtitle = if (biometricsEnabled) "Fingerprint / face lock active" else "Require fingerprint / face to open",
                        checked = biometricsEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (!biometricManager.canAuthenticate()) {
                                    Toast.makeText(
                                        context,
                                        "No biometrics or screen lock enrolled on this device",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        biometricManager.promptBiometric(
                                            activity = activity,
                                            title = "Enable App Lock",
                                            subtitle = "Verify fingerprint or face to enable",
                                            onSuccess = {
                                                biometricManager.isBiometricEnabled = true
                                                biometricsEnabled = true
                                                Toast.makeText(context, "App lock enabled!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { _, err ->
                                                Toast.makeText(context, "Failed: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        biometricManager.isBiometricEnabled = true
                                        biometricsEnabled = true
                                    }
                                }
                            } else {
                                biometricManager.isBiometricEnabled = false
                                biometricsEnabled = false
                                Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SettingActionRow(
                        icon = Icons.Rounded.Settings,
                        title = "Appearance & Theme",
                        subtitle = "${currentPalette.title} · ${currentThemeMode.title}",
                        trailingText = currentPalette.title,
                        onClick = { showThemeSheet = true }
                    )
                }
            }
        }

        // 2b. Color Palettes Quick Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${currentPalette.title} active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            PalettePicker(
                selected = currentPalette,
                onSelect = { ThemeManager.setPalette(it) },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            )
        }

        // 3. Support & Feedback Section
        item {
            Text(
                text = "Support & Feedback",
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
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Help & Support",
                        subtitle = "Contact the Pockit team directly",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:invatechstudioz@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Pockit Support & Feedback")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Contact: invatechstudioz@gmail.com", Toast.LENGTH_LONG).show()
                            }
                        }
                    )


                }
            }
        }

        // 4. Legal & Policies Section
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

        // 5. Sign Out Button
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
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
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

            Spacer(modifier = Modifier.height(16.dp))

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
                    text = "© 2026 InvatechStudioz. · All rights reserved",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Theme Selection Bottom Sheet
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the color palette and brightness of Pockit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Section 1: Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "COLOR PALETTE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    PalettePicker(
                        selected = currentPalette,
                        onSelect = { ThemeManager.setPalette(it) },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
                    )
                }

                // Section 2: Mode (System, Light, Dark)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "THEME MODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        Surface(
                            onClick = {
                                ThemeManager.setThemeMode(mode)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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

    // Sign Out Confirmation Bottom Sheet
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
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = InkMuted,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = com.invatech.pockit.ui.common.motion.M3Spring.touchFeedback(),
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (trailingText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
