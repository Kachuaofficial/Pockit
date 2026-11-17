package com.invatech.pockit.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.ui.theme.PockitLavender

@Composable
fun UserAvatar(userId: String?, name: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val motion by com.invatech.pockit.ui.common.motion.MotionSettings.enabled.collectAsState()
    var photo by remember(userId) { mutableStateOf<String?>(null) }
    var failed by remember(photo) { mutableStateOf(false) }
    DisposableEffect(userId) {
        val listener = userId?.takeIf { it.isNotBlank() }?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                photo = snapshot?.getString("photoUrl")?.takeIf { it.startsWith("https://") }
                    ?: FirebaseAuth.getInstance().currentUser?.takeIf { it.uid == uid }?.photoUrl?.toString()
            }
        }
        onDispose { listener?.remove() }
    }
    Box(modifier.clip(CircleShape).background(PockitLavender), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        if (!photo.isNullOrBlank() && !failed) AsyncImage(model = remember(photo, motion) { ImageRequest.Builder(context).data(photo).crossfade(if (motion) 180 else 0).build() }, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), onError = { failed = true })
    }
}
