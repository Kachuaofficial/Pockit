package com.invatech.pockit.ui.space.util

import com.invatech.pockit.ui.space.model.Space
import kotlin.math.roundToLong

data class Settlement(
    val fromId: String,
    val toId: String,
    val amount: Double
)

object BalanceCalculator {

    /** memberId -> net balance. Positive = gets back money, negative = owes money. Only counts confirmed expenses/settlements. */
    fun netBalances(space: Space): Map<String, Double> {
        val net = space.members.associate { it.id to 0.0 }.toMutableMap()
        space.expenses.forEach { expense ->
            if (expense.isConfirmed) {
                net[expense.paidById] = (net[expense.paidById] ?: 0.0) + expense.amount
                expense.shares.forEach { (memberId, share) ->
                    net[memberId] = (net[memberId] ?: 0.0) - share
                }
            }
        }
        return net
    }

    /** Minimal set of payments that settles everyone (greedy). */
    fun settlements(net: Map<String, Double>): List<Settlement> {
        val creditors = net.filter { it.value > 0.01 }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()
        val debtors = net.filter { it.value < -0.01 }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val result = mutableListOf<Settlement>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val (debtorId, debtorAmount) = debtors[i]
            val (creditorId, creditorAmount) = creditors[j]
            val pay = minOf(debtorAmount, creditorAmount)
            result += Settlement(debtorId, creditorId, (pay * 100).roundToLong() / 100.0)

            val debtorLeft = debtorAmount - pay
            val creditorLeft = creditorAmount - pay
            debtors[i] = debtorId to debtorLeft
            creditors[j] = creditorId to creditorLeft
            if (debtorLeft < 0.01) i++
            if (creditorLeft < 0.01) j++
        }
        return result
    }

    /** Splits [amount] equally in paise so the shares always add up exactly. */
    fun splitEqually(amount: Double, ids: List<String>): Map<String, Double> {
        if (ids.isEmpty() || amount <= 0.0) return emptyMap()
        val totalCents = (amount * 100).roundToLong()
        val base = totalCents / ids.size
        val remainder = (totalCents % ids.size).toInt()
        return ids.mapIndexed { index, id ->
            id to (base + if (index < remainder) 1 else 0) / 100.0
        }.toMap()
    }
}
