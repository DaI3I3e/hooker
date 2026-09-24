package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.screens.transactions.DateFilterPeriod
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    period: DateFilterPeriod,
    customStart: Long?,
    customEnd: Long?,
    accountId: Long?,
    categoryIds: Set<Long>,
    transactionType: TransactionType?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    allowTransfer: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (
        period: DateFilterPeriod,
        customStart: Long?,
        customEnd: Long?,
        accountId: Long?,
        categoryIds: Set<Long>,
        type: TransactionType?
    ) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempPeriod by remember { mutableStateOf(period) }
    var tempCustomStart by remember { mutableStateOf(customStart) }
    var tempCustomEnd by remember { mutableStateOf(customEnd) }
    var tempAccountId by remember { mutableStateOf(accountId) }
    var tempCategoryIds by remember { mutableStateOf(categoryIds) }
    var tempType by remember { mutableStateOf(transactionType) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        JalaliDateRangePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onRangeSelected = { start, end ->
                tempCustomStart = start
                tempCustomEnd = end
                tempPeriod = DateFilterPeriod.CUSTOM
                showCustomDatePicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فیلترها",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. بازه زمانی
            Text(
                text = "بازه زمانی",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempPeriod == DateFilterPeriod.ALL,
                    onClick = { tempPeriod = DateFilterPeriod.ALL },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = tempPeriod == DateFilterPeriod.TODAY,
                    onClick = { tempPeriod = DateFilterPeriod.TODAY },
                    label = { Text("امروز") }
                )
                FilterChip(
                    selected = tempPeriod == DateFilterPeriod.WEEK,
                    onClick = { tempPeriod = DateFilterPeriod.WEEK },
                    label = { Text("این هفته") }
                )
                FilterChip(
                    selected = tempPeriod == DateFilterPeriod.MONTH,
                    onClick = { tempPeriod = DateFilterPeriod.MONTH },
                    label = { Text("این ماه") }
                )
                FilterChip(
                    selected = tempPeriod == DateFilterPeriod.CUSTOM,
                    onClick = { showCustomDatePicker = true },
                    label = { Text("بازه دلخواه") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            if (tempPeriod == DateFilterPeriod.CUSTOM && tempCustomStart != null && tempCustomEnd != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "از ${DateFormatter.formatShort(tempCustomStart!!)} تا ${DateFormatter.formatShort(tempCustomEnd!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. حساب
            Text(
                text = "حساب",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempAccountId == null,
                    onClick = { tempAccountId = null },
                    label = { Text("همه حساب‌ها") }
                )
                accounts.forEach { acc ->
                    FilterChip(
                        selected = tempAccountId == acc.id,
                        onClick = { tempAccountId = acc.id },
                        label = { Text(acc.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. نوع تراکنش
            Text(
                text = "نوع تراکنش",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempType == null,
                    onClick = { tempType = null },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = tempType == TransactionType.EXPENSE,
                    onClick = { tempType = TransactionType.EXPENSE },
                    label = { Text("هزینه") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseColor.copy(alpha = 0.2f),
                        selectedLabelColor = ExpenseColor
                    )
                )
                FilterChip(
                    selected = tempType == TransactionType.INCOME,
                    onClick = { tempType = TransactionType.INCOME },
                    label = { Text("درآمد") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeColor.copy(alpha = 0.2f),
                        selectedLabelColor = IncomeColor
                    )
                )
                if (allowTransfer) {
                    FilterChip(
                        selected = tempType == TransactionType.TRANSFER,
                        onClick = { tempType = TransactionType.TRANSFER },
                        label = { Text("انتقال") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TransferColor.copy(alpha = 0.2f),
                            selectedLabelColor = TransferColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. دسته‌بندی (چند انتخابی)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسته‌بندی‌ها (چند انتخابی)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(
                    onClick = {
                        tempCategoryIds = emptySet()
                    }
                ) {
                    Text(
                        text = if (tempCategoryIds.isEmpty()) "همه انتخاب شده‌اند" else "انتخاب همه",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Categories list with checkboxes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    val isChecked = tempCategoryIds.isEmpty() || tempCategoryIds.contains(cat.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (tempCategoryIds.isEmpty()) {
                                    // Currently all are selected; clicking this one deselects it
                                    tempCategoryIds = categories.map { it.id }.toSet() - cat.id
                                } else {
                                    val next = if (tempCategoryIds.contains(cat.id)) {
                                        tempCategoryIds - cat.id
                                    } else {
                                        tempCategoryIds + cat.id
                                    }
                                    tempCategoryIds = if (next.size == categories.size) emptySet() else next
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (tempCategoryIds.isEmpty()) {
                                    if (!checked) {
                                        tempCategoryIds = categories.map { it.id }.toSet() - cat.id
                                    }
                                } else {
                                    val next = if (checked) {
                                        tempCategoryIds + cat.id
                                    } else {
                                        tempCategoryIds - cat.id
                                    }
                                    tempCategoryIds = if (next.size == categories.size) emptySet() else next
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(cat.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat.icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions: اعمال فیلتر | حذف فیلترها
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempPeriod = DateFilterPeriod.ALL
                        tempCustomStart = null
                        tempCustomEnd = null
                        tempAccountId = null
                        tempCategoryIds = emptySet()
                        tempType = null
                        onReset()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف فیلترها")
                }

                Button(
                    onClick = {
                        onApply(
                            tempPeriod,
                            tempCustomStart,
                            tempCustomEnd,
                            tempAccountId,
                            tempCategoryIds,
                            tempType
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("اعمال فیلتر")
                }
            }
        }
    }
}
