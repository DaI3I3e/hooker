package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.AmountFormatter
import com.example.util.BankLogoBadge
import com.example.util.DateFormatter

@Composable
fun TransactionCard(
    transactionWithDetails: TransactionWithDetails,
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (tx.type == TransactionType.TRANSFER) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "انتقال",
                        tint = TransferColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(defaultIcon),
                        contentDescription = titleText,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (subtitleText.isNotBlank()) {
                        BankLogoBadge(
                            bankId = transactionWithDetails.accountLogo,
                            accountName = transactionWithDetails.accountName,
                            cardNumber = transactionWithDetails.accountCardNumber,
                            size = 18.dp,
                            shapeRadius = 4.dp
                        )
                    }
                    Text(
                        text = if (tx.note.isNullOrBlank()) subtitleText else "$subtitleText (${tx.note})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$amountPrefix ${AmountFormatter.format(tx.amount, includeCurrency = false)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ریال",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = amountColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateFormatter.formatShort(tx.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
