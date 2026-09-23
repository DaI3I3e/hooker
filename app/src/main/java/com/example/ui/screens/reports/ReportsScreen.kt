package com.example.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CategorySummary
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.toPersianDigits

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onCategoryClick: ((categoryId: Long, startDate: Long, endDate: Long, accountId: Long?, type: String) -> Unit)? = null,
    onToggleFullscreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isFullscreen) {
        onToggleFullscreen(state.isFullscreen)
    }

    DisposableEffect(Unit) {
        onDispose {
            onToggleFullscreen(false)
        }
    }

    if (showDateRangePicker) {
        com.example.ui.components.JalaliDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onRangeSelected = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showDateRangePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارش‌های مالی", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFullscreen() }
                    ) {
                        Icon(
                            imageVector = if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (state.isFullscreen) "خروج از تمام‌صفحه" else "تمام‌صفحه"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.isFullscreen) {
                // Compact Filter Bar (Period | Account | Category)
                val periodLabel = when (state.selectedPeriod) {
                    ReportPeriod.TODAY -> "امروز"
                    ReportPeriod.WEEK -> "هفته اخیر"
                    ReportPeriod.MONTH -> "ماه جاری"
                    ReportPeriod.ALL -> "همه زمان‌ها"
                    ReportPeriod.CUSTOM -> "دلخواه"
                }
                com.example.ui.components.CompactFilterBar(
                    periodLabel = periodLabel,
                    isPeriodActive = state.selectedPeriod != ReportPeriod.ALL,
                    onSelectPeriod = { key ->
                        val period = when (key) {
                            "TODAY" -> ReportPeriod.TODAY
                            "WEEK" -> ReportPeriod.WEEK
                            "MONTH" -> ReportPeriod.MONTH
                            "ALL" -> ReportPeriod.ALL
                            else -> ReportPeriod.ALL
                        }
                        viewModel.setPeriod(period)
                    },
                    onRequestCustomDate = { showDateRangePicker = true },
                    accounts = state.accounts,
                    selectedAccountId = state.selectedAccountId,
                    onSelectAccount = { viewModel.setAccountFilter(it) },
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onSelectCategory = { viewModel.setCategoryFilter(it) }
                )

                // Income vs Expense Report Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedReportType == com.example.data.local.entity.TransactionType.EXPENSE,
                        onClick = { viewModel.setReportType(com.example.data.local.entity.TransactionType.EXPENSE) },
                        label = { Text("گزارش هزینه‌ها") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseColor.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = state.selectedReportType == com.example.data.local.entity.TransactionType.INCOME,
                        onClick = { viewModel.setReportType(com.example.data.local.entity.TransactionType.INCOME) },
                        label = { Text("گزارش درآمدها") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeColor.copy(alpha = 0.2f),
                            selectedLabelColor = IncomeColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card (Income / Expense / Balance)
                item {
                    ReportSummaryCard(
                        totalIncome = state.totalIncome,
                        totalExpense = state.totalExpense,
                        netBalance = state.netBalance
                    )
                }

                // Donut Chart Card
                item {
                    val isExpense = state.selectedReportType == com.example.data.local.entity.TransactionType.EXPENSE
                    val totalForType = if (isExpense) state.totalExpense else state.totalIncome
                    val chartTitle = if (isExpense) "نمودار هزینه‌ها به تفکیک دسته" else "نمودار درآمدها به تفکیک دسته"
                    val emptyText = if (isExpense) "هیچ تراکنش هزینه‌ای در این بازه ثبت نشده است." else "هیچ تراکنش درآمدی در این بازه ثبت نشده است."

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = chartTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                textAlign = TextAlign.Start
                            )

                            if (state.categorySummaries.isEmpty() || totalForType == 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emptyText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                DonutChart(
                                    categorySummaries = state.categorySummaries,
                                    totalAmount = totalForType,
                                    isExpense = isExpense,
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                }

                // Category Breakdowns List Header
                item {
                    Text(
                        text = "تفکیک دسته‌بندی‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.categorySummaries.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "اطلاعاتی برای نمایش وجود ندارد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.categorySummaries, key = { it.category.id }) { summary ->
                        CategorySummaryItem(
                            summary = summary,
                            onClick = {
                                onCategoryClick?.invoke(
                                    summary.category.id,
                                    state.currentStartTimestamp,
                                    state.currentEndTimestamp,
                                    state.selectedAccountId,
                                    state.selectedReportType.name
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSummaryCard(
    totalIncome: Long,
    totalExpense: Long,
    netBalance: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "درآمد کل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "هزینه کل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مانده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AmountFormatter.format(netBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) IncomeColor else ExpenseColor
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    categorySummaries: List<CategorySummary>,
    totalAmount: Long,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeWidth = 28.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            var startAngle = -90f

            if (totalAmount <= 0L || categorySummaries.isEmpty()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
            } else {
                categorySummaries.forEach { summary ->
                    val sweepAngle = (summary.percentage / 100f) * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = Color(summary.category.color),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }

        // Center Text inside Donut
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isExpense) "مجموع هزینه" else "مجموع درآمد",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = AmountFormatter.format(totalAmount, includeCurrency = false),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) ExpenseColor else IncomeColor
            )
            Text(
                text = "ریال",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategorySummaryItem(
    summary: CategorySummary,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(summary.category.color)
    val percentageText = "%.1f".format(summary.percentage).toPersianDigits() + "٪"
    val countText = "${summary.transactionCount}".toPersianDigits() + " تراکنش"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(summary.category.icon),
                        contentDescription = summary.category.name,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount & Percentage
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = AmountFormatter.format(summary.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor
                    )
                }
            }

            // Percentage Bar
            LinearProgressIndicator(
                progress = { (summary.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.2f)
            )
        }
    }
}
