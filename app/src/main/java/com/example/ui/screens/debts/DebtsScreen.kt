package com.example.ui.screens.debts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import com.example.ui.components.AmountInput
import com.example.ui.components.EmptyState
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بدهی‌ها و طلب‌ها", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "ثبت جدید")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "طلب‌های من",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(state.totalOwedToMe),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "بدهی‌های من",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(state.totalIOwe),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseColor
                        )
                    }
                }
            }

            // Tabs (Owed To Me / I Owe)
            TabRow(
                selectedTabIndex = if (state.selectedTab == DebtType.OWED_TO_ME) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.selectedTab == DebtType.OWED_TO_ME,
                    onClick = { viewModel.setSelectedTab(DebtType.OWED_TO_ME) },
                    text = {
                        Text(
                            "طلب‌های من",
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedTab == DebtType.OWED_TO_ME) IncomeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                Tab(
                    selected = state.selectedTab == DebtType.I_OWE,
                    onClick = { viewModel.setSelectedTab(DebtType.I_OWE) },
                    text = {
                        Text(
                            "بدهی‌های من",
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedTab == DebtType.I_OWE) ExpenseColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            // Debts List
            if (state.debts.isEmpty()) {
                EmptyState(
                    title = if (state.selectedTab == DebtType.OWED_TO_ME) "هیچ طلبی ثبت نشده است" else "هیچ بدهی ثبت نشده است",
                    buttonText = "ثبت موار جدید",
                    onButtonClick = { viewModel.openAddDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.debts, key = { it.id }) { debt ->
                        DebtCardItem(
                            debt = debt,
                            onToggleSettled = { viewModel.toggleSettled(debt) },
                            onDelete = { viewModel.deleteDebt(debt) }
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddDebtDialog(
            state = state,
            onDismiss = { viewModel.closeAddDialog() },
            onTypeChange = { viewModel.setDialogType(it) },
            onPersonNameChange = { viewModel.setPersonName(it) },
            onAmountChange = { viewModel.setAmount(it) },
            onNoteChange = { viewModel.setNote(it) },
            onDueDateChange = { viewModel.setDueDate(it) },
            onSave = { viewModel.saveDebt() }
        )
    }
}

@Composable
fun DebtCardItem(
    debt: DebtEntity,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOwedToMe = debt.type == DebtType.OWED_TO_ME
    val mainColor = if (isOwedToMe) IncomeColor else ExpenseColor

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (debt.isSettled) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(mainColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = mainColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (debt.isSettled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IncomeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "تسویه شده",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = AmountFormatter.format(debt.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else mainColor
                )

                debt.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                debt.dueDate?.let {
                    Text(
                        text = "سررسید: " + DateFormatter.formatShort(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onToggleSettled) {
                Icon(
                    imageVector = if (debt.isSettled) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "تغییر وضعیت تسویه",
                    tint = if (debt.isSettled) IncomeColor else MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    state: DebtsUiState,
    onDismiss: () -> Unit,
    onTypeChange: (DebtType) -> Unit,
    onPersonNameChange: (String) -> Unit,
    onAmountChange: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onSave: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = state.dueDate ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                onDueDateChange(timestamp)
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت طلب / بدهی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onTypeChange(DebtType.OWED_TO_ME) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.dialogType == DebtType.OWED_TO_ME) IncomeColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.dialogType == DebtType.OWED_TO_ME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("طلب من")
                    }

                    Button(
                        onClick = { onTypeChange(DebtType.I_OWE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.dialogType == DebtType.I_OWE) ExpenseColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.dialogType == DebtType.I_OWE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("بدهی من")
                    }
                }

                OutlinedTextField(
                    value = state.personName,
                    onValueChange = onPersonNameChange,
                    label = { Text("نام شخص") },
                    placeholder = { Text("مثال: علی محمدی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountInput(
                    amount = state.amount,
                    onAmountChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text("توضیحات (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.dueDate != null) "سررسید: " + DateFormatter.formatLong(state.dueDate)
                        else "انتخاب تاریخ سررسید (اختیاری)"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = state.personName.isNotBlank() && state.amount > 0L
            ) {
                Text("ثبت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
