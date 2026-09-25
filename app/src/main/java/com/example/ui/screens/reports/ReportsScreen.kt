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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.util.CsvExporter
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionType
import com.example.domain.model.CategorySummary
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.getCategoryIcon
import com.example.ui.screens.transactions.DateFilterPeriod
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
    val context = LocalContext.current
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var editingCategoryBudget by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(state.isFullscreen) {
        onToggleFullscreen(state.isFullscreen)
    }

    DisposableEffect(Unit) {
        onDispose {
            onToggleFullscreen(false)
        }
    }

    if (showFilterBottomSheet) {
        FilterBottomSheet(
            period = state.selectedPeriod,
            customStart = state.customStartTimestamp,
            customEnd = state.customEndTimestamp,
            accountId = state.selectedAccountId,
            categoryIds = state.selectedCategoryIds,
            transactionType = state.selectedType,
            accounts = state.accounts,
            categories = state.categories,
            allowTransfer = false,
            onDismiss = { showFilterBottomSheet = false },
            onApply = { period, customStart, customEnd, accountId, categoryIds, type ->
                viewModel.applyFilters(period, customStart, customEnd, accountId, categoryIds, type)
                showFilterBottomSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterBottomSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارش‌های مالی", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(
                        badge = {
                            if (state.activeFilterCount > 0) {
                                Badge {
                                    Text(state.activeFilterCount.toString().toPersianDigits())
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { showFilterBottomSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "فیلترها"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val csv = viewModel.exportReportCsv()
                            CsvExporter.shareCsv(context, csv, "fintrack_report.csv")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "خروجی اکسل (CSV)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Financial Insights Card (Month-over-Month comparison & daily average)
                item {
                    FinancialInsightCard(insight = state.financialInsight)
                }

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
                    val isExpense = state.selectedType != TransactionType.INCOME
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
                                ChartLegend(categorySummaries = state.categorySummaries)
                            }
                        }
                    }
                }

                // 6-Month Trend Chart
                item {
                    MonthlyTrendCard(monthlyTrends = state.monthlyTrends)
                }

                // Category Budgeting Section (Monthly spending limits)
                item {
                    CategoryBudgetSection(
                        budgetList = state.budgetProgressList,
                        allCategories = state.categories,
                        onSetBudgetClick = { editingCategoryBudget = it }
                    )
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
                                    summary.category.type.name
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    editingCategoryBudget?.let { cat ->
        BudgetEditDialog(
            category = cat,
            onDismiss = { editingCategoryBudget = null },
            onSave = { newBudget ->
                viewModel.updateCategoryBudget(cat.id, newBudget)
            }
        )
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
fun ChartLegend(
    categorySummaries: List<CategorySummary>,
    modifier: Modifier = Modifier
) {
    val topItems = categorySummaries.take(6)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rows = topItems.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { summary ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(summary.category.color))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = summary.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%.1f٪".format(summary.percentage).toPersianDigits(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendCard(
    monthlyTrends: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    if (monthlyTrends.isEmpty()) return

    val maxAmount = monthlyTrends.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L

    Card(
        modifier = modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with title and Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "روند درآمد و هزینه (۶ ماه اخیر)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "درآمد",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هزینه",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bar Chart Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrends.forEach { trend ->
                    val incomeRatio = if (maxAmount > 0L) (trend.income.toFloat() / maxAmount).coerceIn(0.04f, 1f) else 0.04f
                    val expenseRatio = if (maxAmount > 0L) (trend.expense.toFloat() / maxAmount).coerceIn(0.04f, 1f) else 0.04f

                    val hasIncome = trend.income > 0L
                    val hasExpense = trend.expense > 0L

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Bars container
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .height(95.dp)
                                .padding(horizontal = 4.dp),
                            content = {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(if (hasIncome) incomeRatio else 0.03f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (hasIncome) IncomeColor else IncomeColor.copy(alpha = 0.15f))
                                )
                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(if (hasExpense) expenseRatio else 0.03f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (hasExpense) ExpenseColor else ExpenseColor.copy(alpha = 0.15f))
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Month label
                        Text(
                            text = trend.monthName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
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

@Composable
fun FinancialInsightCard(
    insight: FinancialInsight?,
    modifier: Modifier = Modifier
) {
    if (insight == null || (insight.currentMonthExpense == 0L && insight.lastMonthExpense == 0L)) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "هوش و بینش مالی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تحلیل هوشمند خرج‌کرد ماه جاری نسبت به گذشته",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Month over Month diff badge
            if (insight.expenseDiffPercent != null) {
                val isIncreased = insight.isExpenseIncreased
                val diffAbs = kotlin.math.abs(insight.expenseDiffPercent)
                val diffFormatted = String.format("%.1f", diffAbs).toPersianDigits()
                val badgeColor = if (isIncreased) ExpenseColor else IncomeColor
                val icon = if (isIncreased) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                val statusText = if (isIncreased) {
                    "نسبت به ماه قبل $diffFormatted٪ بیشتر خرج کرده‌اید."
                } else {
                    "نسبت به ماه قبل $diffFormatted٪ صرفه‌جویی و کمتر خرج کرده‌اید! 👏"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            // Stats Row: Daily Average & Top Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily average
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "میانگین روزانه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(insight.dailyAverageExpenseThisMonth),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Top Spending Category
                if (insight.topSpendingCategoryName != null && insight.topSpendingCategoryPercent > 0) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "بیشترین خرج ماه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${insight.topSpendingCategoryName} (${String.format("%.0f", insight.topSpendingCategoryPercent).toPersianDigits()}٪)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetSection(
    budgetList: List<CategoryBudgetProgress>,
    allCategories: List<CategoryEntity>,
    onSetBudgetClick: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سقف بودجه ماهانه دسته‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "کنترل و مدیریت سقف هزینه‌ها در ماه جاری",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (budgetList.isEmpty()) {
                Text(
                    text = "هنوز سقف بودجه‌ای برای دسته‌بندی‌ها تنظیم نشده است. می‌توانید با کلیک روی دسته‌ها سقف هزینه تعیین کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                budgetList.forEach { item ->
                    val progressFraction = (item.percentage / 100f).coerceIn(0f, 1f)
                    val progressColor = when {
                        item.percentage >= 100f -> ExpenseColor
                        item.percentage >= 75f -> Color(0xFFF57C00) // Orange warning
                        else -> IncomeColor
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { onSetBudgetClick(item.category) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(item.category.color).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(item.category.icon),
                                        contentDescription = null,
                                        tint = Color(item.category.color),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${AmountFormatter.format(item.spentThisMonth)} از ${AmountFormatter.format(item.budget)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (item.isOverBudget) {
                                Text(
                                    text = "⚠️ عبور از سقف مجاز (${String.format("%.0f", item.percentage).toPersianDigits()}٪)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "باقیمانده: ${AmountFormatter.format(item.remaining)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${String.format("%.0f", item.percentage).toPersianDigits()}٪ مصرف شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick button to pick an expense category and set budget
            val expenseCats = allCategories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
            if (expenseCats.isNotEmpty()) {
                var showCatPickerForBudget by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showCatPickerForBudget = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظیم یا تغییر سقف بودجه دسته‌ها")
                }

                if (showCatPickerForBudget) {
                    AlertDialog(
                        onDismissRequest = { showCatPickerForBudget = false },
                        title = { Text("انتخاب دسته برای سقف بودجه", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(expenseCats) { cat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                showCatPickerForBudget = false
                                                onSetBudgetClick(cat)
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(cat.color).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(cat.icon),
                                                contentDescription = null,
                                                tint = Color(cat.color),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = cat.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (cat.monthlyBudget > 0) {
                                            Text(
                                                text = AmountFormatter.format(cat.monthlyBudget),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showCatPickerForBudget = false }) { Text("بستن") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetEditDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var budgetText by remember { mutableStateOf(if (category.monthlyBudget > 0) (category.monthlyBudget / 10).toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سقف بودجه ماهانه: ${category.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سقف مجاز خرج‌کرد ماهانه این دسته را به تومان وارد کنید (برای حذف سقف، عدد ۰ را بگذارید):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("سقف ماهانه (تومان)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val toman = budgetText.toLongOrNull() ?: 0L
                val rial = toman * 10
                onSave(rial)
                onDismiss()
            }) {
                Text("ذخیره سقف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
