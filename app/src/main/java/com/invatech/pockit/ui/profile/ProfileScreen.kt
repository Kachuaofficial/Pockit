package com.invatech.pockit.ui.profile

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.data.biometric.BiometricAuthManager
import com.invatech.pockit.data.notification.*
import com.invatech.pockit.ui.common.motion.MotionSettings
import com.invatech.pockit.ui.finance.*
import com.invatech.pockit.ui.notification.UpiPermissionSheet
import com.invatech.pockit.ui.theme.*

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val motion by MotionSettings.enabled.collectAsState()
    val tracking by UpiNotificationManager.isAutoTrackingEnabledFlow.collectAsState()
    val biometric = remember { BiometricAuthManager(context) }
    var locked by remember { mutableStateOf(biometric.isBiometricEnabled) }
    var lockAvailable by remember { mutableStateOf(biometric.canAuthenticate()) }
    var access by remember { mutableStateOf(UpiNotificationManager.isNotificationAccessGranted(context)) }
    var consentRequired by remember { mutableStateOf(UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) }
    var permission by remember { mutableStateOf(false) }
    var signOut by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf(false) }
    var reminders by remember { mutableStateOf(PockitEngagementNotificationManager.isSmartRemindersEnabled(context)) }
    var frequency by remember { mutableIntStateOf(PockitEngagementNotificationManager.frequency(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access = UpiNotificationManager.isNotificationAccessGranted(context)
                lockAvailable = biometric.canAuthenticate()
                locked = biometric.isBiometricEnabled
                consentRequired = UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val user = FirebaseAuth.getInstance().currentUser
    var name by remember(user?.uid) { mutableStateOf(user?.displayName?.takeIf { it.isNotBlank() } ?: "Your account") }
    DisposableEffect(user?.uid) {
        val listener = user?.uid?.let { uid ->
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener { snapshot, _ -> snapshot?.getString("name")?.takeIf { it.isNotBlank() }?.let { name = it } }
        }
        onDispose { listener?.remove() }
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("Profile", "Your account, your preferences.") }
        item {
            val syncError by com.invatech.pockit.data.repository.AccountPreferences.error.collectAsState()
            ErrorNotice(syncError)
        }
        item { FinanceCard(color = PockitLavender) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                com.invatech.pockit.ui.common.UserAvatar(user?.uid, name, Modifier.size(52.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    user?.email?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        } }
        item { ProfileSection("Money tools") {
            ProfileLink("Budgets", "Set monthly and category limits", onNavigateToBudgets)
        } }
        item { ProfileSection("Payment notifications") {
            ProfileToggle("Review UPI payments", "Detected payments need your approval before they enter your wallet.", tracking) {
                if (!it) UpiNotificationManager.setAutoTrackingEnabled(context, false)
                else if (access) UpiNotificationManager.setAutoTrackingEnabled(context, true)
                else permission = true
            }
            if (tracking && !access) {
                Text("Notification access is off. Payment detection is paused.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { UpiNotificationManager.openNotificationListenerSettings(context) }) { Text("Allow notification access") }
            }
        } }
        item { ProfileSection("Preferences") {
            ProfileToggle("Animations", "Turn off visual motion for a quieter experience and less device work.", motion) { MotionSettings.setEnabled(context, it) }
            HorizontalDivider()
            ProfileToggle("Useful reminders", "Quiet hours: 10 pm–8:30 am.", reminders) {
                reminders = it; PockitEngagementNotificationManager.setSmartRemindersEnabled(context, it)
            }
            if (reminders) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Standard", "Frequent").forEachIndexed { index, label ->
                        FilterChip(frequency == index + 1, { frequency = index + 1; PockitEngagementNotificationManager.setFrequency(context, frequency) }, label = { Text(label) })
                    }
                }
            }
            if (context is FragmentActivity && (lockAvailable || locked)) {
                HorizontalDivider()
                ProfileToggle("App lock", "Use your device authentication to open Pockit.", locked) { enable ->
                    if (!enable) { biometric.isBiometricEnabled = false; locked = false }
                    else biometric.promptBiometric(context, onSuccess = { biometric.isBiometricEnabled = true; locked = true },
                        onError = { _, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() })
                }
            }
        } }
        item { ProfileSection("About Pockit") {
            ProfileLink("Privacy & terms", "Read how your information is handled", { legal = true })
            if (context is Activity && consentRequired) {
                HorizontalDivider()
                ProfileLink("Ad privacy choices", "Manage your advertising consent", {
                    com.invatech.pockit.data.ads.AdManager.beginPrivacyChoices()
                    UserMessagingPlatform.showPrivacyOptionsForm(context) { error ->
                        com.invatech.pockit.data.ads.AdManager.finishPrivacyChoices(context)
                        error?.let { Toast.makeText(context, "Couldn't open privacy choices. Try again later.", Toast.LENGTH_SHORT).show() }
                    }
                })
            }
            Text("Pockit · ${com.invatech.pockit.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }

        item {

        }

        if (user != null) item {
            OutlinedButton(onClick = { signOut = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Sign out") }
        }
    }
    if (permission) UpiPermissionSheet(onDismiss = { permission = false }, onEnable = {
        permission = false
        UpiNotificationManager.setAutoTrackingEnabled(context, true)
        UpiNotificationManager.openNotificationListenerSettings(context)
    })
    if (legal) com.invatech.pockit.ui.common.legal.LegalBottomSheet(onDismiss = { legal = false })
    if (signOut) AlertDialog(onDismissRequest = { signOut = false }, title = { Text("Sign out?") },
        text = { Text("Your records stay linked to your account. Make sure pending changes have synced before signing out.") },
        confirmButton = { TextButton(onClick = { signOut = false; onSignOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { signOut = false }) { Text("Cancel") } })
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
        FinanceCard { content() }
    }
}

@Composable
private fun ProfileToggle(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun ProfileLink(title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp))
    }
}
