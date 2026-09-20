package com.example.util

import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.ZoneId

object PatternMatcher {

    private val gson = Gson()
    private val fieldListType = object : TypeToken<List<ExtractedField>>() {}.type

    fun match(smsText: String, patterns: List<SmsPatternEntity>): ParsedSms? {
        if (smsText.isBlank() || patterns.isEmpty()) return null

        val currentTokens = PatternExtractor.extractParts(smsText)
        if (currentTokens.isEmpty()) return null

        for (pattern in patterns) {
            if (!pattern.isActive) continue

            val savedFields: List<ExtractedField> = try {
                gson.fromJson(pattern.extractedFields, fieldListType)
            } catch (e: Exception) {
                emptyList()
            }
            if (savedFields.isEmpty()) continue

            // 1. Structure check: token count within +/- 3
            if (Math.abs(savedFields.size - currentTokens.size) > 3) continue

            var bankName: String? = pattern.bankName
            var amount: Long? = null
            var txType: TransactionType? = null
            var accountIdent: String? = null
            var dateStr: String? = null
            var timeStr: String? = null
            var hourStr: String? = null
            var minStr: String? = null
            var description: String? = null

            // 2. Read each fieldType and map corresponding token
            for (i in savedFields.indices) {
                val savedField = savedFields[i]
                val fieldType = savedField.fieldType ?: FieldType.IGNORE
                if (fieldType == FieldType.IGNORE) continue

                // Find corresponding token in currentTokens at position +/- 1
                val currentToken = currentTokens.getOrNull(i)
                    ?: currentTokens.getOrNull(i - 1)
                    ?: currentTokens.getOrNull(i + 1)
                    ?: continue

                when (fieldType) {
                    FieldType.BANK_NAME -> {
                        bankName = currentToken.text
                    }
                    FieldType.ACCOUNT_IDENTIFIER -> {
                        accountIdent = convertToEnglish(currentToken.text).replace(Regex("""[^\d\.]"""), "")
                    }
                    FieldType.AMOUNT -> {
                        val cleanedNum = convertToEnglish(currentToken.text)
                            .replace(",", "")
                            .replace("،", "")
                            .replace(".", "")
                            .replace(Regex("""[^\d]"""), "")
                        val parsedAmt = cleanedNum.toLongOrNull()
                        if (parsedAmt != null && parsedAmt > 0L) {
                            amount = parsedAmt
                        }
                    }
                    FieldType.TRANSACTION_TYPE -> {
                        val token = currentToken.text
                        if (token.contains("+") || token.contains("واریز") || token.contains("حقوق")) {
                            txType = TransactionType.INCOME
                        } else if (token.contains("-") || token.contains("برداشت") || token.contains("خرید") || token.contains("قبض") || token.contains("انتقال")) {
                            txType = TransactionType.EXPENSE
                        }
                    }
                    FieldType.SIGN -> {
                        val token = currentToken.text
                        if (token.contains("+")) {
                            txType = TransactionType.INCOME
                        } else if (token.contains("-")) {
                            txType = TransactionType.EXPENSE
                        }
                    }
                    FieldType.DATE -> {
                        val eng = convertToEnglish(currentToken.text)
                        dateStr = if (dateStr == null) eng else "$dateStr/$eng"
                    }
                    FieldType.TIME_HOUR -> {
                        hourStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.TIME_MINUTE -> {
                        minStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.TIME -> {
                        val eng = convertToEnglish(currentToken.text)
                        timeStr = if (timeStr == null) eng else "$timeStr:$eng"
                    }
                    FieldType.DESCRIPTION -> {
                        description = if (description == null) currentToken.text else "$description ${currentToken.text}"
                    }
                    FieldType.BALANCE -> {}
                    FieldType.IGNORE -> {}
                }
            }

            if (amount != null && amount > 0L) {
                if (txType == null) {
                    val englishSms = convertToEnglish(smsText)
                    txType = if (englishSms.contains("واریز") || englishSms.contains("حقوق") || englishSms.contains("+")) {
                        TransactionType.INCOME
                    } else {
                        TransactionType.EXPENSE
                    }
                }

                val timestamp = parseDateTimeToTimestamp(dateStr, timeStr, hourStr, minStr)

                return ParsedSms(
                    bankName = bankName ?: pattern.bankName ?: "بانک",
                    transactionType = txType,
                    amount = amount,
                    date = timestamp,
                    accountIdentifier = accountIdent ?: pattern.accountIdentifier.ifBlank { null },
                    rawDescription = description ?: (if (txType == TransactionType.INCOME) "واریز" else "برداشت")
                )
            }
        }

        return null
    }

    private fun convertToEnglish(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i)).replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    fun parseDateTimeToTimestamp(
        dateStr: String?,
        timeStr: String?,
        hourStr: String? = null,
        minStr: String? = null
    ): Long {
        if (dateStr.isNullOrBlank() && timeStr.isNullOrBlank() && hourStr.isNullOrBlank() && minStr.isNullOrBlank()) {
            return System.currentTimeMillis()
        }

        try {
            var hour = hourStr?.toIntOrNull() ?: 0
            var min = minStr?.toIntOrNull() ?: 0
            val currentYear = JalaliDate.today().year
            var jYear = currentYear
            var jMonth = 1
            var jDay = 1

            val combinedStr = "${dateStr.orEmpty()} ${timeStr.orEmpty()}".trim()
            val eng = convertToEnglish(combinedStr)

            // Extract time (HH:MM or HH:MM:SS) if not already explicitly provided
            if (hourStr == null || minStr == null) {
                val timeMatch = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""").find(eng)
                if (timeMatch != null) {
                    if (hourStr == null) hour = timeMatch.groupValues[1].toIntOrNull() ?: 0
                    if (minStr == null) min = timeMatch.groupValues[2].toIntOrNull() ?: 0
                }
            }

            // 1. Full 4-digit year: YYYY/MM/DD or YYYY-MM-DD
            val fullDateMatch = Regex("""(\d{4})[/\-](\d{1,2})[/\-](\d{1,2})""").find(eng)
            if (fullDateMatch != null) {
                jYear = fullDateMatch.groupValues[1].toInt()
                jMonth = fullDateMatch.groupValues[2].toInt()
                jDay = fullDateMatch.groupValues[3].toInt()
            } else {
                // 2. 2-digit year: YY/MM/DD or YY-MM-DD
                val shortYearMatch = Regex("""(\d{2})[/\-](\d{2})[/\-](\d{2})""").find(eng)
                if (shortYearMatch != null) {
                    var yy = shortYearMatch.groupValues[1].toInt()
                    if (yy < 100) yy += 1400
                    jYear = yy
                    jMonth = shortYearMatch.groupValues[2].toInt()
                    jDay = shortYearMatch.groupValues[3].toInt()
                } else {
                    // 3. Month and Day: MM/DD or MM-DD
                    val monthDayMatch = Regex("""(\d{1,2})[/\-](\d{1,2})""").find(eng)
                    if (monthDayMatch != null) {
                        jMonth = monthDayMatch.groupValues[1].toInt()
                        jDay = monthDayMatch.groupValues[2].toInt()
                    } else {
                        // 4. Raw MMDD (e.g. 0525-17:28)
                        val rawMmDdMatch = Regex("""(\d{2})(\d{2})(?:[_\-]\d{1,2}:\d{2})?""").find(eng)
                        if (rawMmDdMatch != null) {
                            jMonth = rawMmDdMatch.groupValues[1].toInt()
                            jDay = rawMmDdMatch.groupValues[2].toInt()
                        }
                    }
                }
            }

            val (gY, gM, gD) = JalaliDate.jalaliToGregorian(jYear, jMonth.coerceIn(1, 12), jDay.coerceIn(1, 31))
            return LocalDateTime.of(gY, gM, gD, hour.coerceIn(0, 23), min.coerceIn(0, 59))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            return System.currentTimeMillis()
        }
    }
}
