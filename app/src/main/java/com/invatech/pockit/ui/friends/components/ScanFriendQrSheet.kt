package com.invatech.pockit.ui.friends.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.friends.util.BarcodeScannerManager
import com.invatech.pockit.ui.space.components.PockitPrimaryButton
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanFriendQrSheet(
    onDismiss: () -> Unit,
    onCodeScanned: (rawCode: String) -> Unit,
    onManualConnect: (codeOrEmail: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scannerManager = remember { BarcodeScannerManager(context) }

    var manualCodeInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    text = "Connect with Friend",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Scan your friend's barcode to connect instantly and start recording shared expenses.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = InkMuted
                )
            }

            // Camera Scan Button (Primary action)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        scannerManager.startScan(
                            onSuccess = { rawCode ->
                                onCodeScanned(rawCode)
                                onDismiss()
                            },
                            onFailure = { error ->
                                Toast.makeText(
                                    context,
                                    "Scanner: ${error.localizedMessage ?: "Camera not available"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                shape = RoundedCornerShape(22.dp),
                color = Ink
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Mint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open Camera Scanner",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Point camera at friend's Pockit QR",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.CenterFocusStrong,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Divider / Or separator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CloudDeep)
                )
                Text(
                    text = "OR ENTER MANUALLY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CloudDeep)
                )
            }

            // Manual Code Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Friend's Code or Email",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkMuted
                )

                TextField(
                    value = manualCodeInput,
                    onValueChange = { manualCodeInput = it },
                    placeholder = { Text("Paste friend code or email") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CloudDeep.copy(alpha = 0.6f),
                        unfocusedContainerColor = CloudDeep.copy(alpha = 0.6f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                PockitPrimaryButton(
                    text = "Connect Friend",
                    enabled = manualCodeInput.isNotBlank(),
                    onClick = {
                        onManualConnect(manualCodeInput)
                        onDismiss()
                    }
                )
            }
        }
    }
}
