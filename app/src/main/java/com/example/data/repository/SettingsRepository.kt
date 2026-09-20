package com.example.data.repository

import com.example.util.Preferences

class SettingsRepository(private val preferences: Preferences) {
    var lastAccountId: Long
        get() = preferences.lastAccountId
        set(value) { preferences.lastAccountId = value }

    var lastCategoryId: Long
        get() = preferences.lastCategoryId
        set(value) { preferences.lastCategoryId = value }

    var themeMode: String
        get() = preferences.themeMode
        set(value) { preferences.themeMode = value }

    var lastBackupTime: Long
        get() = preferences.lastBackupTime
        set(value) { preferences.lastBackupTime = value }
}
