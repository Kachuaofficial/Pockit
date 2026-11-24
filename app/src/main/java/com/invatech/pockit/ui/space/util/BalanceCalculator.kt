package com.invatech.pockit.ui.space.util

/*
 * Pure confirmed-record accounting and greedy settlement suggestions for bill-split spaces.
 * No Firebase calls or money transfers occur here; tests can exercise the calculations without Android UI.
 */

import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.domain.Money

data class Settlement(val fromId: String, val toId: String, val amount: Double)

/** Legacy UI exposes rupees, but all balance and settlement arithmetic uses exact paise. */
object BalanceCalculator {
    /**
     * Returns paid minus allocated shares for each member using confirmed events only. A positive
     * result is receivable; a negative result is payable. Arithmetic converts legacy rupees into
     * paise and back only at the boundary. Household spaces intentionally do not generate this
     * bill-split debt model.
     */
    fun netBalances(space: Space): Map<String, Double> {
        val net = space.members.associate { it.id to 0L }.toMutableMap()
        if (!space.isHousehold) space.expenses.filter { it.isConfirmed }.forEach { expense ->
            net[expense.paidById] = Math.addExact(net[expense.paidById] ?: 0L, Money.legacyPaise(expense.amount))
            expense.shares.forEach { (id, amount) -> net[id] = Math.subtractExact(net[id] ?: 0L, Money.legacyPaise(amount)) }
        }
        return net.mapValues { it.value / 100.0 }
    }
    /**
     * Matches sorted debtors and creditors after requiring that net positions sum to zero. Sorting
     * costs O(N log N); the two-pointer pass is O(N). Each transfer clears at least one position,
     * yielding at most N-1 transfers for N non-zero positions. This is a valid greedy
     * simplification, not a proof of globally minimum transfer count. Returned amounts are legacy
     * display rupees, not executed payments.
     */
    fun settlements(net: Map<String, Double>): List<Settlement> {
        val paise = net.mapValues { Money.legacyPaise(it.value) }
        require(Money.sum(paise.values) == 0L) { "Balances do not reconcile. Check expense shares." }
        val creditors = paise.filterValues { it > 0 }.toList().sortedByDescending { it.second }.toMutableList()
        val debtors = paise.filterValues { it < 0 }.map { it.key to -it.value }.sortedByDescending { it.second }.toMutableList()
        val result = mutableListOf<Settlement>()
        var i = 0; var j = 0
        while (i < debtors.size && j < creditors.size) {
            val (debtor, debt) = debtors[i]; val (creditor, credit) = creditors[j]
            // Pay only what both sides can reconcile; at least one side reaches zero in this step.
            val pay = minOf(debt, credit)
            result += Settlement(debtor, creditor, pay / 100.0)
            debtors[i] = debtor to debt - pay; creditors[j] = creditor to credit - pay
            if (debt == pay) i++
            if (credit == pay) j++
        }
        return result
    }
    /**
     * Allocates a rupee-valued bill to unique member IDs through Money.split in paise, then maps
     * the exact parts back to the legacy UI model. Empty participants or a non-positive amount
     * return an empty map; duplicate IDs are rejected.
     */
    fun splitEqually(amount: Double, ids: List<String>): Map<String, Double> {
        if (ids.isEmpty() || amount <= 0) return emptyMap()
        require(ids.distinct().size == ids.size)
        return ids.zip(Money.split(Money.legacyPaise(amount), ids.size).map { it / 100.0 }).toMap()
    }
}
