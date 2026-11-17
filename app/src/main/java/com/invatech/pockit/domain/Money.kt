package com.invatech.pockit.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** All persisted money is INR minor units. Floating point is only for chart coordinates. */
object Money {
    /** Recorded balances may be zero or negative; regular transaction amounts remain positive. */
    fun parseBalance(input: String): Long? = runCatching {
        val text = input.trim()
        require(Regex("-?[0-9]+(?:\\.[0-9]{1,2})?").matches(text))
        val paise = java.math.BigDecimal(text).movePointRight(2).longValueExact()
        require(paise in -MAX_AMOUNT..MAX_AMOUNT)
        paise
    }.getOrNull()

    const val MAX_AMOUNT = 100_000_000_000L
    fun parse(text: String): Long? {
        val input = text.trim()
        if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(input)) return null
        return runCatching { BigDecimal(input).movePointRight(2).longValueExact() }
            .getOrNull()?.takeIf { it in 1..MAX_AMOUNT }
    }
    /** Compatibility boundary for historical rupee fields; no floating-point arithmetic. */
    fun legacyPaise(rupees: Double): Long {
        require(rupees.isFinite()) { "Invalid monetary value." }
        return BigDecimal.valueOf(rupees).setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact()
    }
    fun decimal(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()
    fun format(paise: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .format(BigDecimal.valueOf(paise, 2))
    fun sum(values: Iterable<Long>): Long = values.fold(0L, Math::addExact)
    fun percent(spent: Long, limit: Long): Int = if (limit <= 0) 0 else
        BigDecimal.valueOf(spent).multiply(BigDecimal(100)).divide(BigDecimal.valueOf(limit), 0, RoundingMode.DOWN)
            .min(BigDecimal(Int.MAX_VALUE)).toInt()
    fun split(total: Long, count: Int): List<Long> {
        require(total >= 0 && count > 0)
        return List(count) { total / count + if (it < total % count) 1 else 0 }
    }
}
