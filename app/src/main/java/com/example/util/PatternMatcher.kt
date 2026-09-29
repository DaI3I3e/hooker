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

            val rawSavedFields: List<ExtractedField> = try {
                gson.fromJson(pattern.extractedFields, fieldListType)
            } catch (e: Exception) {
                emptyList()
            }
            if (rawSavedFields.isEmpty()) continue

            val savedFields = normalizeSavedFields(rawSavedFields)

            // 1. Structure check: token count must match closely (at most difference of 1)
            if (Math.abs(savedFields.size - currentTokens.size) > 1) continue

            var bankName: String? = pattern.bankName
            var amount: Long? = null
            var txType: TransactionType? = null
            var accountIdent: String? = null
            var dateStr: String? = null
            var yearStr: String? = null
            var monthStr: String? = null
            var dayStr: String? = null
            var timeStr: String? = null
            var hourStr: String? = null
            var minStr: String? = null
            var description: String? = null
            var hasMatchingFailure = false

            // 2. Read each fieldType and map corresponding token
            for (i in savedFields.indices) {
                val savedField = savedFields[i]
                val fieldType = savedField.fieldType ?: FieldType.IGNORE
                if (fieldType == FieldType.IGNORE) continue

                // Find corresponding token in currentTokens at position
                val currentToken = currentTokens.getOrNull(i)
                    ?: currentTokens.getOrNull(i - 1)
                    ?: currentTokens.getOrNull(i + 1)

                if (currentToken == null) {
                    hasMatchingFailure = true
                    break
                }

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
                        } else {
                            // Expected amount here but token is not a number! Structure mismatch
                            hasMatchingFailure = true
                            break
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
                        if (token.contains("+") || token.contains("واریز")) {
                            txType = TransactionType.INCOME
                        } else if (token.contains("-") || token.contains("برداشت")) {
                            txType = TransactionType.EXPENSE
                        }
                    }
                    FieldType.DATE_YEAR -> {
                        yearStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.DATE_MONTH -> {
                        monthStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.DATE_DAY -> {
                        dayStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.TIME_HOUR -> {
                        hourStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.TIME_MINUTE -> {
                        minStr = convertToEnglish(currentToken.text).filter { it.isDigit() }
                    }
                    FieldType.DESCRIPTION -> {
                        description = if (description == null) currentToken.text else "$description ${currentToken.text}"
                    }
                    FieldType.BALANCE -> {}
                    FieldType.IGNORE -> {}
                }
            }

            if (hasMatchingFailure) continue

            val hasDateFields = !dateStr.isNullOrBlank() || !timeStr.isNullOrBlank() ||
                !yearStr.isNullOrBlank() || !monthStr.isNullOrBlank() || !dayStr.isNullOrBlank() ||
                !hourStr.isNullOrBlank() || !minStr.isNullOrBlank()

            // Strict requirements: amount > 0, transaction type non-null, valid date
            if (amount != null && amount > 0L && txType != null && hasDateFields) {
                val timestamp = parseDateTimeToTimestamp(
                    dateStr = dateStr,
                    timeStr = timeStr,
                    hourStr = hourStr,
                    minStr = minStr,
                    yearStr = yearStr,
                    monthStr = monthStr,
                    dayStr = dayStr
                )

                if (timestamp != null) {
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
        }

        return null
    }

    fun normalizeSavedFields(savedFields: List<ExtractedField>): List<ExtractedField> {
        val result = mutableListOf<ExtractedField>()
        var i = 0
        while (i < savedFields.size) {
            val field = savedFields[i]
            val text = field.text.trim()

            // 1. Full combined MMDD-HH:MM or MMDD_HH:MM (e.g. 0706-21:05)
            val fullMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})$""").find(text)
            if (fullMatch != null) {
                val m = fullMatch.groupValues[1]
                val d = fullMatch.groupValues[2]
                val h = fullMatch.groupValues[3]
                val min = fullMatch.groupValues[4]
                val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                val hInt = convertToEnglish(h).toIntOrNull() ?: -1
                val minInt = convertToEnglish(min).toIntOrNull() ?: -1
                if (mInt in 1..12 && dInt in 1..31 && hInt in 0..23 && minInt in 0..59) {
                    result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                    result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                    result.add(ExtractedField(h, field.lineIndex, FieldType.TIME_HOUR))
                    result.add(ExtractedField(min, field.lineIndex, FieldType.TIME_MINUTE))
                    i++
                    continue
                }
            }

            // 2. Partial bug where previous PatternExtractor split around colon:
            // Field i was "0706-21" and field i+1 was "05"
            val partialBugMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2})$""").find(text)
            if (partialBugMatch != null && i + 1 < savedFields.size) {
                val nextField = savedFields[i + 1]
                val nextText = nextField.text.trim()
                if (nextText.matches(Regex("""^[\d۰-۹٠-٩]{2}$"""))) {
                    val m = partialBugMatch.groupValues[1]
                    val d = partialBugMatch.groupValues[2]
                    val h = partialBugMatch.groupValues[3]
                    val min = nextText
                    val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                    val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                    val hInt = convertToEnglish(h).toIntOrNull() ?: -1
                    val minInt = convertToEnglish(min).toIntOrNull() ?: -1
                    if (mInt in 1..12 && dInt in 1..31 && hInt in 0..23 && minInt in 0..59) {
                        result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                        result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                        result.add(ExtractedField(h, field.lineIndex, FieldType.TIME_HOUR))
                        result.add(ExtractedField(min, nextField.lineIndex, FieldType.TIME_MINUTE))
                        i += 2
                        continue
                    }
                }
            }

            // 3. Standalone MMDD (e.g. "0706")
            val mmDdMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})$""").find(text)
            if (mmDdMatch != null && (field.fieldType == FieldType.DATE_MONTH || field.fieldType == FieldType.DATE_DAY || field.fieldType == FieldType.IGNORE)) {
                val m = mmDdMatch.groupValues[1]
                val d = mmDdMatch.groupValues[2]
                val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                if (mInt in 1..12 && dInt in 1..31 && (text.startsWith("0") || text.startsWith("۰") || text.startsWith("٠"))) {
                    result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                    result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                    i++
                    continue
                }
            }

            result.add(field)
            i++
        }
        return result
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
        minStr: String? = null,
        yearStr: String? = null,
        monthStr: String? = null,
        dayStr: String? = null
    ): Long? {
        if (dateStr.isNullOrBlank() && timeStr.isNullOrBlank() && hourStr.isNullOrBlank() && minStr.isNullOrBlank()
            && yearStr.isNullOrBlank() && monthStr.isNullOrBlank() && dayStr.isNullOrBlank()) {
            return null
        }

        try {
            var hour = hourStr?.toIntOrNull() ?: 0
            var min = minStr?.toIntOrNull() ?: 0
            val currentYear = JalaliDate.today().year
            var jYear = currentYear
            var jMonth = 1
            var jDay = 1

            // If explicit year, month, or day provided
            if (!yearStr.isNullOrBlank()) {
                var y = yearStr.toIntOrNull() ?: currentYear
                if (y < 100) y += 1400
                jYear = y
            }
            if (!monthStr.isNullOrBlank()) {
                jMonth = monthStr.toIntOrNull() ?: 1
            }
            if (!dayStr.isNullOrBlank()) {
                jDay = dayStr.toIntOrNull() ?: 1
            }

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

            // If separate year/month/day were not provided, parse from dateStr/combinedStr
            if (yearStr.isNullOrBlank() && monthStr.isNullOrBlank() && dayStr.isNullOrBlank()) {
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
                            // 4. Raw MMDD (e.g. 0525-17:28 or 0706-21:05)
                            val rawMmDdMatch = Regex("""(\d{2})(\d{2})(?:[_\-]\d{1,2}:\d{2})?""").find(eng)
                            if (rawMmDdMatch != null) {
                                val mCandidate = rawMmDdMatch.groupValues[1].toInt()
                                val dCandidate = rawMmDdMatch.groupValues[2].toInt()
                                if (mCandidate in 1..12 && dCandidate in 1..31) {
                                    jMonth = mCandidate
                                    jDay = dCandidate
                                }
                            }
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
            return null
        }
    }
}
