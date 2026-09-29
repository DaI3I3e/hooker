package com.example.ui.screens.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.components.AmountInput
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor

enum class GoalActionType {
    DEPOSIT, // واریز به پس‌انداز
    WITHDRAW // برداشت از پس‌انداز
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDepositWithdrawDialog(
    goal: SavingsGoalEntity,
    currentAmount: Long = 0L,
    accounts: List<AccountEntity>,
    actionType: GoalActionType,
    onConfirm: (amount: Long, accountId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(0L) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDeposit = actionType == GoalActionType.DEPOSIT
    val title = if (isDeposit) "واریز به «${goal.title}»" else "برداشت از «${goal.title}»"
    val buttonColor = if (isDeposit) IncomeColor else ExpenseColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isDeposit) 
                        "مبلغ از حساب انتخابی کسر و به این هدف پس‌انداز اضافه خواهد شد."
                    else 
                        "مبلغ از پس‌انداز کسر و به حساب انتخابی واریز خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Amount Input
                AmountInput(
                    amount = amount,
                    onAmountChange = {
                        amount = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = it }
                ) {
                    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "انتخاب حساب...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isDeposit) "حساب مبدأ (کسر از)" else "حساب مقصد (واریز به)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount <= 0) {
                        errorMessage = "مبلغ باید بیشتر از صفر باشد"
                        return@Button
                    }
                    if (!isDeposit && currentAmount > 0 && amount > currentAmount) {
                        errorMessage = "موجودی این پس‌انداز کافی نیست"
                        return@Button
                    }
                    if (selectedAccountId <= 0L) {
                        errorMessage = "لطفاً یک حساب انتخاب کنید"
                        return@Button
                    }
                    onConfirm(amount, selectedAccountId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(if (isDeposit) "واریز" else "برداشت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
