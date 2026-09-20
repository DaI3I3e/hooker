package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.entity.AccountEntity
import com.example.data.repository.AccountRepository
import com.example.data.repository.SettingsRepository
import com.example.util.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val lastBackupTime: Long = 0L,
    val lastBackupTimeFormatted: String = "هرگز",
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val pendingRestoreJson: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val accountRepository: AccountRepository,
    private val onThemeChanged: (String) -> Unit = {}
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsRepository.themeMode,
            lastBackupTime = settingsRepository.lastBackupTime,
            lastBackupTimeFormatted = DateFormatter.formatDateTime(settingsRepository.lastBackupTime)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: String) {
        settingsRepository.themeMode = mode
        onThemeChanged(mode)
        _uiState.update {
            it.copy(
                themeMode = mode,
                userMessage = "پوسته برنامه تغییر یافت"
            )
        }
    }

    suspend fun generateBackupJson(): String {
        val json = backupManager.createBackupJson()
        val now = System.currentTimeMillis()
        settingsRepository.lastBackupTime = now
        _uiState.update {
            it.copy(
                lastBackupTime = now,
                lastBackupTimeFormatted = DateFormatter.formatDateTime(now),
                userMessage = "نسخه پشتیبان با موفقیت ایجاد شد",
                isErrorMessage = false
            )
        }
        return json
    }

    fun onFileSelectedForRestore(jsonString: String) {
        val isValid = backupManager.validateBackupJson(jsonString)
        if (!isValid) {
            _uiState.update {
                it.copy(
                    userMessage = "فایل پشتیبان نامعتبر است یا ساختار فین‌ترک را ندارد.",
                    isErrorMessage = true
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                pendingRestoreJson = jsonString,
                showRestoreDialog = true
            )
        }
    }

    fun confirmRestore() {
        val jsonString = _uiState.value.pendingRestoreJson ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showRestoreDialog = false) }

            try {
                // 1. Auto backup current state before restoring
                backupManager.createBackupJson()

                // 2. Perform restoration
                val success = backupManager.restoreBackupJson(jsonString)
                val now = System.currentTimeMillis()

                if (success) {
                    settingsRepository.lastBackupTime = now
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastBackupTime = now,
                            lastBackupTimeFormatted = DateFormatter.formatDateTime(now),
                            pendingRestoreJson = null,
                            userMessage = "اطلاعات با موفقیت بازیابی شدند (پشتیبان‌گیری خودکار انجام شد)",
                            isErrorMessage = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingRestoreJson = null,
                            userMessage = "خطا در بازیابی اطلاعات",
                            isErrorMessage = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRestoreJson = null,
                        userMessage = "خطایی رخ داد: ${e.localizedMessage}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update {
            it.copy(
                showRestoreDialog = false,
                pendingRestoreJson = null
            )
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val backupManager: BackupManager,
        private val accountRepository: AccountRepository,
        private val onThemeChanged: (String) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, backupManager, accountRepository, onThemeChanged) as T
        }
    }
}
