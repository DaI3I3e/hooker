package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation for Iranian 16-digit debit/credit card numbers.
 * Displays 4-digit groups separated by space with Persian digits (e.g., ۶۰۳۷ ۹۹۷۵ ۱۲۳۴ ۵۶۷۸).
 * The underlying state remains clean raw digits (0-9) without spaces.
 */
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            val c = raw[i]
            val persianChar = when (c) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> c
            }
            out.append(persianChar)
            if ((i + 1) % 4 == 0 && (i + 1) != raw.length && (i + 1) < 16) {
                out.append(' ')
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val spaces = (offset - 1) / 4
                return (offset + spaces).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val fullGroups = offset / 5
                val remainder = offset % 5
                val orig = fullGroups * 4 + remainder
                return orig.coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

/**
 * VisualTransformation for card expiration date formatted as MM/YY with Persian digits (e.g., ۱۲/۰۸).
 * User types only 4 digits (MMYY).
 * Automatic slash '/' is inserted after month (2nd digit).
 */
class CardExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            val c = raw[i]
            val persianChar = when (c) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> c
            }
            out.append(persianChar)
            if (i == 1) {
                out.append('/')
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                return (offset + 1).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                return (offset - 1).coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

/**
 * VisualTransformation for Iranian Shaba number displaying numbers with Persian digits.
 */
class ShabaVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = text.text.toPersianDigits()
        return TransformedText(AnnotatedString(transformed), OffsetMapping.Identity)
    }
}

object CardDisplayUtils {
    /**
     * Formats card number for masked display with strict Left-To-Right Override (\u202D ... \u202C).
     * Visual result: Three groups of asterisks (**** **** ****) on the LEFT,
     * and the last 4 digits (e.g., ۱۲۳۴) on the RIGHT, exactly like standard banking apps.
     */
    fun formatMaskedCard(cardNumber: String?): String {
        if (cardNumber.isNullOrBlank()) return ""
        val clean = cardNumber.filter { it.isDigit() }
        val last4 = clean.takeLast(4).toPersianDigits()
        return "\u202D**** **** **** $last4\u202C"
    }

    /**
     * Formats Iranian Shaba number with Left-To-Right Override (\u202D ... \u202C)
     * and Persian digits, ensuring IR remains on the left and digits follow left-to-right.
     */
    fun formatShaba(shabaNumber: String?): String {
        if (shabaNumber.isNullOrBlank()) return ""
        val formatted = shabaNumber.trim().toPersianDigits()
        return "\u202D$formatted\u202C"
    }

    /**
     * Formats card expiration date for display as MM/YY with Persian digits (e.g., ۱۲/۰۸).
     */
    fun formatExpiryDate(rawExpiry: String?): String {
        if (rawExpiry.isNullOrBlank()) return ""
        val clean = rawExpiry.trim()
        return if (clean.length == 4 && !clean.contains('/')) {
            "${clean.take(2)}/${clean.takeLast(2)}".toPersianDigits()
        } else {
            clean.toPersianDigits()
        }
    }
}
