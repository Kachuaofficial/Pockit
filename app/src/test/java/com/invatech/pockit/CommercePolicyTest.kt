package com.invatech.pockit
import com.invatech.pockit.ui.commerce.reportCsv
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.model.WalletTransactionType
import org.junit.Assert.*
import org.junit.Test

class CommercePolicyTest {
    @Test fun csvQuotesUserTextAndPreventsSpreadsheetFormulaInjection() {
        val csv=reportCsv(listOf(HomeTransactionUi("a"," =HYPERLINK(\"https://example.com\")",1250,WalletTransactionType.EXPENSE,"Food, dining")))
        assertTrue(csv.contains("\"' =HYPERLINK(\"\"https://example.com\"\")\""))
        assertTrue(csv.contains("\"Food, dining\""));assertTrue(csv.contains(",12.50\r\n"))
    }
}
