package com.invatech.pockit.data.ml

/*
 * Runs ML Kit OCR on a selected receipt and heuristically suggests amount, merchant and category.
 * The entry sheet lets the user review extracted values before saving a wallet event.
 */

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.invatech.pockit.ui.home.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Result model representing data extracted from a bill/receipt using Google ML Kit OCR.
 */
data class ScannedReceiptResult(
    val merchantName: String? = null,
    val amount: Double? = null,
    val category: Category? = null,
    val rawText: String = ""
)

/**
 * High-performance on-device Machine Learning scanner using Google ML Kit Text Recognition.
 * Operates 100% locally on the device (no cloud API fees, instant response, zero privacy leakage).
 */
object ReceiptScannerManager {

    private const val TAG = "ReceiptScannerManager"

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Loads the selected image for on-device text recognition and returns suggested receipt fields
     * or a failure. OCR is heuristic; the transaction form remains responsible for user review and
     * final monetary validation.
     */
    suspend fun scanReceipt(context: Context, imageUri: Uri): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        runCatching {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val visionText = recognizer.process(inputImage).await()
            parseReceiptText(visionText)
        }.onFailure { e ->
            Log.e(TAG, "Error processing receipt image with ML Kit", e)
        }
    }

    /**
     * Parses the vision text blocks extracted by ML Kit to infer merchant name, total bill amount,
     * and category using heuristic financial pattern matching.
     */
    fun parseReceiptText(visionText: Text): ScannedReceiptResult {
        val rawText = visionText.text
        if (rawText.isBlank()) return ScannedReceiptResult()

        val lines = visionText.textBlocks
            .flatMap { it.lines }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        // 1. Extract Merchant/Shop Name (Look at the first few lines, filtering out generic tax/bill words)
        val merchantName = lines.take(4).firstOrNull { line ->
            line.length in 3..40 &&
            !line.contains("invoice", ignoreCase = true) &&
            !line.contains("receipt", ignoreCase = true) &&
            !line.contains("tax", ignoreCase = true) &&
            !line.contains("gst", ignoreCase = true) &&
            !line.contains("bill", ignoreCase = true) &&
            !line.contains("date", ignoreCase = true) &&
            !line.contains("phone", ignoreCase = true) &&
            !line.contains("tel", ignoreCase = true) &&
            line.any { it.isLetter() }
        }

        // 2. Extract Total Amount
        val amount = extractAmount(lines, rawText)

        // 3. Infer Spending Category
        val detectedCategory = detectCategory(rawText)

        return ScannedReceiptResult(
            merchantName = merchantName,
            amount = amount,
            category = detectedCategory,
            rawText = rawText
        )
    }

    /**
     * Searches receipt text for likely total lines and fallback amount candidates. A recognized
     * number is only a suggestion because receipts can also contain tax, change and subtotal
     * values.
     */
    private fun extractAmount(lines: List<String>, rawText: String): Double? {
        val totalKeywords = listOf(
            "grand total", "total amount", "net amount", "total", "amount payable",
            "balance due", "sub total", "subtotal", "amt", "bill amount", "total inr", "net payable"
        )

        // Priority 1: Check lines with explicit 'Total' labels from bottom up
        for (i in lines.indices.reversed()) {
            val lineLower = lines[i].lowercase(Locale.ROOT)
            if (totalKeywords.any { lineLower.contains(it) }) {
                val match = findAmountInString(lines[i])
                    ?: if (i + 1 < lines.size) findAmountInString(lines[i + 1]) else null
                if (match != null && match > 0.0) {
                    return match
                }
            }
        }

        // Priority 2: Look for currency symbols (₹, Rs, INR)
        val currencyRegex = Regex("""(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val matches = currencyRegex.findAll(rawText).mapNotNull {
            it.groupValues[1].replace(",", "").toDoubleOrNull()
        }.toList()

        if (matches.isNotEmpty()) {
            return matches.maxOrNull()
        }

        // Priority 3: Fallback to largest standard decimal price formatted candidate
        val generalPriceRegex = Regex("""\b\d{1,6}\.\d{2}\b""")
        val priceCandidates = generalPriceRegex.findAll(rawText).mapNotNull {
            it.value.toDoubleOrNull()
        }.filter { it in 1.0..500000.0 }.toList()

        return priceCandidates.maxOrNull()
    }

    private fun findAmountInString(text: String): Double? {
        val regex = Regex("""([\d,]+(?:\.\d{1,2})?)""")
        val matches = regex.findAll(text)
        return matches.mapNotNull {
            it.groupValues[1].replace(",", "").toDoubleOrNull()
        }.lastOrNull()
    }

    private fun detectCategory(rawText: String): Category {
        val lower = rawText.lowercase(Locale.ROOT)
        return when {
            listOf("restaurant", "cafe", "dhaba", "kitchen", "bakery", "food", "burger", "pizza", "coffee", "bistro", "dining", "swiggy", "zomato", "biryani", "bar", "sweets", "tandoor", "meal").any { lower.contains(it) } -> Category.Food
            listOf("fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "uber", "ola", "rapido", "railway", "irctc", "indigo", "air", "cab", "metro", "toll", "fastag").any { lower.contains(it) } -> Category.Travel
            listOf("supermarket", "mart", "store", "retail", "fashion", "mall", "clothing", "bazaar", "amazon", "flipkart", "myntra", "zudio", "reliance", "dmart").any { lower.contains(it) } -> Category.Shopping
            listOf("electricity", "water", "gas", "broadband", "wifi", "telecom", "airtel", "jio", "vodafone", "vi", "recharge", "utility", "bill").any { lower.contains(it) } -> Category.Bills
            listOf("cinema", "movie", "pvr", "inox", "cinepolis", "theatre", "game", "gaming", "amusement").any { lower.contains(it) } -> Category.Entertainment
            else -> Category.Other
        }
    }
}
