package com.example.ui.screens.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.relation.AccountWithBalance
import com.example.ui.components.AccountIconDisplay
import com.example.ui.screens.categories.CategoriesViewModel
import com.example.util.AmountFormatter
import com.example.util.BankLogoBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountsViewModel: AccountsViewModel,
    categoriesViewModel: CategoriesViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    onNavigateToAddCategory: (String) -> Unit,
    onNavigateToEditCategory: (Long, String) -> Unit,
    onNavigateToPatternLearner: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var mainTabSelected by remember { mutableStateOf(0) } // 0: Accounts, 1: Categories
    val accounts by accountsViewModel.accountsWithBalance.collectAsStateWithLifecycle()
    val patterns by accountsViewModel.allPatterns.collectAsStateWithLifecycle()
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("حذف حساب", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا از حذف حساب «${accountToDelete?.name}» اطمینان دارید؟ تراکنش‌های مرتبط حذف نخواهند شد.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let { accountsViewModel.deleteAccount(it) }
                        accountToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت حساب‌ها و دسته‌ها", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (mainTabSelected == 0) {
                        onNavigateToAddAccount()
                    } else {
                        onNavigateToAddCategory("EXPENSE")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (mainTabSelected == 0) "افزودن حساب" else "افزودن دسته‌بندی"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Top Tabs
            TabRow(
                selectedTabIndex = mainTabSelected,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = mainTabSelected == 0,
                    onClick = { mainTabSelected = 0 },
                    text = {
                        Text(
                            text = "حساب‌ها (${accounts.size})",
                            fontWeight = if (mainTabSelected == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = mainTabSelected == 1,
                    onClick = { mainTabSelected = 1 },
                    text = {
                        Text(
                            text = "دسته‌بندی‌ها",
                            fontWeight = if (mainTabSelected == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (mainTabSelected == 0) {
                // Accounts Tab
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هیچ حسابی یافت نشد. جهت افزودن، دکمه + را بزنید.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(accounts, key = { it.account.id }) { item ->
                            val hasPattern = patterns.any { p ->
                                p.bankName.equals(item.account.name, ignoreCase = true) ||
                                (p.accountIdentifier.isNotBlank() && (
                                    item.account.cardNumber?.endsWith(p.accountIdentifier) == true ||
                                    item.account.shabaNumber?.contains(p.accountIdentifier) == true
                                ))
                            }

                            AccountListItem(
                                accountWithBalance = item,
                                hasPattern = hasPattern,
                                onClick = { onNavigateToAccountDetails(item.account.id) },
                                onEdit = { onNavigateToEditAccount(item.account.id) },
                                onDelete = { accountToDelete = item.account },
                                onPatternClick = {
                                    onNavigateToPatternLearner?.invoke(item.account.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // Categories Tab Content
                com.example.ui.screens.categories.CategoriesScreenContent(
                    viewModel = categoriesViewModel,
                    onNavigateToAddCategory = onNavigateToAddCategory,
                    onNavigateToEditCategory = onNavigateToEditCategory
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountListItem(
    accountWithBalance: AccountWithBalance,
    hasPattern: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPatternClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val account = accountWithBalance.account
    val cardColor = Color(account.color)
    var showMenu by remember { mutableStateOf(false) }

    val typeLabel = when (account.type) {
        AccountType.CASH -> "کیف پول / نقدی"
        AccountType.BANK -> "حساب بانکی"
        AccountType.CREDIT_CARD -> "کارت اعتباری"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankLogoBadge(
                bankId = account.logoResName,
                logoImage = account.logoImage,
                accountName = account.name,
                cardNumber = account.cardNumber,
                size = 48.dp,
                shapeRadius = 14.dp,
                fallbackColor = cardColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasPattern) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                .clickable { onPatternClick() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Pattern,
                                    contentDescription = "دارای الگوی پیامک",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "الگو",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AmountFormatter.format(accountWithBalance.currentBalance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${accountWithBalance.transactionCount} تراکنش",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "عملیات")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("ویرایش حساب") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "ویرایش") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("الگوی پیامک") },
                        leadingIcon = { Icon(Icons.Default.Pattern, contentDescription = "الگوی پیامک") },
                        onClick = {
                            showMenu = false
                            onPatternClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
