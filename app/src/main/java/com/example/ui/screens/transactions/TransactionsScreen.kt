package com.example.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.ui.components.TransactionCard
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToEditTransaction: (Long) -> Unit,
    onToggleFullscreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
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

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف تراکنش", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این تراکنش اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tx = transactionToDelete
                        if (tx != null) {
                            viewModel.deleteTransaction(tx)
                            transactionToDelete = null

                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "تراکنش حذف شد",
                                    actionLabel = "بازگردانی",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDelete()
                                }
                            }
                        }
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (!state.isFullscreen) {
                TopAppBar(
                    title = {
                        if (state.isSearchActive) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("جستجو در توضیحات و دسته...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Text("تراکنش‌ها", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setSearchActive(!state.isSearchActive) }
                        ) {
                            Icon(
                                imageVector = if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "جستجو"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "تمام‌صفحه"
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (state.isFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!state.isFullscreen) {
                    // Date Filter Tabs (Today / Week / Month / All)
                    TabRow(
                        selectedTabIndex = DateFilterPeriod.entries.indexOf(state.selectedPeriod),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        DateFilterPeriod.entries.forEach { period ->
                            val selected = state.selectedPeriod == period
                            Tab(
                                selected = selected,
                                onClick = {
                                    if (period == DateFilterPeriod.CUSTOM) {
                                        showDateRangePicker = true
                                    } else {
                                        viewModel.setPeriod(period)
                                    }
                                },
                                text = {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // Type Filter Chips (All / Expense / Income / Transfer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedType == null,
                            onClick = { viewModel.setType(null) },
                            label = { Text("همه") }
                        )
                        FilterChip(
                            selected = state.selectedType == TransactionType.EXPENSE,
                            onClick = { viewModel.setType(TransactionType.EXPENSE) },
                            label = { Text("هزینه") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseColor.copy(alpha = 0.2f),
                                selectedLabelColor = ExpenseColor
                            )
                        )
                        FilterChip(
                            selected = state.selectedType == TransactionType.INCOME,
                            onClick = { viewModel.setType(TransactionType.INCOME) },
                            label = { Text("درآمد") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeColor.copy(alpha = 0.2f),
                                selectedLabelColor = IncomeColor
                            )
                        )
                        FilterChip(
                            selected = state.selectedType == TransactionType.TRANSFER,
                            onClick = { viewModel.setType(TransactionType.TRANSFER) },
                            label = { Text("انتقال") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TransferColor.copy(alpha = 0.2f),
                                selectedLabelColor = TransferColor
                            )
                        )
                    }

                    // Account Filter Chips
                    if (state.accounts.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedAccountId == null,
                                    onClick = { viewModel.setAccountFilter(null) },
                                    label = { Text("همه حساب‌ها") }
                                )
                            }
                            items(state.accounts, key = { it.id }) { acc ->
                                FilterChip(
                                    selected = state.selectedAccountId == acc.id,
                                    onClick = { viewModel.setAccountFilter(acc.id) },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }
                }

                // Grouped Transaction List
                if (state.groupedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هیچ تراکنشی یافت نشد.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = if (state.isFullscreen) PaddingValues(horizontal = 8.dp, vertical = (4.dp / state.zoomLevel).coerceIn(1.dp, 12.dp)) else PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(if (state.isFullscreen) (4.dp / state.zoomLevel).coerceIn(1.dp, 12.dp) else 12.dp)
                    ) {
                        state.groupedTransactions.forEach { (dateHeader, transactions) ->
                            item(key = dateHeader) {
                                Text(
                                    text = dateHeader,
                                    style = if (state.isFullscreen) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                                    fontSize = if (state.isFullscreen) (13.sp * state.zoomLevel) else TextUnit.Unspecified,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        top = if (state.isFullscreen) (4.dp / state.zoomLevel).coerceIn(1.dp, 8.dp) else 8.dp,
                                        bottom = if (state.isFullscreen) (2.dp / state.zoomLevel).coerceIn(1.dp, 4.dp) else 4.dp
                                    )
                                )
                            }

                            items(transactions, key = { it.transaction.id }) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                            transactionToDelete = item.transaction
                                            false // return false so item stays until confirmed
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val isDismissing = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(if (state.isFullscreen) 10.dp else 16.dp))
                                                .background(if (isDismissing) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            if (isDismissing) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        if (state.isFullscreen) {
                                            CompactTransactionCard(
                                                transactionWithDetails = item,
                                                zoomLevel = state.zoomLevel,
                                                onClick = { onNavigateToEditTransaction(item.transaction.id) }
                                            )
                                        } else {
                                            TransactionCard(
                                                transactionWithDetails = item,
                                                onClick = { onNavigateToEditTransaction(item.transaction.id) }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (state.isFullscreen) {
                Surface(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.zoomIn() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "بزرگ‌نمایی",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.zoomOut() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "کوچک‌نمایی",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "خروج از تمام‌صفحه",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTransactionCard(
    transactionWithDetails: TransactionWithDetails,
    zoomLevel: Float = 1.0f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tx = transactionWithDetails.transaction
    val (amountColor, amountPrefix, defaultIcon) = when (tx.type) {
        TransactionType.EXPENSE -> Triple(ExpenseColor, "-", transactionWithDetails.categoryIcon ?: "more_horiz")
        TransactionType.INCOME -> Triple(IncomeColor, "+", transactionWithDetails.categoryIcon ?: "attach_money")
        TransactionType.TRANSFER -> Triple(TransferColor, "", "swap_horiz")
    }

    val iconColor = transactionWithDetails.categoryColor?.let { Color(it) } ?: amountColor

    val titleText = when (tx.type) {
        TransactionType.EXPENSE, TransactionType.INCOME -> transactionWithDetails.categoryName ?: "بدون دسته‌بندی"
        TransactionType.TRANSFER -> "انتقال بین حساب‌ها"
    }

    val subtitleText = when (tx.type) {
        TransactionType.EXPENSE, TransactionType.INCOME -> transactionWithDetails.accountName ?: ""
        TransactionType.TRANSFER -> "${transactionWithDetails.accountName ?: ""} ← ${transactionWithDetails.toAccountName ?: ""}"
    }

    val cardPadding = (8.dp / zoomLevel).coerceIn(2.dp, 14.dp)
    val iconBoxSize = (32.dp * zoomLevel).coerceIn(18.dp, 56.dp)
    val innerIconSize = (18.dp * zoomLevel).coerceIn(12.dp, 32.dp)
    val titleFontSize = (13.sp * zoomLevel)
    val amountFontSize = (14.sp * zoomLevel)
    val subFontSize = (11.sp * zoomLevel)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (tx.type == TransactionType.TRANSFER) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "انتقال",
                        tint = TransferColor,
                        modifier = Modifier.size(innerIconSize)
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(defaultIcon),
                        contentDescription = titleText,
                        tint = iconColor,
                        modifier = Modifier.size(innerIconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (tx.note.isNullOrBlank()) subtitleText else "$subtitleText (${tx.note})",
                    fontSize = subFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix ${AmountFormatter.format(tx.amount)}",
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = DateFormatter.formatShort(tx.date),
                    fontSize = subFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
