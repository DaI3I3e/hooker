package com.example.ui.screens.group_expenses

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GroupExpenseShareEntity
import com.example.data.local.entity.GroupExpenseShareMode
import com.example.data.local.relation.GroupExpenseWithShares
import com.example.ui.components.EmptyState
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupExpensesScreen(
    viewModel: GroupExpensesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("هزینه‌های مشترک (دنگ)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Cards Header
            GroupExpenseSummaryHeader(
                totalPending = state.totalPendingAmount,
                pendingCount = state.pendingPeopleCount,
                totalSettled = state.totalSettledAmount
            )

            // Filter Tabs
            TabRow(
                selectedTabIndex = if (state.filterOnlyUnsettled) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = state.filterOnlyUnsettled,
                    onClick = { viewModel.setFilterOnlyUnsettled(true) },
                    text = { Text("تسویه‌نشده‌ها (${state.pendingPeopleCount})", fontWeight = FontWeight.Medium) }
                )
                Tab(
                    selected = !state.filterOnlyUnsettled,
                    onClick = { viewModel.setFilterOnlyUnsettled(false) },
                    text = { Text("همه هزینه‌ها", fontWeight = FontWeight.Medium) }
                )
            }

            // List of Group Expenses
            if (state.groupExpenses.isEmpty()) {
                EmptyState(
                    title = if (state.filterOnlyUnsettled) "دنگ تسویه‌نشده‌ای وجود ندارد" else "هنوز هزینه مشترکی ثبت نشده است",
                    icon = Icons.Default.Groups,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = state.groupExpenses,
                        key = { it.groupExpense.id }
                    ) { expenseItem ->
                        GroupExpenseCard(
                            expense = expenseItem,
                            onSettleShare = { share -> viewModel.openSettleDialog(share, expenseItem) },
                            onUnsettleShare = { share -> viewModel.openUnsettleDialog(share) },
                            onShareDong = { share -> viewModel.shareSingleDong(context, expenseItem, share) },
                            onShareAll = { viewModel.shareAllDongs(context, expenseItem) }
                        )
                    }
                }
            }
        }
    }

    // Settle Share Dialog
    state.settlingShare?.let { share ->
        val expense = state.settlingExpense
        SettleDongDialog(
            share = share,
            expense = expense,
            accounts = state.accounts,
            selectedAccountId = state.selectedIncomeAccountId,
            createIncomeTx = state.createIncomeTransaction,
            onSelectAccount = { viewModel.setSelectedIncomeAccountId(it) },
            onToggleCreateIncome = { viewModel.setCreateIncomeTransaction(it) },
            onConfirm = { viewModel.confirmSettleShare() },
            onDismiss = { viewModel.closeSettleDialog() }
        )
    }

    // Unsettle Share Dialog
    state.unsettlingShare?.let { share ->
        AlertDialog(
            onDismissRequest = { viewModel.closeUnsettleDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = ExpenseColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "لغو تسویه دنگ",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "آیا از بازگردانی وضعیت دنگ «${share.personName}» به تسویه‌نشده مطمئن هستید؟ در صورتی که تراکنش درآمدی ثبت شده باشد، آن تراکنش حذف خواهد شد.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmUnsettleShare() },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("لغو تسویه")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeUnsettleDialog() }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun GroupExpenseSummaryHeader(
    totalPending: Long,
    pendingCount: Int,
    totalSettled: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "مجموع طلب‌های دنگ تسویه‌نشده",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$pendingCount مورد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = AmountFormatter.formatAmountWithCurrency(totalPending),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (totalPending > 0) ExpenseColor else MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموع مبالغ تسویه شده:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = AmountFormatter.formatAmountWithCurrency(totalSettled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeColor
                )
            }
        }
    }
}

@Composable
fun GroupExpenseCard(
    expense: GroupExpenseWithShares,
    onSettleShare: (GroupExpenseShareEntity) -> Unit,
    onUnsettleShare: (GroupExpenseShareEntity) -> Unit,
    onShareDong: (GroupExpenseShareEntity) -> Unit,
    onShareAll: () -> Unit
) {
    val tx = expense.transaction
    val title = tx?.note?.ifBlank { "هزینه مشترک" } ?: "هزینه مشترک"
    val dateStr = tx?.date?.let { DateFormatter.formatMedium(it) } ?: ""
    val totalAmount = tx?.amount ?: 0L

    val settledCount = expense.shares.count { it.isSettled }
    val totalCount = expense.shares.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (expense.groupExpense.shareMode == GroupExpenseShareMode.EQUAL) "مساوی" else "دلخواه",
                                fontSize = 11.sp
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onShareAll) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک صورت‌حساب",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Total Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کل هزینه: ${AmountFormatter.formatAmountWithCurrency(totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$settledCount از $totalCount تسویه شد",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (settledCount == totalCount) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Shares list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expense.shares.forEach { share ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (share.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (share.isSettled) IncomeColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = share.personName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = AmountFormatter.formatAmountWithCurrency(share.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (share.isSettled) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Share button for individual dong
                            IconButton(
                                onClick = { onShareDong(share) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "ارسال پیام دنگ",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Status / Settle Action
                            if (share.isSettled) {
                                OutlinedButton(
                                    onClick = { onUnsettleShare(share) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomeColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسویه شده", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onSettleShare(share) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("دریافت دنگ", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDongDialog(
    share: GroupExpenseShareEntity,
    expense: GroupExpenseWithShares?,
    accounts: List<com.example.data.local.entity.AccountEntity>,
    selectedAccountId: Long?,
    createIncomeTx: Boolean,
    onSelectAccount: (Long) -> Unit,
    onToggleCreateIncome: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedAccountDropdown by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = IncomeColor,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "پس گرفتن دنگ",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "دنگ «${share.personName}» را پس گرفتی؟ مبلغ ${AmountFormatter.formatAmountWithCurrency(share.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Option to create income transaction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCreateIncome(!createIncomeTx) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = createIncomeTx,
                        onCheckedChange = { onToggleCreateIncome(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ثبت به عنوان تراکنش درآمد در حساب",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (createIncomeTx) {
                    ExposedDropdownMenuBox(
                        expanded = expandedAccountDropdown,
                        onExpandedChange = { expandedAccountDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "انتخاب حساب...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("واریز به حساب") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAccountDropdown,
                            onDismissRequest = { expandedAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        onSelectAccount(acc.id)
                                        expandedAccountDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
            ) {
                Text("تأیید تسویه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
