package com.example.ui.screens.add_transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AmountInput
import com.example.ui.components.CategoryPicker
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.DateFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: EditTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        com.example.ui.components.JalaliDatePickerDialog(
            initialTimestamp = state.dateTimestamp,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                viewModel.setDateTimestamp(timestamp)
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        com.example.ui.components.FinTrackTimePickerDialog(
            initialHour = state.selectedHour,
            initialMinute = state.selectedMinute,
            onTimeSelected = { hour, minute ->
                viewModel.setTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ویرایش تراکنش", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Tabs
            val selectedTabIndex = when (state.type) {
                TransactionType.EXPENSE -> 0
                TransactionType.INCOME -> 1
                TransactionType.TRANSFER -> 2
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setTransactionType(TransactionType.EXPENSE) },
                    text = {
                        Text(
                            "هزینه",
                            fontWeight = FontWeight.Bold,
                            color = if (state.type == TransactionType.EXPENSE) ExpenseColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.setTransactionType(TransactionType.INCOME) },
                    text = {
                        Text(
                            "درآمد",
                            fontWeight = FontWeight.Bold,
                            color = if (state.type == TransactionType.INCOME) IncomeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = state.type == TransactionType.TRANSFER,
                    onClick = { viewModel.setTransactionType(TransactionType.TRANSFER) },
                    text = {
                        Text(
                            "انتقال",
                            fontWeight = FontWeight.Bold,
                            color = if (state.type == TransactionType.TRANSFER) TransferColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            // Amount Input
            Text(
                text = "مبلغ تراکنش",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AmountInput(
                amount = state.amount,
                onAmountChange = { viewModel.setAmount(it) }
            )

            // Category Picker (For EXPENSE / INCOME)
            if (state.type != TransactionType.TRANSFER) {
                Text(
                    text = "دسته‌بندی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                CategoryPicker(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategory?.id,
                    onCategorySelected = { viewModel.setSelectedCategory(it) },
                    modifier = Modifier.height(180.dp)
                )
            }

            // Account Selector
            Text(
                text = if (state.type == TransactionType.TRANSFER) "حساب مبدا" else "حساب",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AccountDropdown(
                accounts = state.accounts,
                selectedAccount = state.selectedAccount,
                onAccountSelected = { viewModel.setSelectedAccount(it) }
            )

            // Destination Account Selector (For TRANSFER)
            if (state.type == TransactionType.TRANSFER) {
                Text(
                    text = "حساب مقصد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AccountDropdown(
                    accounts = state.accounts.filter { it.id != state.selectedAccount?.id },
                    selectedAccount = state.selectedToAccount,
                    onAccountSelected = { viewModel.setSelectedToAccount(it) }
                )
            }

            // Date & Time Selector
            Text(
                text = "تاریخ و ساعت تراکنش",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Selector
                OutlinedCard(
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "تاریخ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateFormatter.formatMedium(state.dateTimestamp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Time Selector
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "ساعت",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateFormatter.formatTime(state.selectedHour, state.selectedMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Optional Note
            Text(
                text = "توضیحات (اختیاری)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.setNote(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("مثال: ویرایش یادداشت...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            val buttonColor = when (state.type) {
                TransactionType.EXPENSE -> ExpenseColor
                TransactionType.INCOME -> IncomeColor
                TransactionType.TRANSFER -> TransferColor
            }

            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "ذخیره")
                Spacer(modifier = Modifier.width(8.dp))
                Text("بروزرسانی تراکنش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
