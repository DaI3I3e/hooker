package com.example.ui.screens.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.cheques.ChequesScreen
import com.example.ui.screens.cheques.ChequesViewModel
import com.example.ui.screens.recurring.RecurringTransactionsScreen
import com.example.ui.screens.recurring.RecurringTransactionsViewModel
import com.example.ui.screens.savings.SavingsGoalsScreen
import com.example.ui.screens.savings.SavingsGoalsViewModel

data class PlanTabItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    recurringViewModel: RecurringTransactionsViewModel,
    chequesViewModel: ChequesViewModel,
    savingsGoalsViewModel: SavingsGoalsViewModel,
    onNavigateToAddRecurring: () -> Unit,
    onNavigateToAddCheque: () -> Unit,
    onNavigateToAddSavingsGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        PlanTabItem(title = "اقساط و دوره‌ای", icon = Icons.Default.Repeat),
        PlanTabItem(title = "چک‌های صیادی", icon = Icons.Default.ReceiptLong),
        PlanTabItem(title = "اهداف پس‌انداز", icon = Icons.Default.Savings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "برنامه‌ها",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    RecurringTransactionsScreen(
                        viewModel = recurringViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddRecurring,
                        showTopBar = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    ChequesScreen(
                        viewModel = chequesViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddCheque,
                        showTopBar = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    SavingsGoalsScreen(
                        viewModel = savingsGoalsViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddSavingsGoal,
                        showTopBar = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
