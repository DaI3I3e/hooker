package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.DecryptResult
import com.example.data.backup.RestoreExecutionResult
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
    val isBiometricEnabled: Boolean = false,
    val isSecureScreenEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val pendingRestoreJson: String? = null,
    val pendingRestoreIsLegacy: Boolean = false
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
            lastBackupTimeFormatted = if (settingsRepository.lastBackupTime > 0L) {
                DateFormatter.formatDateTime(settingsRepository.lastBackupTime)
            } else {
                "هرگز"
            },
            isBiometricEnabled = settingsRepository.isBiometricEnabled,
            isSecureScreenEnabled = settingsRepository.isSecureScreenEnabled
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

    suspend fun generateBackupEncryptedBytes(): ByteArray {
        val bytes = backupManager.createEncryptedBackup()
        val now = System.currentTimeMillis()
        settingsRepository.lastBackupTime = now
        _uiState.update {
            it.copy(
                lastBackupTime = now,
                lastBackupTimeFormatted = DateFormatter.formatDateTime(now),
                userMessage = "نسخه پشتیبان رمزنگاری‌شده (AES-256) با موفقیت ایجاد شد",
                isErrorMessage = false
            )
        }
        return bytes
    }

    suspend fun generateBackupEncryptedBase64(): String {
        val base64 = backupManager.createEncryptedBackupBase64()
        val now = System.currentTimeMillis()
        settingsRepository.lastBackupTime = now
        _uiState.update {
            it.copy(
                lastBackupTime = now,
                lastBackupTimeFormatted = DateFormatter.formatDateTime(now),
                userMessage = "متن پشتیبان رمزنگاری‌شده در کلیپ‌بورد کپی شد",
                isErrorMessage = false
            )
        }
        return base64
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

    fun onFileSelectedForRestore(bytes: ByteArray) {
        if (bytes.size > 25_000_000) {
            _uiState.update {
                it.copy(
                    userMessage = "حجم فایل انتخاب‌شده بیش از حد مجاز است.",
                    isErrorMessage = true
                )
            }
            return
        }

        val (decryptResult, isValid) = backupManager.decryptAndValidateBytes(bytes)
        when (decryptResult) {
            is DecryptResult.Error -> {
                _uiState.update {
                    it.copy(
                        userMessage = decryptResult.message,
                        isErrorMessage = true
                    )
                }
            }
            is DecryptResult.LegacyPlainJson -> {
                if (!isValid) {
                    _uiState.update {
                        it.copy(
                            userMessage = "فایل پشتیبان نامعتبر است یا ساختار جیب‌بان را ندارد.",
                            isErrorMessage = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pendingRestoreJson = decryptResult.jsonString,
                            pendingRestoreIsLegacy = true,
                            showRestoreDialog = true,
                            userMessage = "فایل قدیمی (رمزنگاری‌نشده) شناسایی شد."
                        )
                    }
                }
            }
            is DecryptResult.Success -> {
                if (!isValid) {
                    _uiState.update {
                        it.copy(
                            userMessage = "فایل پشتیبان رمزگشایی شد اما ساختار داده‌های آن معتبر نیست.",
                            isErrorMessage = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pendingRestoreJson = decryptResult.jsonString,
                            pendingRestoreIsLegacy = false,
                            showRestoreDialog = true
                        )
                    }
                }
            }
        }
    }

    fun onTextSelectedForRestore(rawText: String) {
        if (rawText.length > 35_000_000) {
            _uiState.update {
                it.copy(
                    userMessage = "حجم متن انتخاب‌شده بیش از حد مجاز است.",
                    isErrorMessage = true
                )
            }
            return
        }

        val (decryptResult, isValid) = backupManager.decryptAndValidateString(rawText)
        when (decryptResult) {
            is DecryptResult.Error -> {
                _uiState.update {
                    it.copy(
                        userMessage = decryptResult.message,
                        isErrorMessage = true
                    )
                }
            }
            is DecryptResult.LegacyPlainJson -> {
                if (!isValid) {
                    _uiState.update {
                        it.copy(
                            userMessage = "متن پشتیبان نامعتبر است یا ساختار جیب‌بان را ندارد.",
                            isErrorMessage = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pendingRestoreJson = decryptResult.jsonString,
                            pendingRestoreIsLegacy = true,
                            showRestoreDialog = true,
                            userMessage = "متن قدیمی (رمزنگاری‌نشده) شناسایی شد."
                        )
                    }
                }
            }
            is DecryptResult.Success -> {
                if (!isValid) {
                    _uiState.update {
                        it.copy(
                            userMessage = "متن پشتیبان رمزگشایی شد اما ساختار داده‌های آن معتبر نیست.",
                            isErrorMessage = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pendingRestoreJson = decryptResult.jsonString,
                            pendingRestoreIsLegacy = false,
                            showRestoreDialog = true
                        )
                    }
                }
            }
        }
    }

    fun onFileSelectedForRestore(jsonString: String) {
        onTextSelectedForRestore(jsonString)
    }

    fun confirmRestore() {
        val jsonString = _uiState.value.pendingRestoreJson ?: return
        val isLegacy = _uiState.value.pendingRestoreIsLegacy
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showRestoreDialog = false) }

            try {
                // 1. Auto backup current state before restoring
                backupManager.createEncryptedBackup()

                // 2. Perform restoration
                val result = backupManager.restoreDecryptedJson(jsonString, isLegacy)
                val now = System.currentTimeMillis()

                when (result) {
                    is RestoreExecutionResult.Success -> {
                        settingsRepository.lastBackupTime = now
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                lastBackupTime = now,
                                lastBackupTimeFormatted = DateFormatter.formatDateTime(now),
                                pendingRestoreJson = null,
                                pendingRestoreIsLegacy = false,
                                userMessage = "${result.message} (پشتیبان‌گیری خودکار انجام شد)",
                                isErrorMessage = false
                            )
                        }
                    }
                    is RestoreExecutionResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingRestoreJson = null,
                                pendingRestoreIsLegacy = false,
                                userMessage = result.message,
                                isErrorMessage = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRestoreJson = null,
                        pendingRestoreIsLegacy = false,
                        userMessage = "خطایی رخ داد: ${e.localizedMessage}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        cancelRestore()
    }

    fun cancelRestore() {
        _uiState.update {
            it.copy(
                showRestoreDialog = false,
                pendingRestoreJson = null,
                pendingRestoreIsLegacy = false
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        settingsRepository.isBiometricEnabled = enabled
        _uiState.update {
            it.copy(
                isBiometricEnabled = enabled,
                userMessage = if (enabled) "قفل بیومتریک فعال شد" else "قفل بیومتریک غیرفعال شد"
            )
        }
    }

    fun setSecureScreenEnabled(enabled: Boolean) {
        settingsRepository.isSecureScreenEnabled = enabled
        _uiState.update {
            it.copy(
                isSecureScreenEnabled = enabled,
                userMessage = if (enabled) "امنیت صفحه نمایش فعال شد" else "امنیت صفحه نمایش غیرفعال شد"
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
        private val onThemeChanged: (String) -> Unit = {}
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, backupManager, accountRepository, onThemeChanged) as T
        }
    }
}
