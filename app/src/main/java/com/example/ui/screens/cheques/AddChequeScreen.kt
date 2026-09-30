package com.example.ui.screens.cheques

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ChequeType
import com.example.ui.components.AmountInput
import com.example.ui.components.JalaliDatePickerDialog
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChequeScreen(
    viewModel: AddChequeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val popularBanks = listOf(
        "ملی", "ملت", "صادرات", "تجارت", "سپه", "پاسارگاد",
        "سامان", "پارسیان", "رسالت", "بلو (سامان)", "آینده",
        "شهر", "کشاورزی", "مسکن", "رفاه", "سینا"
    )

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = state.dueDate,
            onDateSelected = { timestamp ->
                viewModel.setDueDate(timestamp)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت چک صیادی جدید", fontWeight = FontWeight.Bold) },
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
            // Cheque Type Tabs
            TabRow(
                selectedTabIndex = if (state.type == ChequeType.PAYABLE) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.type == ChequeType.PAYABLE,
                    onClick = { viewModel.setType(ChequeType.PAYABLE) },
                    text = { Text("چک پرداختی (صادره توسط من)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.type == ChequeType.RECEIVABLE,
                    onClick = { viewModel.setType(ChequeType.RECEIVABLE) },
                    text = { Text("چک دریافتی (واگذار شده به من)", fontWeight = FontWeight.Bold) }
                )
            }

            // Sayad Number Input (16 digits)
            OutlinedTextField(
                value = state.sayadNumber,
                onValueChange = { viewModel.setSayadNumber(it) },
                label = { Text("شناسه ۱۶ رقمی صیاد یا شماره سریال چک") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Amount Input
            Text("مبلغ چک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AmountInput(
                amount = state.amount,
                onAmountChange = { viewModel.setAmount(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Due Date Picker Field
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تاریخ سررسید چک",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateFormatter.formatJalali(state.dueDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "انتخاب تاریخ سررسید",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bank Name Dropdown
            ExposedDropdownMenuBox(
                expanded = bankDropdownExpanded,
                onExpandedChange = { bankDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "بانک ${state.bankName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("نام بانک صادرکننده") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = bankDropdownExpanded,
                    onDismissRequest = { bankDropdownExpanded = false }
                ) {
                    popularBanks.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text("بانک $bank") },
                            onClick = {
                                viewModel.setBankName(bank)
                                bankDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Party Name Input
            OutlinedTextField(
                value = state.partyName,
                onValueChange = { viewModel.setPartyName(it) },
                label = { Text(if (state.type == ChequeType.PAYABLE) "در وجه (نام دریافت‌کننده چک)" else "صادرکننده (نام پرداخت‌کننده یا مشتری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Linked Account Dropdown (Optional)
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }
                OutlinedTextField(
                    value = selectedAccount?.name ?: "انتخاب حساب بانکی مرتبط...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("حساب بانکی مرتبط (جهت پاس شدن در آینده)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.setAccount(account.id)
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Note Input
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.setNote(it) },
                label = { Text("بابت / شماره فاکتور یا توضیحات (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            // Error Message
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت در دفترچه چک", fontWeight = FontWeight.Bold)
            }
        }
    }
}
