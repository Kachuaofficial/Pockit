package com.invatech.pockit.ui.space.util

import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.domain.Money

data class Settlement(val fromId: String, val toId: String, val amount: Double)

/** Legacy UI exposes rupees, but all balance and settlement arithmetic uses exact paise. */
object BalanceCalculator {
    fun netBalances(space: Space): Map<String, Double> {
        val net = space.members.associate { it.id to 0L }.toMutableMap()
        if (!space.isHousehold) space.expenses.filter { it.isConfirmed }.forEach { expense ->
            net[expense.paidById] = Math.addExact(net[expense.paidById] ?: 0L, Money.legacyPaise(expense.amount))
            expense.shares.forEach { (id, amount) -> net[id] = Math.subtractExact(net[id] ?: 0L, Money.legacyPaise(amount)) }
        }
        return net.mapValues { it.value / 100.0 }
    }
    fun settlements(net: Map<String, Double>): List<Settlement> {
        val paise = net.mapValues { Money.legacyPaise(it.value) }
        require(Money.sum(paise.values) == 0L) { "Balances do not reconcile. Check expense shares." }
        val creditors = paise.filterValues { it > 0 }.toList().sortedByDescending { it.second }.toMutableList()
        val debtors = paise.filterValues { it < 0 }.map { it.key to -it.value }.sortedByDescending { it.second }.toMutableList()
        val result = mutableListOf<Settlement>()
        var i = 0; var j = 0
        while (i < debtors.size && j < creditors.size) {
            val (debtor, debt) = debtors[i]; val (creditor, credit) = creditors[j]
            val pay = minOf(debt, credit)
            result += Settlement(debtor, creditor, pay / 100.0)
            debtors[i] = debtor to debt - pay; creditors[j] = creditor to credit - pay
            if (debt == pay) i++
            if (credit == pay) j++
        }
        return result
    }
    fun splitEqually(amount: Double, ids: List<String>): Map<String, Double> {
        if (ids.isEmpty() || amount <= 0) return emptyMap()
        require(ids.distinct().size == ids.size)
        return ids.zip(Money.split(Money.legacyPaise(amount), ids.size).map { it / 100.0 }).toMap()
    }
}
