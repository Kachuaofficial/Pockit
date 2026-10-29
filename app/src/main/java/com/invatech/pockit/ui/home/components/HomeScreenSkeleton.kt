package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.common.loading.ShimmerBox
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkSoft
import com.invatech.pockit.ui.theme.Mint

/**
 * High-production Material 3 Expressive Skeleton screen for HomeScreen.
 * Displayed while the wallet and transactions are loading so users never see
 * a blank white/empty screen or sudden content flashes.
 */
@Composable
fun HomeScreenSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Skeleton
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(150.dp)
                            .height(24.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(105.dp)
                            .height(16.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                ShimmerBox(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape
                )

                ShimmerBox(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape
                )
            }
        }

        // 2. BalanceSection Skeleton
        item {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShimmerBox(
                    modifier = Modifier
                        .width(230.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 3. QuickActions Skeleton
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = CircleShape
                )
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = CircleShape
                )
            }
        }

        // 4. SpendingChartCard Skeleton
        item {
            Surface(
                color = Mint.copy(alpha = 0.6f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(95.dp)
                                    .height(14.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .width(75.dp)
                                    .height(26.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        ShimmerBox(
                            modifier = Modifier
                                .width(70.dp)
                                .height(30.dp),
                            shape = CircleShape
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 7 Bar Skeletons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barHeights = listOf(45.dp, 75.dp, 55.dp, 105.dp, 65.dp, 88.dp, 50.dp)
                        barHeights.forEach { height ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(height),
                                    shape = RoundedCornerShape(50)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(12.dp),
                                    shape = RoundedCornerShape(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. RecentTransactionsCard Skeleton
        item {
            Surface(
                color = Ink,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(20.dp),
                            shape = RoundedCornerShape(6.dp),
                            baseColor = InkSoft,
                            highlightColor = Color(0xFF2E2F35)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        ShimmerBox(
                            modifier = Modifier
                                .width(55.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(6.dp),
                            baseColor = InkSoft,
                            highlightColor = Color(0xFF2E2F35)
                        )
                    }

                    // 3 Transaction Row Skeletons
                    repeat(3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(InkSoft)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                baseColor = Color(0xFF26272B),
                                highlightColor = Color(0xFF33353B)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(16.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    baseColor = Color(0xFF26272B),
                                    highlightColor = Color(0xFF33353B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(12.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    baseColor = Color(0xFF26272B),
                                    highlightColor = Color(0xFF33353B)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(65.dp)
                                        .height(16.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    baseColor = Color(0xFF26272B),
                                    highlightColor = Color(0xFF33353B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ShimmerBox(
                                    modifier = Modifier
                                        .width(45.dp)
                                        .height(12.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    baseColor = Color(0xFF26272B),
                                    highlightColor = Color(0xFF33353B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
