package com.invatech.pockit.data.notification

import com.invatech.pockit.ui.home.model.WalletTransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedUpiTransaction(
    val amountPaise: Long,
    val merchantOrSender: String,
    val type: WalletTransactionType, // INCOME or EXPENSE
    val category: String,
    val appName: String,
    val rawText: String
)

/**
 * High-performance, on-device parser for Indian UPI & banking payment notifications.
 * Automatically classifies outgoing EXPENSE payments vs incoming INCOME payments.
 * 100% private & on-device: Rejects OTPs, personal chat messages, loans, requests, and non-genuine alerts.
 */
object UpiNotificationParser {

    // Common UPI payment apps in India
    val SUPPORTED_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "com.dreamplug.androidapp",               // CRED
        "in.org.npci.upiapp",                    // BHIM
        "com.naviapp",                           // Navi

    )

    private val APP_NAMES = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "com.dreamplug.androidapp" to "CRED",
        "in.org.npci.upiapp" to "BHIM",
        "com.naviapp" to "Navi",
        "in.amazon.mShop.android.shopping" to "Amazon Pay"
    )

    // Strict spam/noise keywords that MUST BE REJECTED immediately
    private val REJECT_KEYWORDS = listOf(
        "otp", "verification code", "code is", "secret code",
        "bill due", "due in", "payment due", "upcoming bill", "bill generated",
        "failed", "declined", "cancelled", "unsuccessful", "timed out",
        "requested", "has requested", "request to pay", "request from",
        "loan up to", "pre-approved", "apply for", "credit limit",
        "recharge reminder", "statement", "will expire", "pending", "processing",
        "cashback", "reward", "offer", "earn", "win ", "refund", "reversed", "reversal", "collect request"
    )

    // Outgoing Debit keywords
    private val DEBIT_KEYWORDS = listOf(
        "paid to", "paid", "debited from", "debited", "sent to", "sent",
        "payment of", "payment to", "spent", "transferred to"
    )

    // Incoming Credit keywords (Genuine Money Received)
    private val CREDIT_KEYWORDS = listOf(
        "received from", "received", "credited to", "credited with", "credited by",
        "credited", "sent you", "money received", "deposited"
    )

    // Currency Amount Regex
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:₹|rs\.?|inr)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val VERB_AMOUNT_PATTERN = Pattern.compile(
        """(?:paid|debited|sent|spent|received|credited)\s+(?:₹|rs\.?|inr)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Payee (Expense): "to Starbucks", "at FreshMenu"
    private val PAYEE_TO_PATTERN = Pattern.compile(
        """(?:to|at)\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s+(?:is|was|successful|via|using|from|on|ref|upi|\.|\,)|$)""",
        Pattern.CASE_INSENSITIVE
    )

    // Payer (Income): "from Ramesh", "by Priya"
    private val PAYER_FROM_PATTERN = Pattern.compile(
        """(?:from|by)\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s+(?:is|was|to|via|using|in|into|ref|upi|\.|\,)|$)""",
        Pattern.CASE_INSENSITIVE
    )

    fun isSupportedPackage(packageName: String): Boolean = SUPPORTED_PACKAGES.contains(packageName)

    /**
     * Parses notification text and classifies it as EXPENSE, INCOME, or null (if non-genuine or noise).
     */
    fun parse(packageName: String, title: String?, text: String?, bigText: String?): ParsedUpiTransaction? {
        if (!isSupportedPackage(packageName)) return null
        val combined = listOfNotNull(title, text, bigText).distinct().joinToString(" ").trim()
        if (combined.isBlank()) return null

        val lowerCombined = combined.lowercase(Locale.ROOT)

        // 1. Strict rejection of OTPs, requests, loan marketing, bills due, or failures
        for (rejectWord in REJECT_KEYWORDS) {
            if (lowerCombined.contains(rejectWord)) {
                return null
            }
        }

        // 2. Classify transaction type (INCOME vs EXPENSE)
        val isCredit = CREDIT_KEYWORDS.any { Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(lowerCombined) }
        val debitText = lowerCombined.replace("sent you", "")
        val isDebit = DEBIT_KEYWORDS.any { Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(debitText) }

        if (isCredit == isDebit) {
            return null
        }

        if (!Regex("(?i)\\b(paid|debited|sent|spent|received|credited|deposited|transferred|successful)\\b").containsMatchIn(combined)) return null

        // 3. Extract transaction amount
        val amountPaise = extractAmountPaise(combined) ?: return null
        if (amountPaise <= 0L) return null

        val appName = APP_NAMES[packageName] ?: "UPI"

        return if (isCredit && !isDebit) {
            // Genuine Incoming Money (INCOME)
            val sender = extractSender(title, combined, appName)
            ParsedUpiTransaction(
                amountPaise = amountPaise,
                merchantOrSender = sender,
                type = WalletTransactionType.INCOME,
                category = "Income",
                appName = appName,
                rawText = combined
            )
        } else {
            // Outgoing Expense (EXPENSE)
            val merchant = extractMerchant(title, combined, appName)
            val category = autoCategorize(merchant)
            ParsedUpiTransaction(
                amountPaise = amountPaise,
                merchantOrSender = merchant,
                type = WalletTransactionType.EXPENSE,
                category = category,
                appName = appName,
                rawText = combined
            )
        }
    }

    private fun extractAmountPaise(text: String): Long? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        val amounts = mutableSetOf<Long>()
        while (matcher.find()) {
            val end = matcher.end()
            if (end < text.length && (text[end].isDigit() || text[end] == '.')) return null
            val raw = matcher.group(1) ?: return null
            if (',' in raw && !Regex("(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})(?:\\.[0-9]{1,2})?").matches(raw)) return null
            val amount = com.invatech.pockit.domain.Money.parse(raw.replace(",", "")) ?: return null
            amounts.add(amount)
        }
        // Multiple distinct amounts (balance, offer, fee) are ambiguous; never guess.
        return amounts.singleOrNull()
    }

    private fun extractMerchant(title: String?, fullText: String, fallbackApp: String): String {
        val matcher = PAYEE_TO_PATTERN.matcher(fullText)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim()
            if (!candidate.isNullOrBlank() && candidate.length > 1 && !isStopWord(candidate)) {
                return cleanPartyName(candidate)
            }
        }

        if (!title.isNullOrBlank()) {
            val cleanTitle = title
                .replace(Regex("""(?i)^(paid to|payment to|sent to|transfer to)\s*"""), "")
                .trim()
            if (cleanTitle.isNotBlank() && cleanTitle.length > 2 && !cleanTitle.equals(fallbackApp, ignoreCase = true)) {
                return cleanPartyName(cleanTitle)
            }
        }

        return "$fallbackApp Payment"
    }

    private fun extractSender(title: String?, fullText: String, fallbackApp: String): String {
        val matcher = PAYER_FROM_PATTERN.matcher(fullText)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim()
            if (!candidate.isNullOrBlank() && candidate.length > 1 && !isStopWord(candidate)) {
                return cleanPartyName(candidate)
            }
        }

        if (!title.isNullOrBlank()) {
            val cleanTitle = title
                .replace(Regex("""(?i)^(received from|money from|transfer from)\s*"""), "")
                .trim()
            if (cleanTitle.isNotBlank() && cleanTitle.length > 2 && !cleanTitle.equals(fallbackApp, ignoreCase = true)) {
                return cleanPartyName(cleanTitle)
            }
        }

        return "$fallbackApp Transfer"
    }

    private fun isStopWord(word: String): Boolean {
        val w = word.lowercase(Locale.ROOT).trim()
        return w in setOf("bank", "account", "a/c", "upi", "card", "vpa", "your", "my", "self", "contact", "wallet")
    }

    private fun cleanPartyName(name: String): String {
        var cleaned = name
            .replace(Regex("""(?i)\s+(ltd|limited|pvt|private|inc|corp|co|payment|successful|via|using)$"""), "")
            .replace(Regex("""[^\w\s&'-]"""), "")
            .trim()

        if (cleaned.length > 25) {
            cleaned = cleaned.take(25).trim()
        }
        return cleaned.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }.ifBlank { "UPI Transfer" }
    }

    private fun autoCategorize(merchant: String): String {
        val m = merchant.lowercase(Locale.ROOT)

        return when {
            listOf("swiggy", "zomato", "starbucks", "chai", "tea", "cafe", "coffee", "restaurant",
                   "mcdonald", "burger", "pizza", "domino", "biryani", "bakery", "food", "haldiram",
                   "kfc", "subway", "eats", "dhaba", "bistro", "diner").any { m.contains(it) } -> "Food"

            listOf("blinkit", "zepto", "instamart", "bigbasket", "supermarket", "kirana", "mart",
                   "dmart", "store", "provision", "vegetable", "fruits", "dairy", "spencer").any { m.contains(it) } -> "Groceries"

            listOf("uber", "ola", "rapido", "metro", "petrol", "fuel", "indian oil", "bharat petroleum",
                   "shell", "hp", "irctc", "rail", "flight", "makemytrip", "goibibo", "fastag", "toll").any { m.contains(it) } -> "Travel"

            listOf("amazon", "flipkart", "myntra", "meesho", "zara", "h&m", "nykaa", "retail",
                   "trends", "reliance", "ajio", "mall", "clothes", "fashion").any { m.contains(it) } -> "Shopping"

            listOf("bescom", "electricity", "water", "gas", "jio", "airtel", "vi", "broadband",
                   "recharge", "tata play", "dish", "utility", "postpaid", "bill").any { m.contains(it) } -> "Bills"

            listOf("apollo", "pharmeasy", "hospital", "clinic", "medical", "pharmacy", "1mg",
                   "netmeds", "dr", "doctor", "health", "dental").any { m.contains(it) } -> "Health"

            listOf("netflix", "spotify", "bookmyshow", "prime", "cinema", "pvr", "inox", "steam",
                   "hotstar", "youtube", "playstation", "game").any { m.contains(it) } -> "Entertainment"

            else -> "Other"
        }
    }
}
