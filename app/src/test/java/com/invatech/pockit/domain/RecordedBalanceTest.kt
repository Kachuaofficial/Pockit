package com.invatech.pockit.domain

/*
 * Unit tests for the separate signed recorded-balance parser and balance-related arithmetic assumptions.
 */

import org.junit.Assert.*
import org.junit.Test

class RecordedBalanceTest {
    @Test fun correctionsAcceptZeroDebtAndExactPaise() {
        assertEquals(0L, Money.parseBalance("0.00"))
        assertEquals(-12501L, Money.parseBalance("-125.01"))
        assertEquals(299L, Money.parseBalance("2.99"))
    }
    @Test fun correctionsRejectRoundingAndOutOfRangeInput() {
        listOf("1.001", "NaN", "1e3", "", "999999999999999999999").forEach { assertNull(Money.parseBalance(it)) }
    }
}
