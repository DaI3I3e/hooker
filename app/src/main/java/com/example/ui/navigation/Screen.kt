package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("add_transaction?type={type}") {
        fun createRoute(type: String = "EXPENSE") = "add_transaction?type=$type"
    }
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object Transfer : Screen("transfer")
    object Accounts : Screen("accounts")
    object AccountDetails : Screen("account_details/{accountId}") {
        fun createRoute(accountId: Long) = "account_details/$accountId"
    }
    object AddAccount : Screen("add_account?accountId={accountId}") {
        fun createRoute(accountId: Long? = null) = if (accountId != null) "add_account?accountId=$accountId" else "add_account"
    }
    object Categories : Screen("categories")
    object More : Screen("more")
    object AddCategory : Screen("add_category?categoryId={categoryId}&type={type}") {
        fun createRoute(categoryId: Long? = null, type: String = "EXPENSE") =
            if (categoryId != null) "add_category?categoryId=$categoryId&type=$type" else "add_category?type=$type"
    }
    object Reports : Screen("reports")
    object Debts : Screen("debts")
    object ImportSms : Screen("import_sms?initialText={initialText}") {
        fun createRoute(initialText: String? = null): String {
            return if (!initialText.isNullOrBlank()) {
                val encoded = java.net.URLEncoder.encode(initialText, "UTF-8")
                "import_sms?initialText=$encoded"
            } else {
                "import_sms"
            }
        }
    }
    object ScanSms : Screen("scan_sms")
    object PreConfirm : Screen("pre_confirm")
    object CategoryDetail : Screen("category_detail/{categoryId}?startDate={startDate}&endDate={endDate}&accountId={accountId}&type={type}") {
        fun createRoute(
            categoryId: Long,
            startDate: Long = 0L,
            endDate: Long = Long.MAX_VALUE,
            accountId: Long? = null,
            type: String = "EXPENSE"
        ): String {
            val accPart = if (accountId != null) "&accountId=$accountId" else ""
            return "category_detail/$categoryId?startDate=$startDate&endDate=$endDate$accPart&type=$type"
        }
    }
    object Settings : Screen("settings")
    object PatternLearner : Screen("pattern_learner?accountId={accountId}&initialText={initialText}") {
        fun createRoute(accountId: Long? = null, initialText: String? = null): String {
            val accPart = if (accountId != null) "accountId=$accountId" else "accountId=-1"
            val textPart = if (!initialText.isNullOrBlank()) {
                "&initialText=" + java.net.URLEncoder.encode(initialText, "UTF-8")
            } else ""
            return "pattern_learner?$accPart$textPart"
        }
    }
}

