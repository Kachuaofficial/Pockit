package com.invatech.pockit.data.notification

/*
 * Unit tests for supported payment text, incoming/outgoing direction, rejection and ambiguous amounts.
 */

import com.invatech.pockit.ui.home.model.WalletTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpiNotificationParserTest {

    @Test
    fun parse_googlePay_starbucks_detectsExpenseAndCategory() {
        val result = UpiNotificationParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Paid ₹150.00 to Starbucks Coffee",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(15000L, result?.amountPaise)
        assertEquals(WalletTransactionType.EXPENSE, result?.type)
        assertEquals("Food", result?.category)
        assertEquals("Starbucks Coffee", result?.merchantOrSender)
    }

    @Test
    fun parse_phonePe_swiggy_detectsExpenseAndFood() {
        val result = UpiNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Paid ₹420 to Swiggy",
            bigText = "Payment of ₹420 to Swiggy is successful"
        )

        assertNotNull(result)
        assertEquals(42000L, result?.amountPaise)
        assertEquals(WalletTransactionType.EXPENSE, result?.type)
        assertEquals("Food", result?.category)
        assertEquals("Swiggy", result?.merchantOrSender)
    }

    @Test
    fun parse_phonePe_blinkit_detectsGroceries() {
        val result = UpiNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "Paid to Blinkit",
            text = "Payment of ₹350.50 was successful",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(35050L, result?.amountPaise)
        assertEquals(WalletTransactionType.EXPENSE, result?.type)
        assertEquals("Groceries", result?.category)
        assertEquals("Blinkit", result?.merchantOrSender)
    }

    @Test
    fun parse_cred_instamart_detectsAmountWithCommas() {
        val result = UpiNotificationParser.parse(
            packageName = "com.dreamplug.androidapp",
            title = "CRED UPI",
            text = "Paid ₹1,250 on CRED UPI to Zepto",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(125000L, result?.amountPaise)
        assertEquals(WalletTransactionType.EXPENSE, result?.type)
        assertEquals("Groceries", result?.category)
        assertEquals("Zepto", result?.merchantOrSender)
    }

    @Test
    fun parse_uber_detectsTravel() {
        val result = UpiNotificationParser.parse(
            packageName = "net.one97.paytm",
            title = "Paytm",
            text = "Paid ₹285 at Uber India",
            bigText = null
        )

        assertNotNull(result)
        assertEquals(28500L, result?.amountPaise)
        assertEquals(WalletTransactionType.EXPENSE, result?.type)
        assertEquals("Travel", result?.category)
    }

    @Test
    fun parse_incomingMoney_detectsIncome() {
        val result = UpiNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Received ₹500 from Rohit Sharma",
            bigText = null
        )

        assertNotNull("Genuine incoming money must be logged as INCOME", result)
        assertEquals(50000L, result?.amountPaise)
        assertEquals(WalletTransactionType.INCOME, result?.type)
        assertEquals("Rohit Sharma", result?.merchantOrSender)
        assertEquals("Income", result?.category)
    }

    @Test
    fun parse_bankNotification_isExcludedFromUpiOnlyTracking() {
        assertNull(UpiNotificationParser.parse("com.snapwork.hdfc", "HDFC", "Credited INR 2,000.00 by Priya", null))
    }

    @Test
    fun parse_ignorePaymentRequests() {
        val request = UpiNotificationParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Aman has requested ₹350 from you",
            bigText = null
        )
        assertNull("Payment requests are not completed payments", request)
    }

    @Test
    fun parse_ignoreOtpAndReminders() {
        val otp = UpiNotificationParser.parse(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "Your OTP is 492810 for payment of Rs 1500",
            bigText = null
        )
        assertNull(otp)

        val billDue = UpiNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Electricity bill due in 3 days: ₹1,450",
            bigText = null
        )
        assertNull(billDue)
    }

    @Test
    fun parse_ignoreLoanSpam() {
        val loanSpam = UpiNotificationParser.parse(
            packageName = "net.one97.paytm",
            title = "Paytm Postpaid",
            text = "Pre-approved loan up to ₹50,000! Apply now in 2 mins",
            bigText = null
        )
        assertNull(loanSpam)
    }

    @Test fun rejectsUnknownAndOwnApps() {
        listOf("com.whatsapp", "com.invatech.pockit", "com.example.fake").forEach {
            assertNull(UpiNotificationParser.parse(it, "Payment", "Paid ₹400 to Cafe", null))
        }
    }
    @Test fun rejectsBareNumbersAndAmbiguousAmounts() {
        listOf("Sent 123456 to Rahul", "Paid ₹100 to Store. Balance ₹2000", "Paid ₹1.999 to Store", "Paid ₹1,2,3 to Store", "Received ₹100 and paid ₹50", "Payment of ₹500", "You received 123456 points").forEach {
            assertNull(it, UpiNotificationParser.parse("com.phonepe.app", null, it, null))
        }
    }
    @Test fun rejectsUncompletedPaymentsAndPromotions() {
        listOf("Payment of ₹100 pending", "You received ₹100 cashback", "Paid ₹100 failed", "Payment of ₹100 processing", "Refund received ₹100", "Earn ₹100 when you paid to a friend").forEach {
            assertNull(it, UpiNotificationParser.parse("com.phonepe.app", null, it, null))
        }
    }
    @Test fun exactPaiseAndDuplicateText() {
        val text = "Paid ₹0.29 to Store"
        assertEquals(29L, UpiNotificationParser.parse("com.phonepe.app", "PhonePe", text, text)?.amountPaise)
    }
}
