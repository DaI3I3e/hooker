package com.example.util

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var lastAccountId: Long
        get() = prefs.getLong(Constants.KEY_LAST_ACCOUNT_ID, Constants.DEFAULT_ACCOUNT_ID)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_ACCOUNT_ID, value).apply()

    var lastCategoryId: Long
        get() = prefs.getLong(Constants.KEY_LAST_CATEGORY_ID, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_CATEGORY_ID, value).apply()

    var lastExpenseCategoryId: Long
        get() = prefs.getLong(Constants.KEY_LAST_EXPENSE_CATEGORY_ID, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_EXPENSE_CATEGORY_ID, value).apply()

    var lastIncomeCategoryId: Long
        get() = prefs.getLong(Constants.KEY_LAST_INCOME_CATEGORY_ID, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_INCOME_CATEGORY_ID, value).apply()

    var themeMode: String
        get() = prefs.getString(Constants.KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(Constants.KEY_THEME_MODE, value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong(Constants.KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_BACKUP_TIME, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_BIOMETRIC_ENABLED, value).apply()

    var isSecureScreenEnabled: Boolean
        get() = prefs.getBoolean(Constants.KEY_SECURE_SCREEN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.KEY_SECURE_SCREEN_ENABLED, value).apply()
}
