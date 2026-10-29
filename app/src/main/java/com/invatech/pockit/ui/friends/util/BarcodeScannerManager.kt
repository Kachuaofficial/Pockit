package com.invatech.pockit.ui.friends.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class BarcodeScannerManager(private val context: Context) {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_UPC_A
        )
        .enableAutoZoom()
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    fun startScan(
        onSuccess: (rawCode: String) -> Unit,
        onCanceled: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                if (raw.isNotBlank()) {
                    onSuccess(raw)
                } else {
                    onCanceled()
                }
            }
            .addOnCanceledListener {
                onCanceled()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}
