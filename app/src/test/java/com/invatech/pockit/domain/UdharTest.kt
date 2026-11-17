package com.invatech.pockit.domain
import com.invatech.pockit.data.local.database.FinanceRecord
import com.invatech.pockit.ui.finance.udharBalance
import com.invatech.pockit.ui.space.util.BalanceCalculator
import org.junit.Assert.*
import org.junit.Test

class UdharTest {
    private fun entry(amount: Long, direction: String) = FinanceRecord(direction, "owner", "UDHAR", "Rahul", amount, direction)
    @Test fun partialAndFullRepayments() {
        val rows = listOf(entry(10000, "LENT"), entry(2500, "RECEIVED"))
        assertEquals(7500L, udharBalance(rows))
        assertEquals(0L, udharBalance(rows + entry(7500, "RECEIVED")))
    }
    @Test fun borrowedAndRepaid() { assertEquals(-7000L, udharBalance(listOf(entry(10000, "BORROWED"), entry(3000, "REPAID")))) }
    @Test fun onePaisaDebtIsNotDropped() {
        val payment = BalanceCalculator.settlements(mapOf("a" to 0.01, "b" to -0.01)).single()
        assertEquals(0.01, payment.amount, 0.0)
    }
}
