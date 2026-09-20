package com.example.ui.screens.scan_sms

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionType
import com.example.ui.components.CategoryPicker
import com.example.ui.components.FinTrackTimePickerDialog
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.BankLogoBadge
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreConfirmScreen(
    viewModel: PreConfirmViewModel,
    amount: Long,
    typeStr: String,
    date: Long,
    bankName: String?,
    accountIdent: String?,
    smsHash: String,
    note: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val (currentHour, currentMinute) = remember(state.dateTimestamp) {
        DateFormatter.extractHourAndMinute(state.dateTimestamp)
    }

    LaunchedEffect(smsHash) {
        viewModel.initData(amount, typeStr, date, bankName, accountIdent, smsHash, note)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            onNavigateBack()
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = state.dateTimestamp,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                val newTimestamp = DateFormatter.combineDateAndTime(timestamp, currentHour, currentMinute)
                viewModel.setDateTimestamp(newTimestamp)
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        FinTrackTimePickerDialog(
            initialHour = currentHour,
            initialMinute = currentMinute,
            onTimeSelected = { h, m ->
                val newTimestamp = DateFormatter.combineDateAndTime(state.dateTimestamp, h, m)
                viewModel.setDateTimestamp(newTimestamp)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(state.categories) {
        if (state.categories.isNotEmpty()) {
            listState.animateScrollToItem(2)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تأیید و ثبت تراکنش", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Summary Card
            item {
                val isExpense = state.transactionType != TransactionType.INCOME
                val typeText = if (isExpense) "هزینه" else "درآمد"
                val typeColor = if (isExpense) ExpenseColor else IncomeColor

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.bankName ?: "اطلاعات پیامک",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(typeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = typeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Expense / Income Toggle Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.setTransactionType(TransactionType.EXPENSE) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.transactionType == TransactionType.EXPENSE) ExpenseColor else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (state.transactionType == TransactionType.EXPENSE) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("هزینه (برداشت)")
                            }

                            Button(
                                onClick = { viewModel.setTransactionType(TransactionType.INCOME) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.transactionType == TransactionType.INCOME) IncomeColor else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (state.transactionType == TransactionType.INCOME) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("درآمد (واریز)")
                            }
                        }

                        Text(
                            text = AmountFormatter.formatRial(state.amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )

                        // Date and Time Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateFormatter.formatMedium(state.dateTimestamp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Time
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateFormatter.formatTime(currentHour, currentMinute),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Account Selector Card
            item {
                var expanded by remember { mutableStateOf(false) }
                val selectedAccount = state.accounts.find { it.id == state.selectedAccountId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "انتخاب حساب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedAccount?.name ?: "انتخاب حساب...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("حساب واریز/برداشت") },
                                leadingIcon = {
                                    if (selectedAccount != null) {
                                        BankLogoBadge(
                                            bankId = selectedAccount.logoResName,
                                            accountName = selectedAccount.name,
                                            cardNumber = selectedAccount.cardNumber,
                                            size = 24.dp,
                                            shapeRadius = 6.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null
                                        )
                                    }
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name) },
                                        leadingIcon = {
                                            BankLogoBadge(
                                                bankId = acc.logoResName,
                                                accountName = acc.name,
                                                cardNumber = acc.cardNumber,
                                                size = 22.dp,
                                                shapeRadius = 6.dp
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSelectedAccount(acc.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Picker Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "دسته تراکنش (الزامی)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        CategoryPicker(
                            categories = state.categories,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelected = { category -> viewModel.setSelectedCategory(category.id) }
                        )
                    }
                }
            }

            // Note Text Field Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "توضیحات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = state.note,
                            onValueChange = { viewModel.setNote(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("توضیحات اختیاری...") },
                            singleLine = true
                        )
                    }
                }
            }

            // Save Action Button
            item {
                Button(
                    onClick = { viewModel.saveTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ثبت نهایی تراکنش",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
