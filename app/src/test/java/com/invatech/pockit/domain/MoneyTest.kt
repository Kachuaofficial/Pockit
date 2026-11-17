package com.invatech.pockit.domain
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun parsesExactPaise() { assertEquals(29L, Money.parse("0.29")); assertEquals(123456L, Money.parse("1234.56")); assertEquals(100L, Money.parse("1")) }
    @Test fun rejectsInvalidAmounts() { listOf("", "0", "-1", "NaN", "Infinity", "1e5", "1.001", "1,23", "99999999999999999999").forEach { assertNull(it, Money.parse(it)) } }
    @Test fun splitConservesEveryPaisa() { for (amount in 1L..100L) for (count in 1..20) { val split = Money.split(amount, count); assertEquals(amount, Money.sum(split)); assertTrue(split.max() - split.min() <= 1) } }
    @Test fun percentageDoesNotOverflow() { assertEquals(100, Money.percent(Long.MAX_VALUE, Long.MAX_VALUE)); assertEquals(125, Money.percent(125, 100)) }
    @Test(expected = ArithmeticException::class) fun sumDetectsOverflow() { Money.sum(listOf(Long.MAX_VALUE, 1)) }
    @Test fun decimalRoundTrip() { listOf(1L, 29L, 99999999L, Money.MAX_AMOUNT).forEach { assertEquals(it, Money.parse(Money.decimal(it))) } }
}
