package com.example.data.repository

import com.example.util.Preferences

class SettingsRepository(private val preferences: Preferences) {
    var lastAccountId: Long
        get() = preferences.lastAccountId
        set(value) { preferences.lastAccountId = value }

    var lastCategoryId: Long
        get() = preferences.lastCategoryId
        set(value) { preferences.lastCategoryId = value }

    var lastExpenseCategoryId: Long
        get() = preferences.lastExpenseCategoryId
        set(value) { preferences.lastExpenseCategoryId = value }

    var lastIncomeCategoryId: Long
        get() = preferences.lastIncomeCategoryId
        set(value) { preferences.lastIncomeCategoryId = value }

    var themeMode: String
        get() = preferences.themeMode
        set(value) { preferences.themeMode = value }

    var lastBackupTime: Long
        get() = preferences.lastBackupTime
        set(value) { preferences.lastBackupTime = value }

    var isBiometricEnabled: Boolean
        get() = preferences.isBiometricEnabled
        set(value) { preferences.isBiometricEnabled = value }

    var isSecureScreenEnabled: Boolean
        get() = preferences.isSecureScreenEnabled
        set(value) { preferences.isSecureScreenEnabled = value }
}
