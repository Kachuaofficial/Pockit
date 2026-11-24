package com.invatech.pockit.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.invatech.pockit.data.offline.OfflineSyncState
import com.invatech.pockit.ui.home.components.PockitMark

@Composable
fun OfflineStatusCard(online: Boolean, sync: OfflineSyncState, onSync: () -> Unit) {
    if (online && sync.count == 0) return
    Surface(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("offline_status_card"), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (online) Icons.Rounded.CloudUpload else Icons.Rounded.WifiOff, null)
                Column(Modifier.weight(1f)) {
                    Text(if (online) "${sync.count} entries waiting to sync" else "You're offline",
                        style = MaterialTheme.typography.titleSmall)
                    Text(if (!online) "Saved records, expense/income drafts and planner work offline."
                        else if (sync.busy) "Saving your entries securely…"
                        else "Watch a video to save these entries to your account. Pro syncs without ads.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!online && sync.count > 0) Text("${sync.count} entries saved on this phone only. Sync when you're online.",
                style = MaterialTheme.typography.bodySmall)
            if (online && sync.count > 0) TextButton(onClick = onSync, enabled = !sync.busy,
                modifier = Modifier.align(Alignment.End)) { Text(if (sync.busy) "Syncing…" else "Open sync options") }
            if (sync.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun OfflineFeatureScreen(onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        PockitMark(Modifier.size(76.dp))
        Spacer(Modifier.height(20.dp))
        Icon(Icons.Rounded.WifiOff, null, Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("This feature needs internet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text("You can still view saved activity, record income or expenses, read cached reports and use the planner. Friends, shared spaces and ad-supported feature access require a connection.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onHome) { Text("Go to offline tools") }
    }
}
