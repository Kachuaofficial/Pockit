package com.invatech.pockit.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.invatech.pockit.MainActivity
import com.invatech.pockit.data.local.database.PockitDatabase
import com.invatech.pockit.ui.common.mascot.MascotCustomizationManager
import com.invatech.pockit.ui.home.model.WalletTransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PockitHomeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Query local Room database off the main thread
        val db = runCatching { PockitDatabase.getDatabase(context) }.getOrNull()
        val transactions = db?.walletDao()?.getAllTransactions() ?: emptyList()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        var todaySpentPaise = 0L
        var totalIncomePaise = 0L
        var totalExpensePaise = 0L

        for (tx in transactions) {
            if (tx.type == WalletTransactionType.EXPENSE) {
                totalExpensePaise += tx.amountPaise
                if (tx.createdAtMillis >= startOfToday) {
                    todaySpentPaise += tx.amountPaise
                }
            } else if (tx.type == WalletTransactionType.INCOME) {
                totalIncomePaise += tx.amountPaise
            }
        }

        val balancePaise = totalIncomePaise - totalExpensePaise
        val todaySpentRupees = todaySpentPaise / 100
        val balanceRupees = balancePaise / 100

        val indianFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"))
        val formattedTodaySpent = "₹" + indianFormat.format(todaySpentRupees)
        val formattedBalance = "₹" + indianFormat.format(balanceRupees)

        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val equippedAccessory = MascotCustomizationManager.equippedAccessory.value
        val mascotEmoji = if (equippedAccessory.iconEmoji.isNotBlank()) equippedAccessory.iconEmoji else "🐾"

        provideContent {
            WidgetContent(
                context = context,
                dateString = dateString,
                todaySpent = formattedTodaySpent,
                balance = formattedBalance,
                mascotEmoji = mascotEmoji
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        dateString: String,
        todaySpent: String,
        balance: String,
        mascotEmoji: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "quick_add")
            putExtra("open_add_transaction", true)
        }

        val primaryText = ColorProvider(Color(0xFF2C241E), Color(0xFF2C241E))
        val mutedText = ColorProvider(Color(0xFF8D6E63), Color(0xFF8D6E63))
        val expenseRed = ColorProvider(Color(0xFFC62828), Color(0xFFC62828))
        val balanceGreen = ColorProvider(Color(0xFF2E7D32), Color(0xFF2E7D32))
        val whiteText = ColorProvider(Color.White, Color.White)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFFFFDF9))
                .cornerRadius(22.dp)
                .clickable(actionStartActivity(openAppIntent))
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header Row: Mascot, App Name, Date
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$mascotEmoji Pockit",
                        style = TextStyle(
                            color = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = dateString,
                        style = TextStyle(
                            color = mutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Stats Bento Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Today's Spent
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFFFFEFEA))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "TODAY'S SPENT",
                            style = TextStyle(
                                color = expenseRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = todaySpent,
                            style = TextStyle(
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Right Column: Balance
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFFE8F5E9))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "BALANCE",
                            style = TextStyle(
                                color = balanceGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = balance,
                            style = TextStyle(
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom Action Button Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick "+ Add Expense" Pill Button
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(Color(0xFF2C241E))
                            .cornerRadius(10.dp)
                            .clickable(actionStartActivity(quickAddIntent))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Add Expense",
                            style = TextStyle(
                                color = whiteText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
