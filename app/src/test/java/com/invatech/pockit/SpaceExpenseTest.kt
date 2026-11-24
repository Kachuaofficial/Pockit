package com.invatech.pockit

/*
 * Regression tests for member balances and settlements after expense deletion and restoration.
 */

import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.BalanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceExpenseTest {

    @Test
    fun testExpenseDeletionRebalancesSpace() {
        val alice = Member("alice", "Alice")
        val bob = Member("bob", "Bob")
        val charlie = Member("charlie", "Charlie")

        val expense1 = SpaceExpense(
            id = "exp1",
            title = "Dinner",
            amount = 90.0,
            paidById = "alice",
            shares = mapOf("alice" to 30.0, "bob" to 30.0, "charlie" to 30.0),
            dateLabel = "Today"
        )

        val expense2 = SpaceExpense(
            id = "exp2",
            title = "Cab",
            amount = 30.0,
            paidById = "bob",
            shares = mapOf("alice" to 10.0, "bob" to 10.0, "charlie" to 10.0),
            dateLabel = "Today"
        )

        val initialSpace = Space(
            id = "space1",
            name = "Trip",
            emoji = "✈️",
            themeIndex = 0,
            inviteCode = "TRIP26",
            members = listOf(alice, bob, charlie),
            expenses = listOf(expense1, expense2)
        )

        // Initial net balances:
        // Alice: paid 90, owes 30+10=40 -> net +50
        // Bob: paid 30, owes 30+10=40 -> net -10
        // Charlie: paid 0, owes 30+10=40 -> net -40
        val initialBalances = BalanceCalculator.netBalances(initialSpace)
        assertEquals(50.0, initialBalances["alice"] ?: 0.0, 0.001)
        assertEquals(-10.0, initialBalances["bob"] ?: 0.0, 0.001)
        assertEquals(-40.0, initialBalances["charlie"] ?: 0.0, 0.001)

        // When expense1 is deleted:
        val spaceAfterDelete = initialSpace.copy(
            expenses = initialSpace.expenses.filter { it.id != "exp1" }
        )
        val balancesAfterDelete = BalanceCalculator.netBalances(spaceAfterDelete)
        // Only expense2 remains: Bob paid 30, split equally (10 each)
        // Alice owes 10 -> net -10
        // Bob paid 30, owes 10 -> net +20
        // Charlie owes 10 -> net -10
        assertEquals(-10.0, balancesAfterDelete["alice"] ?: 0.0, 0.001)
        assertEquals(20.0, balancesAfterDelete["bob"] ?: 0.0, 0.001)
        assertEquals(-10.0, balancesAfterDelete["charlie"] ?: 0.0, 0.001)

        // Total spent adjusts automatically
        assertEquals(120.0, initialSpace.totalSpent, 0.001)
        assertEquals(30.0, spaceAfterDelete.totalSpent, 0.001)

        // When restored:
        val spaceAfterRestore = spaceAfterDelete.copy(
            expenses = spaceAfterDelete.expenses + expense1
        )
        val balancesAfterRestore = BalanceCalculator.netBalances(spaceAfterRestore)
        assertEquals(50.0, balancesAfterRestore["alice"] ?: 0.0, 0.001)
        assertEquals(-10.0, balancesAfterRestore["bob"] ?: 0.0, 0.001)
        assertEquals(-40.0, balancesAfterRestore["charlie"] ?: 0.0, 0.001)
    }

    @Test
    fun testSettlementsAfterExpenseDeletion() {
        val alice = Member("alice", "Alice")
        val bob = Member("bob", "Bob")

        val expense = SpaceExpense(
            id = "exp1",
            title = "Movie",
            amount = 50.0,
            paidById = "alice",
            shares = mapOf("alice" to 25.0, "bob" to 25.0),
            dateLabel = "Today"
        )

        val spaceWithExpense = Space(
            id = "s1",
            name = "Cinema",
            emoji = "🎬",
            themeIndex = 1,
            inviteCode = "CINE26",
            members = listOf(alice, bob),
            expenses = listOf(expense)
        )

        val balances = BalanceCalculator.netBalances(spaceWithExpense)
        val settlements = BalanceCalculator.settlements(balances)
        assertEquals(1, settlements.size)
        assertEquals("bob", settlements[0].fromId)
        assertEquals("alice", settlements[0].toId)
        assertEquals(25.0, settlements[0].amount, 0.001)

        // Delete expense
        val spaceAfterDelete = spaceWithExpense.copy(expenses = emptyList())
        val balancesAfterDelete = BalanceCalculator.netBalances(spaceAfterDelete)
        val settlementsAfterDelete = BalanceCalculator.settlements(balancesAfterDelete)
        assertTrue(settlementsAfterDelete.isEmpty())
    }
}
