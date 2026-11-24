package com.invatech.pockit.ui.common.legal

/*
 * In-app privacy, terms and copyright text shown in a selectable bottom sheet.
 * This is explanatory content; actual permissions are enforced by Android and server configuration.
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach

enum class LegalDocType(
    val title: String,
    val icon: ImageVector
) {
    PRIVACY_POLICY("Privacy Policy", Icons.Rounded.Shield),
    TERMS_OF_SERVICE("Terms & Conditions", Icons.Rounded.Description),
    COPYRIGHTS("Copyright & Legal", Icons.Rounded.Copyright)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalBottomSheet(
    initialDocType: LegalDocType = LegalDocType.PRIVACY_POLICY,
    onDismiss: () -> Unit
) {
    var selectedDoc by remember { mutableStateOf(initialDocType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                when (selectedDoc) {
                                    LegalDocType.PRIVACY_POLICY -> Mint
                                    LegalDocType.TERMS_OF_SERVICE -> Lavender
                                    LegalDocType.COPYRIGHTS -> Peach
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedDoc.icon,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = selectedDoc.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Last updated: October 2026",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkMuted
                        )
                    }
                }
            }

            // Interactive Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegalDocType.entries.forEach { doc ->
                    val isSelected = doc == selectedDoc
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_scale_${doc.name}"
                    )

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        label = "tab_bg_${doc.name}"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tab_text_${doc.name}"
                    )

                    Surface(
                        color = bgColor,
                        shape = CircleShape,
                        modifier = Modifier
                            .scale(scale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { selectedDoc = doc }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = doc.icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Document Content Container with Scroll
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedDoc,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "legal_doc_content"
                    ) { doc ->
                        when (doc) {
                            LegalDocType.PRIVACY_POLICY -> PrivacyPolicyContent()
                            LegalDocType.TERMS_OF_SERVICE -> TermsOfServiceContent()
                            LegalDocType.COPYRIGHTS -> CopyrightContent()
                        }
                    }
                }
            }

            // Close / Action Button
            PockitPrimaryButton(
                text = "I understand and agree",
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // High-security highlight card
        Surface(
            color = Mint.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Zero Financial Access: Pockit never collects, stores, or asks for your bank passwords, UPI PINs, or debit/credit card CVVs.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
            }
        }

        LegalSection(
            number = "1",
            title = "Information We Collect",
            body = "When you register or use Pockit, we receive profile information from Google Sign-In (name, email address, profile photo, and unique identifier). We also store the wallet entries, expense titles, amounts, categories, and group memberships that you enter or explicitly approve. Optional UPI notification access prepares on-device payment drafts for your review; raw notification text is not retained. Approved wallet entries sync to Firebase. Personal udhar records, savings goals, pending payment reviews, planner details, notifications and preferences sync privately to your Firebase account. Name-only udhar records are not shared with the named friend. Local storage retains cached records for offline access."
        )

        LegalSection(
            number = "2",
            title = "How We Use Your Data",
            body = "Your information is used solely to: (a) calculate your running balances and spending charts; (b) compute shared expense shares and minimal debt settlements in your Spaces; and (c) synchronize your records in real time across your devices via secure cloud databases."
        )

        LegalSection(
            number = "3",
            title = "Data Storage & Encryption",
            body = "All transmissions between Pockit and our servers are encrypted using modern Transport Layer Security (TLS 1.3). Data is securely persisted on Google Cloud Platform / Firebase Firestore with strict access control rules ensuring only you and your authorized Space members can access shared records."
        )

        LegalSection(
            number = "4",
            title = "Third-Party Services & Advertising",
            body = "Pockit uses Google Identity for sign-in, Firebase for account sync, and Google AdMob for banner, native and optional rewarded ads. Reward videos are shown only after you choose a clearly labelled offer. Completing one stores a 24-hour feature unlock locally on your device. AdMob and its advertising partners may process device identifiers and ad interaction data. Consent choices can be managed through the app's ad privacy controls. Firebase App Check and Play Integrity help verify authentic app requests. Pockit does not sell subscriptions or virtual coins."
        )

        LegalSection(
            number = "5",
            title = "Your Rights & Account Deletion",
            body = "You have full control over your data. You may sign out at any time or request complete deletion of your account, wallet history, and group memberships by contacting our privacy team at privacy@pockit.app."
        )
    }
}

@Composable
private fun TermsOfServiceContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Platform nature disclaimer
        Surface(
            color = Lavender.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Gavel,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Utility Platform: Pockit is an expense tracking and arithmetic calculation tool. We are not a bank, money transmitter, or licensed payment gateway.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
            }
        }

        LegalSection(
            number = "1",
            title = "Agreement to Terms",
            body = "By accessing, downloading, or signing in to Pockit, you agree to be bound by these Terms and Conditions and our Privacy Policy. If you do not agree, please do not use the application."
        )

        LegalSection(
            number = "2",
            title = "User Accounts & Responsibility",
            body = "You are responsible for safeguarding your Google account credentials used to access Pockit. Any activity originating from your authenticated account will be deemed your responsibility."
        )

        LegalSection(
            number = "3",
            title = "Shared Spaces & Group Settlements",
            body = "Pockit calculates mathematical debt balances and suggests simplified settlement routes between group members. Pockit does not execute bank transfers. All actual currency transfers occur externally via your preferred UPI, bank, or cash payment methods."
        )

        LegalSection(
            number = "4",
            title = "Acceptable Use",
            body = "You agree not to use Pockit for fraudulent transactions, unlawful money tracking, harassment, or attempting to reverse-engineer our proprietary synchronization and settlement algorithms."
        )

        LegalSection(
            number = "5",
            title = "Limitation of Liability",
            body = "Pockit is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind. Under no circumstances shall Pockit Technologies Inc. be liable for any indirect, incidental, or financial dispute damages arising between group members."
        )

        LegalSection(
            number = "6",
            title = "Governing Law",
            body = "These Terms are governed by and construed in accordance with the laws of India. Any legal dispute shall be subject to the exclusive jurisdiction of the competent courts in India."
        )
        LegalSection(number = "7", title = "Advertising and rewarded access",
            body = "Pockit is supported by advertising and does not sell subscriptions or virtual coins. Banner and native ads may appear during everyday use. Reward videos are optional and appear only after you select an offer. Completing a rewarded video unlocks the named feature on that device for 24 hours; closing it early does not unlock access. Watching again extends access by another 24 hours. Core personal expense tracking remains available without completing a rewarded video.")
    }
}

@Composable
private fun CopyrightContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Copyright hero card
        Surface(
            color = Peach.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Â© 2026 Pockit Technologies Inc.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All Rights Reserved Worldwide.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.copy(alpha = 0.75f)
                )
            }
        }

        LegalSection(
            number = "1",
            title = "Proprietary Intellectual Property",
            body = "The Pockit application, including but not limited to its distinctive brand identity, visual layout, color schemes (Mint, Lavender, Peach, Ink), custom UI components, vector icons, source code, and algorithms (such as the greedy debt settlement engine and expressive animations), is the exclusive intellectual property of Pockit Technologies Inc."
        )

        LegalSection(
            number = "2",
            title = "Trademarks",
            body = "The 'Pockit' wordmark, stylized 'P' emblem, and associated logos are registered trademarks of Pockit Technologies Inc. Unauthorized reproduction, modification, distribution, or commercial exploitation is strictly prohibited without explicit written consent."
        )

        LegalSection(
            number = "3",
            title = "Open Source Acknowledgments",
            body = "Pockit is built using Android Open Source Project (AOSP), Kotlin, Jetpack Compose (Apache 2.0 License), and Google Firebase SDKs. We gratefully acknowledge the open-source community for providing foundational building blocks."
        )

        LegalSection(
            number = "4",
            title = "Contact Legal & Inquiries",
            body = "For copyright clearance, partnership inquiries, or grievance redressal, please write to: legal@pockit.app / contact@pockit.app."
        )
    }
}

@Composable
private fun LegalSection(
    number: String,
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$number. $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
