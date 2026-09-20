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
    object PreConfirm : Screen("pre_confirm?amount={amount}&type={type}&date={date}&bankName={bankName}&accountIdent={accountIdent}&smsHash={smsHash}&note={note}") {
        fun createRoute(
            amount: Long,
            type: String,
            date: Long,
            bankName: String?,
            accountIdent: String?,
            smsHash: String,
            note: String?
        ): String {
            val encBank = java.net.URLEncoder.encode(bankName ?: "", "UTF-8")
            val encIdent = java.net.URLEncoder.encode(accountIdent ?: "", "UTF-8")
            val encHash = java.net.URLEncoder.encode(smsHash, "UTF-8")
            val encNote = java.net.URLEncoder.encode(note ?: "", "UTF-8")
            return "pre_confirm?amount=$amount&type=$type&date=$date&bankName=$encBank&accountIdent=$encIdent&smsHash=$encHash&note=$encNote"
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

