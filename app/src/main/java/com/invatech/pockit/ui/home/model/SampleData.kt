package com.invatech.pockit.ui.home.model

/** Temporary data so the UI looks alive. Replace with real data from your ViewModel/Room later. */
object SampleData {

    val transactions = listOf(
        Transaction(1, "Swiggy", Category.Food, 450.0, true, "Today"),
        Transaction(2, "Salary", Category.Salary, 45000.0, false, "Today"),
        Transaction(3, "Uber", Category.Travel, 220.0, true, "Yesterday"),
        Transaction(4, "Myntra", Category.Shopping, 1899.0, true, "Yesterday"),
        Transaction(5, "Netflix", Category.Entertainment, 649.0, true, "Mon")
    )

    val week = listOf(
        DaySpending("M", 320f),
        DaySpending("T", 640f),
        DaySpending("W", 450f),
        DaySpending("T", 980f),
        DaySpending("F", 560f),
        DaySpending("S", 1200f),
        DaySpending("S", 170f)
    )
}
