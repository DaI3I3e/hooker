package com.example.ui.screens.scan_sms

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SmsPatternRepository
import com.example.data.repository.TransactionRepository
import com.example.util.ParsedSms
import com.example.util.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedSmsItem(
    val id: String,
    val parsedSms: ParsedSms,
    val rawText: String,
    val smsHash: String,
    val date: Long,
    val isRecognized: Boolean = true,
    val isRegistered: Boolean = false
)

data class ScanSmsUiState(
    val selectedDays: Int = 30,
    val isLoading: Boolean = false,
    val scannedList: List<ScannedSmsItem> = emptyList(),
    val hasPermission: Boolean = false,
    val isScanCompleted: Boolean = false,
    val errorMessage: String? = null
)

class ScanSmsViewModel(
    private val transactionRepository: TransactionRepository,
    private val smsPatternRepository: SmsPatternRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanSmsUiState())
    val uiState: StateFlow<ScanSmsUiState> = _uiState.asStateFlow()

    fun updatePermissionState(hasPermission: Boolean) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    fun setSelectedDays(days: Int) {
        _uiState.update { it.copy(selectedDays = days) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun scanSms(context: Context) {
        val days = _uiState.value.selectedDays
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    val existingHashes = transactionRepository.getAllSmsHashesList().toSet()
                    val patterns = try {
                        smsPatternRepository.getActivePatternsList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val scannedResult = mutableListOf<ScannedSmsItem>()

                    val uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf("_id", "body", "date")
                    val startTime = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
                    val selection = "date >= ?"
                    val selectionArgs = arrayOf(startTime.toString())

                    val cursor = context.contentResolver.query(
                        uri, projection, selection, selectionArgs, "date DESC"
                    )

                    cursor?.use { c ->
                        val bodyIndex = c.getColumnIndex("body")
                        val dateIndex = c.getColumnIndex("date")

                        while (c.moveToNext()) {
                            val body = if (bodyIndex != -1) c.getString(bodyIndex) else ""
                            val date = if (dateIndex != -1) c.getLong(dateIndex) else System.currentTimeMillis()

                            if (body.isBlank()) continue

                            // 1. Filter out OTP / Second Passwords / Verification codes
                            if (isOtpSms(body)) continue

                            // 2. Parse SMS with custom learned patterns
                            val parsed = SmsParser.parse(body, patterns)

                            val amt = parsed.amount
                            val isRecognized = (amt != null && amt > 0L)

                            if (isRecognized) {
                                val smsDate = parsed.date ?: date
                                val accountIdent = parsed.accountIdentifier ?: parsed.bankName ?: ""

                                // Calculate hash and check if registered
                                val hash = "${amt}_${smsDate}_${accountIdent}"
                                val isRegistered = existingHashes.contains(hash)

                                // Avoid duplicate within current scan list
                                if (scannedResult.any { it.smsHash == hash }) continue

                                scannedResult.add(
                                    ScannedSmsItem(
                                        id = hash,
                                        parsedSms = parsed.copy(date = smsDate),
                                        rawText = body,
                                        smsHash = hash,
                                        date = smsDate,
                                        isRecognized = true,
                                        isRegistered = isRegistered
                                    )
                                )
                            }
                        }
                    }
                    scannedResult
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scannedList = items,
                        isScanCompleted = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "خطا در اسکن پیامک‌ها: ${e.message}",
                        isScanCompleted = true
                    )
                }
            }
        }
    }

    private fun isPotentialBankSms(text: String): Boolean {
        val keywords = listOf(
            "واریز", "برداشت", "خرید", "انتقال", "مانده", "موجودی",
            "کارت", "حساب", "شاپرک", "شتاب", "پایا", "ساتنا",
            "بانک", "ملت", "ملی", "صادرات", "تجارت", "سپه", "رسالت",
            "کشاورزی", "مسکن", "سامان", "پاسارگاد", "پارسیان", "آینده", "شهر", "دی"
        )
        val matchCount = keywords.count { text.contains(it) }
        return matchCount >= 2
    }

    private fun isOtpSms(text: String): Boolean {
        val otpKeywords = listOf(
            "رمز پویا", "رمز یکبار", "کد تایید", "کد فعالسازی", "رمز اینترنتی",
            "رمز دوم", "کد ورود", "کد احراز", "کد صحت", "کد ثبت نام", "کد تائید"
        )
        return otpKeywords.any { text.contains(it, ignoreCase = true) }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val smsPatternRepository: SmsPatternRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanSmsViewModel(transactionRepository, smsPatternRepository) as T
        }
    }
}
