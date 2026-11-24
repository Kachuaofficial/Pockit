package com.invatech.pockit.ui.commerce

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.data.ads.RewardedFeature
import com.invatech.pockit.data.ads.RewardedFeatureAccess
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.finance.PageHeading
import com.invatech.pockit.ui.home.HomeViewModel
import com.invatech.pockit.ui.home.components.CinematicCard
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun AdvancedReportsScreen(homeViewModel:HomeViewModel=viewModel(),onBack:()->Unit,onPerks:()->Unit) {
    val rewardVersion by RewardedFeatureAccess.updates.collectAsState()
    val reportsUnlocked = remember(rewardVersion) { RewardedFeatureAccess.isUnlocked(RewardedFeature.ADVANCED_REPORTS) }
    if(!reportsUnlocked){
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            item{PageHeading("Advanced reports","See the bigger picture.",onBack)}
            item{CinematicCard{Text("A deeper look at your spending",style=MaterialTheme.typography.headlineSmall)
                Text("Explore up to 12 months, compare spending categories, and export a CSV of your records.",color=PockitSecondaryText)
                Button(onClick=onPerks,modifier=Modifier.fillMaxWidth()){Text("Watch a video · unlock 24 hours")}}}
        };return
    }
    val wallet by homeViewModel.uiState.collectAsState()
    var months by remember{mutableIntStateOf(3)}
    val start=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);add(Calendar.MONTH,1-months)}.timeInMillis
    val now=System.currentTimeMillis()
    val rows=remember(wallet.transactions,start,now){wallet.transactions.filter{(it.createdAt?.toDate()?.time?:0) in start..now}}
    val income=Money.sum(rows.filter{it.type==WalletTransactionType.INCOME}.map{it.amountPaise})
    val spent=Money.sum(rows.filter{it.type==WalletTransactionType.EXPENSE}.map{it.amountPaise})
    val categories=rows.filter{it.type==WalletTransactionType.EXPENSE}.groupBy{it.category}.mapValues{(_,tx)->Money.sum(tx.map{it.amountPaise})}.toList().sortedByDescending{it.second}
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var exporting by remember{mutableStateOf(false)}
    val currentRows by rememberUpdatedState(rows)
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        if(uri!=null)scope.launch{
            exporting=true
            try{
                // Recheck at export time, including after a suspended document-picker round trip.
                check(RewardedFeatureAccess.isUnlocked(RewardedFeature.ADVANCED_REPORTS))
                val csv=reportCsv(currentRows)
                withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use{it.write(csv)}?:error("Could not open export")}
                Toast.makeText(context,"Report exported",Toast.LENGTH_SHORT).show()
            }catch(_:Exception){Toast.makeText(context,"Could not export this report. Please try again.",Toast.LENGTH_SHORT).show()}
            finally{exporting=false}
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{PageHeading("Advanced reports","Your records, with a wider perspective.",onBack)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(3,6,12).forEach{n->FilterChip(months==n,{months=n},label={Text("$n months")})}}}
        item{CinematicCard{Text("LAST $months MONTHS",color=PockitOrange,style=MaterialTheme.typography.labelSmall)
            Text(Money.format(spent),style=MaterialTheme.typography.displaySmall);Text("Total spending",color=PockitSecondaryText)
            HorizontalDivider();Text("Income: ${Money.format(income)}",color=PockitGreen)
            Text("Income minus spending: ${Money.format(income-spent)}",style=MaterialTheme.typography.titleMedium)
            Text("Recorded transactions only. Balance corrections, transfers and shared settlements are not income.",style=MaterialTheme.typography.bodySmall,color=PockitSecondaryText)}}
        if(rows.size>=3)item{com.invatech.pockit.ui.common.ads.PockitNativeAdCard(placementKey="reports.summary.native")}
        item{CinematicCard{Text("Spending by category",style=MaterialTheme.typography.titleLarge)
            if(categories.isEmpty())Text("No spending in this period.",color=PockitSecondaryText)
            categories.forEach{(name,amount)->Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,modifier=Modifier.weight(1f));Text(Money.format(amount))}
                LinearProgressIndicator(progress={(amount.toDouble()/spent.coerceAtLeast(1)).toFloat()},modifier=Modifier.fillMaxWidth(),color=PockitOrange,trackColor=PockitElevated)
            }}
        }}
        item{Button(onClick={export.launch("Pockit-${months}-month-report.csv")},enabled=!exporting && rows.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text(if(exporting)"Exporting…" else "Export CSV")}}
    }
}

internal fun reportCsv(rows:List<com.invatech.pockit.ui.home.HomeTransactionUi>):String {
    fun cell(value:String):String {
        // Spreadsheet formula injection protection; quote every text field and escape quotes.
        val safe=if(value.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r')) "'"+value else value
        return "\""+safe.replace("\"","\"\"")+"\""
    }
    return "Date,Title,Category,Type,Amount (INR)\r\n"+rows.joinToString("\r\n"){
        listOf(cell(it.createdAt?.toDate()?.let{d->java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",java.util.Locale.ROOT).format(d)}?:""),cell(it.title),cell(it.category),cell(it.type.name),Money.decimal(it.amountPaise)).joinToString(",")
    }+"\r\n"
}
