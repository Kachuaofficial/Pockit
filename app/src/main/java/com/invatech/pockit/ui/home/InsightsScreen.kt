package com.invatech.pockit.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.finance.PageHeading
import com.invatech.pockit.ui.home.components.*
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.*
import java.util.Calendar

@Composable
fun InsightsScreen(homeViewModel: HomeViewModel = viewModel(), onBack: () -> Unit,
    onBudgets: () -> Unit, onTransactions: () -> Unit,onAdvanced:()->Unit={}) {
    val state by homeViewModel.uiState.collectAsState()
    val start=Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)
    }.timeInMillis
    val now=System.currentTimeMillis()
    val rows=remember(state.transactions,start) { state.transactions.filter { (it.createdAt?.toDate()?.time?:0) in start..now } }
    val spent=Money.sum(rows.filter{it.type==WalletTransactionType.EXPENSE}.map{it.amountPaise})
    val income=Money.sum(rows.filter{it.type==WalletTransactionType.INCOME}.map{it.amountPaise})
    val day=Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daily=remember(rows) { (0 until day).map { index ->
        Money.sum(rows.filter { it.type==WalletTransactionType.EXPENSE &&
            Calendar.getInstance().apply{timeInMillis=it.createdAt!!.toDate().time}.get(Calendar.DAY_OF_MONTH)<=index+1 }.map{it.amountPaise})
    } }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Insights","A clearer view of your money.",onBack) }
        item { CinematicCard {
            Text("THIS MONTH",style=MaterialTheme.typography.labelSmall,color=PockitOrange)
            Text(Money.format(spent),style=MaterialTheme.typography.displayMedium)
            Text("Total spent",color=PockitSecondaryText)
            SpendingSparkline(listOf(0L)+daily,Modifier.fillMaxWidth().height(100.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text("Income",color=PockitSecondaryText,style=MaterialTheme.typography.bodySmall);Text(Money.format(income),color=PockitGreen) }
                Column { Text("Daily average",color=PockitSecondaryText,style=MaterialTheme.typography.bodySmall);Text(Money.format(spent/day)) }
            }
        } }
        item { CategorySpending(rows,onTransactions) }
        item { com.invatech.pockit.ui.common.ads.PockitNativeAdCard(placementKey="insights.category.native") }
        item { CinematicCard {
            Text("Go beyond this month",style=MaterialTheme.typography.titleLarge)
            Text("Explore longer-term trends, detailed categories and CSV exports.",color=PockitSecondaryText)
            Button(onClick=onAdvanced,modifier=Modifier.fillMaxWidth()){Text("Open advanced reports")}
        } }
        item { CinematicCard {
            Text("Make room for what matters",style=MaterialTheme.typography.titleLarge)
            Text("Set a monthly limit and track the categories that need a little more attention.",color=PockitSecondaryText)
            Button(onClick=onBudgets,modifier=Modifier.fillMaxWidth()) { Text("Manage budgets") }
            TextButton(onClick=onTransactions,modifier=Modifier.fillMaxWidth()) { Text("Explore transactions →") }
        } }
    }
}
