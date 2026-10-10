package com.example.ui.screens.accounts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TransactionCard
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.BankLogoBadge
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accountWithBalance = state.accountWithBalance
    val account = accountWithBalance?.account
    val accountColor = account?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "جزئیات حساب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (accountWithBalance == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("اطلاعات حساب یافت نشد.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Balance Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = accountColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val account = accountWithBalance.account
                            BankLogoBadge(
                                bankId = account.logoResName,
                                logoImage = account.logoImage,
                                accountName = account.name,
                                cardNumber = account.cardNumber,
                                size = 52.dp,
                                shapeRadius = 14.dp,
                                fallbackColor = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "موجودی فعلی",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AmountFormatter.format(accountWithBalance.currentBalance),
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (!account.cardNumber.isNullOrBlank() || !account.shabaNumber.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    if (!account.cardNumber.isNullOrBlank()) {
                                        val formattedCard = account.cardNumber.chunked(4).joinToString(" - ").toPersianDigits()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("شماره کارت:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                            Text(formattedCard, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    if (!account.shabaNumber.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("شبا:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                            Text(account.shabaNumber.toPersianDigits(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
                                        }
                                    }
                                    if (!account.cardExpiry.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("انقضا:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                            Text(account.cardExpiry.toPersianDigits(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Stats Cards (Income, Expense, Count)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBox(
                            title = "ورودی کل",
                            amountStr = AmountFormatter.format(state.totalIncome),
                            color = IncomeColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "خروجی کل",
                            amountStr = AmountFormatter.format(state.totalExpense),
                            color = ExpenseColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Transactions List Header
                item {
                    Text(
                        text = "تراکنش‌های این حساب (${state.transactionCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.transactions.isEmpty()) {
                    item {
                        Text(
                            text = "تراکنشی برای این حساب ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.transactions, key = { it.transaction.id }) { tx ->
                        TransactionCard(transactionWithDetails = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    amountStr: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amountStr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
