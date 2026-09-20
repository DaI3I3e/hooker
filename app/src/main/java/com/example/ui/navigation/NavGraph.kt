package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.FinTrackApp
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountDetailViewModel
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.accounts.AccountsViewModel
import com.example.ui.screens.accounts.AddAccountScreen
import com.example.ui.screens.accounts.AddAccountViewModel
import com.example.ui.screens.add_transaction.AddTransactionScreen
import com.example.ui.screens.add_transaction.AddTransactionViewModel
import com.example.ui.screens.add_transaction.EditTransactionScreen
import com.example.ui.screens.add_transaction.EditTransactionViewModel
import com.example.ui.screens.categories.AddCategoryScreen
import com.example.ui.screens.categories.AddCategoryViewModel
import com.example.ui.screens.categories.CategoriesViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.import_sms.ImportSmsScreen
import com.example.ui.screens.import_sms.ImportSmsViewModel
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.screens.transactions.TransactionsViewModel
import com.example.ui.screens.transfer.TransferScreen
import com.example.ui.screens.transfer.TransferViewModel
import com.example.ui.screens.scan_sms.PreConfirmScreen
import com.example.ui.screens.scan_sms.PreConfirmViewModel
import com.example.ui.screens.scan_sms.ScanSmsScreen
import com.example.ui.screens.scan_sms.ScanSmsViewModel
import com.example.ui.screens.pattern_learner.SmsPatternLearnerScreen
import com.example.ui.screens.pattern_learner.SmsPatternLearnerViewModel
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    app: FinTrackApp,
    initialSharedSmsText: String? = null,
    navigateToScanSms: Boolean = false,
    navController: NavHostController = rememberNavController(),
    onThemeChanged: (String) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showFabBottomSheet by remember { mutableStateOf(false) }
    var isFullscreenMode by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Transactions.route && currentRoute != Screen.Reports.route) {
            isFullscreenMode = false
        }
    }

    LaunchedEffect(initialSharedSmsText) {
        if (!initialSharedSmsText.isNullOrBlank()) {
            navController.navigate(Screen.ImportSms.createRoute(initialSharedSmsText))
        }
    }

    LaunchedEffect(navigateToScanSms) {
        if (navigateToScanSms) {
            navController.navigate(Screen.ScanSms.route)
        }
    }

    val bottomNavRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Reports.route,
        Screen.More.route
    )

    val showBottomBar = (currentRoute in bottomNavRoutes) && !isFullscreenMode

    if (showFabBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "افزودن سریع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Option 1: Import SMS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            showFabBottomSheet = false
                            navController.navigate(Screen.ImportSms.route)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "📥 وارد کردن پیامک",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "استخراج و ثبت خودکار تراکنش از متن پیامک بانک",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 2: Add Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ExpenseColor.copy(alpha = 0.12f))
                        .clickable {
                            showFabBottomSheet = false
                            navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ExpenseColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "💸 ثبت هزینه (دستی)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseColor
                        )
                        Text(
                            text = "ثبت پرداخت یا خرج جدید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 3: Add Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IncomeColor.copy(alpha = 0.12f))
                        .clickable {
                            showFabBottomSheet = false
                            navController.navigate(Screen.AddTransaction.createRoute("INCOME"))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IncomeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "💰 ثبت درآمد (دستی)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = IncomeColor
                        )
                        Text(
                            text = "ثبت حقوق یا دریافتی جدید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // 1. Dashboard
                    val isDashSelected = currentRoute == Screen.Dashboard.route
                    NavigationBarItem(
                        selected = isDashSelected,
                        onClick = {
                            if (currentRoute != Screen.Dashboard.route) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "داشبورد") },
                        label = { Text("داشبورد", fontWeight = if (isDashSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )

                    // 2. Transactions
                    val isTxSelected = currentRoute == Screen.Transactions.route
                    NavigationBarItem(
                        selected = isTxSelected,
                        onClick = {
                            if (currentRoute != Screen.Transactions.route) {
                                navController.navigate(Screen.Transactions.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "تراکنش‌ها") },
                        label = { Text("تراکنش‌ها", fontWeight = if (isTxSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )

                    // 3. Large Blue FAB in Center with label "اقدام"
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            FloatingActionButton(
                                onClick = { showFabBottomSheet = true },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "اقدام",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "اقدام",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // 4. Reports
                    val isRepSelected = currentRoute == Screen.Reports.route
                    NavigationBarItem(
                        selected = isRepSelected,
                        onClick = {
                            if (currentRoute != Screen.Reports.route) {
                                navController.navigate(Screen.Reports.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "گزارش‌ها") },
                        label = { Text("گزارش‌ها", fontWeight = if (isRepSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )

                    // 5. More
                    val isMoreSelected = currentRoute == Screen.More.route
                    NavigationBarItem(
                        selected = isMoreSelected,
                        onClick = {
                            if (currentRoute != Screen.More.route) {
                                navController.navigate(Screen.More.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(androidx.compose.material.icons.Icons.Default.MoreHoriz, contentDescription = "بیشتر") },
                        label = { Text("بیشتر", fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(app.appModule.getDashboardDataUseCase)
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction.createRoute(type))
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route)
                    },
                    onNavigateToDebts = {
                        navController.navigate(Screen.Debts.route)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onNavigateToScanSms = {
                        navController.navigate(Screen.ScanSms.route)
                    },
                    onNavigateToImportSms = {
                        navController.navigate(Screen.ImportSms.route)
                    },
                    onNavigateToTransfer = {
                        navController.navigate(Screen.Transfer.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(
                route = Screen.ImportSms.route,
                arguments = listOf(
                    navArgument("initialText") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialText = backStackEntry.arguments?.getString("initialText")
                val viewModel: ImportSmsViewModel = viewModel(
                    factory = ImportSmsViewModel.Factory(
                        accountRepository = app.appModule.accountRepository,
                        categoryRepository = app.appModule.categoryRepository,
                        smsPatternRepository = app.appModule.smsPatternRepository,
                        addTransactionUseCase = app.appModule.addTransactionUseCase
                    )
                )
                ImportSmsScreen(
                    viewModel = viewModel,
                    initialText = initialText,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ScanSms.route) {
                val viewModel: ScanSmsViewModel = viewModel(
                    factory = ScanSmsViewModel.Factory(
                        transactionRepository = app.appModule.transactionRepository,
                        smsPatternRepository = app.appModule.smsPatternRepository
                    )
                )
                ScanSmsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreConfirm = { amt, type, date, bank, ident, hash, note ->
                        navController.navigate(
                            Screen.PreConfirm.createRoute(amt, type, date, bank, ident, hash, note)
                        )
                    },
                    onNavigateToPatternLearnerWithText = { text ->
                        navController.navigate(Screen.PatternLearner.createRoute(null, text))
                    }
                )
            }

            composable(
                route = Screen.PreConfirm.route,
                arguments = listOf(
                    navArgument("amount") { type = NavType.LongType },
                    navArgument("type") { type = NavType.StringType },
                    navArgument("date") { type = NavType.LongType },
                    navArgument("bankName") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("accountIdent") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("smsHash") { type = NavType.StringType },
                    navArgument("note") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStackEntry ->
                val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                val bankName = backStackEntry.arguments?.getString("bankName")
                val accountIdent = backStackEntry.arguments?.getString("accountIdent")
                val smsHash = backStackEntry.arguments?.getString("smsHash") ?: ""
                val note = backStackEntry.arguments?.getString("note")

                val viewModel: PreConfirmViewModel = viewModel(
                    factory = PreConfirmViewModel.Factory(
                        accountRepository = app.appModule.accountRepository,
                        categoryRepository = app.appModule.categoryRepository,
                        addTransactionUseCase = app.appModule.addTransactionUseCase
                    )
                )

                PreConfirmScreen(
                    viewModel = viewModel,
                    amount = amount,
                    typeStr = type,
                    date = date,
                    bankName = bankName,
                    accountIdent = accountIdent,
                    smsHash = smsHash,
                    note = note,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                val viewModel: AddTransactionViewModel = viewModel(
                    factory = AddTransactionViewModel.Factory(
                        initialTypeString = type,
                        accountRepository = app.appModule.accountRepository,
                        categoryRepository = app.appModule.categoryRepository,
                        settingsRepository = app.appModule.settingsRepository,
                        addTransactionUseCase = app.appModule.addTransactionUseCase
                    )
                )
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel(
                    factory = TransactionsViewModel.Factory(
                        transactionRepository = app.appModule.transactionRepository,
                        accountRepository = app.appModule.accountRepository
                    )
                )
                TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateToEditTransaction = { transactionId ->
                        navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                    },
                    onToggleFullscreen = { isFullscreenMode = it }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
                val viewModel: EditTransactionViewModel = viewModel(
                    factory = EditTransactionViewModel.Factory(
                        transactionId = transactionId,
                        transactionRepository = app.appModule.transactionRepository,
                        accountRepository = app.appModule.accountRepository,
                        categoryRepository = app.appModule.categoryRepository
                    )
                )
                EditTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Transfer.route) {
                val viewModel: TransferViewModel = viewModel(
                    factory = TransferViewModel.Factory(
                        accountRepository = app.appModule.accountRepository,
                        settingsRepository = app.appModule.settingsRepository,
                        addTransactionUseCase = app.appModule.addTransactionUseCase
                    )
                )
                TransferScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reports.route) {
                val viewModel: ReportsViewModel = viewModel(
                    factory = ReportsViewModel.Factory(
                        transactionRepository = app.appModule.transactionRepository,
                        accountRepository = app.appModule.accountRepository
                    )
                )
                ReportsScreen(
                    viewModel = viewModel,
                    onToggleFullscreen = { isFullscreenMode = it }
                )
            }

            composable(Screen.Debts.route) {
                val viewModel: com.example.ui.screens.debts.DebtsViewModel = viewModel(
                    factory = com.example.ui.screens.debts.DebtsViewModel.Factory(
                        debtRepository = app.appModule.debtRepository
                    )
                )
                com.example.ui.screens.debts.DebtsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.More.route) {
                com.example.ui.screens.more.MoreScreen(
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Accounts.route) {
                val accountsViewModel: AccountsViewModel = viewModel(
                    factory = AccountsViewModel.Factory(
                        accountRepository = app.appModule.accountRepository,
                        smsPatternRepository = app.appModule.smsPatternRepository
                    )
                )
                val categoriesViewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModel.Factory(app.appModule.categoryRepository)
                )
                AccountsScreen(
                    accountsViewModel = accountsViewModel,
                    categoriesViewModel = categoriesViewModel,
                    onNavigateToAddAccount = {
                        navController.navigate(Screen.AddAccount.createRoute(null))
                    },
                    onNavigateToEditAccount = { accountId ->
                        navController.navigate(Screen.AddAccount.createRoute(accountId))
                    },
                    onNavigateToAccountDetails = { accountId ->
                        navController.navigate(Screen.AccountDetails.createRoute(accountId))
                    },
                    onNavigateToAddCategory = { type ->
                        navController.navigate(Screen.AddCategory.createRoute(null, type))
                    },
                    onNavigateToEditCategory = { categoryId, type ->
                        navController.navigate(Screen.AddCategory.createRoute(categoryId, type))
                    },
                    onNavigateToPatternLearner = { accountId ->
                        navController.navigate(Screen.PatternLearner.createRoute(accountId))
                    }
                )
            }

            composable(
                route = Screen.AddAccount.route,
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val argId = backStackEntry.arguments?.getLong("accountId") ?: -1L
                val accountId = if (argId > 0) argId else null
                val viewModel: AddAccountViewModel = viewModel(
                    factory = AddAccountViewModel.Factory(accountId, app.appModule.accountRepository)
                )
                AddAccountScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPatternLearner = { accId ->
                        navController.navigate(Screen.PatternLearner.createRoute(accId))
                    }
                )
            }

            composable(
                route = Screen.AccountDetails.route,
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
                val viewModel: AccountDetailViewModel = viewModel(
                    factory = AccountDetailViewModel.Factory(
                        accountId = accountId,
                        accountRepository = app.appModule.accountRepository,
                        transactionRepository = app.appModule.transactionRepository
                    )
                )
                AccountDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                val viewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModel.Factory(app.appModule.categoryRepository)
                )
                com.example.ui.screens.categories.CategoriesScreenContent(
                    viewModel = viewModel,
                    onNavigateToAddCategory = { type: String ->
                        navController.navigate(Screen.AddCategory.createRoute(null, type))
                    },
                    onNavigateToEditCategory = { categoryId: Long, type: String ->
                        navController.navigate(Screen.AddCategory.createRoute(categoryId, type))
                    }
                )
            }

            composable(
                route = Screen.AddCategory.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    }
                )
            ) { backStackEntry ->
                val argId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
                val categoryId = if (argId > 0) argId else null
                val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                val viewModel: AddCategoryViewModel = viewModel(
                    factory = AddCategoryViewModel.Factory(categoryId, type, app.appModule.categoryRepository)
                )
                AddCategoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsRepository = app.appModule.settingsRepository,
                        backupManager = app.appModule.backupManager,
                        accountRepository = app.appModule.accountRepository,
                        onThemeChanged = onThemeChanged
                    )
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToScanSms = {
                        navController.navigate(Screen.ScanSms.route)
                    },
                    onNavigateToPatternLearner = { accountId ->
                        navController.navigate(Screen.PatternLearner.createRoute(accountId))
                    }
                )
            }

            composable(
                route = Screen.PatternLearner.route,
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("initialText") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val argId = backStackEntry.arguments?.getLong("accountId") ?: -1L
                val accountId = if (argId > 0) argId else null
                val initialText = backStackEntry.arguments?.getString("initialText")
                val viewModel: SmsPatternLearnerViewModel = viewModel(
                    factory = SmsPatternLearnerViewModel.Factory(
                        smsPatternRepository = app.appModule.smsPatternRepository,
                        accountRepository = app.appModule.accountRepository,
                        accountId = accountId,
                        initialSampleText = initialText
                    )
                )
                SmsPatternLearnerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}


@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
