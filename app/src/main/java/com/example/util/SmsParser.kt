package com.example.util

import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionType

data class ParsedSms(
    val bankName: String?,
    val transactionType: TransactionType?,   // EXPENSE or INCOME
    val amount: Long?,                       // amount in Rials (integer)
    val date: Long?,                         // timestamp
    val accountIdentifier: String?,          // account number or last 4 digits of card
    val rawDescription: String?              // transaction type (قبض، حقوق، برداشت و...)
)

object SmsParser {

    /**
     * Parses an SMS message solely using learned patterns stored in the database.
     * If no patterns exist or no pattern matches, returns ParsedSms with null values.
     */
    fun parse(
        smsText: String,
        patterns: List<SmsPatternEntity> = emptyList()
    ): ParsedSms {
        val cleanedText = cleanSharedText(smsText)
        if (cleanedText.isBlank() || patterns.isEmpty()) {
            return ParsedSms(null, null, null, null, null, null)
        }

        val matched = PatternMatcher.match(cleanedText, patterns)
        return matched ?: ParsedSms(null, null, null, null, null, null)
    }

    fun cleanSharedText(text: String): String {
        return PatternExtractor.cleanSharedText(text)
    }
}
