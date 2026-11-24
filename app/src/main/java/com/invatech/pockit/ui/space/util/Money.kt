package com.invatech.pockit.ui.space.util

/*
 * Rupee-valued display adapter for legacy shared-space models.
 * Uses the central Money helper to apply a consistent INR format at the presentation boundary.
 */

import java.text.NumberFormat
import java.util.Locale

/** 12345.5 -> "₹12,345.5" (Indian digit grouping, up to 2 decimals). */
fun formatMoney(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "₹" + format.format(amount)
}
