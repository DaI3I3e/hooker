package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity

@Composable
fun CompactFilterBar(
    periodLabel: String,
    isPeriodActive: Boolean,
    onSelectPeriod: (String) -> Unit,
    onRequestCustomDate: () -> Unit,
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onSelectAccount: (Long?) -> Unit,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val accountLabel = if (selectedAccount != null) "حساب: ${selectedAccount.name}" else "همه حساب‌ها"

    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val categoryLabel = if (selectedCategory != null) "دسته: ${selectedCategory.name}" else "همه دسته‌ها"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Period Filter Chip
        Box {
            FilterChip(
                selected = isPeriodActive,
                onClick = { showPeriodMenu = true },
                label = {
                    Text(
                        text = "بازه: $periodLabel",
                        fontWeight = if (isPeriodActive) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            DropdownMenu(
                expanded = showPeriodMenu,
                onDismissRequest = { showPeriodMenu = false }
            ) {
                listOf(
                    "TODAY" to "امروز",
                    "WEEK" to "هفته اخیر",
                    "MONTH" to "ماه جاری",
                    "ALL" to "همه زمان‌ها",
                    "CUSTOM" to "بازه دلخواه..."
                ).forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            showPeriodMenu = false
                            if (key == "CUSTOM") {
                                onRequestCustomDate()
                            } else {
                                onSelectPeriod(key)
                            }
                        }
                    )
                }
            }
        }

        // 2. Account Filter Chip
        Box {
            val isAccActive = selectedAccountId != null
            FilterChip(
                selected = isAccActive,
                onClick = { showAccountMenu = true },
                label = {
                    Text(
                        text = accountLabel,
                        fontWeight = if (isAccActive) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            DropdownMenu(
                expanded = showAccountMenu,
                onDismissRequest = { showAccountMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("همه حساب‌ها", fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = if (selectedAccountId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        showAccountMenu = false
                        onSelectAccount(null)
                    }
                )
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name, fontWeight = if (selectedAccountId == acc.id) FontWeight.Bold else FontWeight.Normal) },
                        trailingIcon = if (selectedAccountId == acc.id) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showAccountMenu = false
                            onSelectAccount(acc.id)
                        }
                    )
                }
            }
        }

        // 3. Category Filter Chip
        Box {
            val isCatActive = selectedCategoryId != null
            FilterChip(
                selected = isCatActive,
                onClick = { showCategoryMenu = true },
                label = {
                    Text(
                        text = categoryLabel,
                        fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            DropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { showCategoryMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("همه دسته‌ها", fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = if (selectedCategoryId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        showCategoryMenu = false
                        onSelectCategory(null)
                    }
                )
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name, fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat.icon),
                                contentDescription = null,
                                tint = Color(cat.color),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (selectedCategoryId == cat.id) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showCategoryMenu = false
                            onSelectCategory(cat.id)
                        }
                    )
                }
            }
        }
    }
}
