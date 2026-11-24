package com.invatech.pockit.domain

/*
 * Shared monetary rules for parsing, formatting and exact arithmetic in integer INR paise.
 * Wallet and allocation code use these helpers so decimal input and residual paise follow one policy.
 */

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Canonical wallet arithmetic uses INR paise; legacy space/friend rupee fields convert at their boundaries. */
object Money {
    /**
     * Parses a user-recorded balance into paise. Zero and negative balances are allowed here;
     * ordinary income/expense entry uses parse instead. Invalid syntax, excess decimal scale and
     * values outside the configured range return null.
     */
    fun parseBalance(input: String): Long? = runCatching {
        val text = input.trim()
        require(Regex("-?[0-9]+(?:\\.[0-9]{1,2})?").matches(text))
        val paise = java.math.BigDecimal(text).movePointRight(2).longValueExact()
        require(paise in -MAX_AMOUNT..MAX_AMOUNT)
        paise
    }.getOrNull()

    const val MAX_AMOUNT = 100_000_000_000L
    /**
     * Parses a positive decimal rupee string into Long paise, or returns null for invalid input.
     * BigDecimal reads the text directly, and longValueExact prevents silent truncation or
     * overflow. A regular transaction may have at most two decimal places.
     */
    fun parse(text: String): Long? {
        val input = text.trim()
        if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(input)) return null
        return runCatching { BigDecimal(input).movePointRight(2).longValueExact() }
            .getOrNull()?.takeIf { it in 1..MAX_AMOUNT }
    }
    /**
     * Compatibility boundary for historical Double rupee fields. Rejects non-finite input, rounds
     * to two decimal places using HALF_EVEN and returns exact integer paise. New wallet values
     * already use paise and should not be passed through this conversion twice.
     */
    fun legacyPaise(rupees: Double): Long {
        require(rupees.isFinite()) { "Invalid monetary value." }
        return BigDecimal.valueOf(rupees).setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact()
    }
    /**
     * Returns plain decimal rupees without a currency symbol, suitable for pre-filling an editable
     * amount field.
     */
    fun decimal(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()
    /**
     * Formats integer paise as INR for display using decimal arithmetic. Formatting never changes
     * the stored amount.
     */
    fun format(paise: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .format(BigDecimal.valueOf(paise, 2))
    /**
     * Adds paise with checked Long arithmetic. Overflow throws instead of wrapping a financial
     * total.
     */
    fun sum(values: Iterable<Long>): Long = values.fold(0L, Math::addExact)
    /**
     * Calculates a bounded integer spending percentage. A non-positive budget gives zero, and
     * values above the integer range are clamped for display.
     */
    fun percent(spent: Long, limit: Long): Int = if (limit <= 0) 0 else
        BigDecimal.valueOf(spent).multiply(BigDecimal(100)).divide(BigDecimal.valueOf(limit), 0, RoundingMode.DOWN)
            .min(BigDecimal(Int.MAX_VALUE)).toInt()
    /**
     * Divides non-negative paise across a positive number of positions. Each receives total/count,
     * and the first total%count positions receive one extra paisa. The parts sum exactly to total;
     * the caller supplies a stable participant order.
     */
    fun split(total: Long, count: Int): List<Long> {
        require(total >= 0 && count > 0)
        return List(count) { total / count + if (it < total % count) 1 else 0 }
    }
}
