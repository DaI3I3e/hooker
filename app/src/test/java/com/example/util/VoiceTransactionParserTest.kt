package com.example.util

import com.example.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTransactionParserTest {

    @Test
    fun testSpokenNumberExtraction() {
        assertEquals(83_000L, VoiceTransactionParser.extractAmount("هشتاد و سه هزار"))
        assertEquals(1_200_000L, VoiceTransactionParser.extractAmount("یک میلیون و دویست هزار"))
        assertEquals(1_200_000L, VoiceTransactionParser.extractAmount("یک میلیون و دویست"))
        assertEquals(2_000_000_000L, VoiceTransactionParser.extractAmount("دو میلیارد"))
        assertEquals(750_000L, VoiceTransactionParser.extractAmount("۷۵۰ هزار"))
        assertEquals(50_000L, VoiceTransactionParser.extractAmount("50000"))
        assertEquals(2_500_000L, VoiceTransactionParser.extractAmount("دو میلیون و پانصد هزار"))
    }

    @Test
    fun testParseFullExpenseTransaction() {
        val result = VoiceTransactionParser.parse(
            text = "هزینه پنجاه هزار تومن خوراک",
            categoryNames = listOf("خوراک", "تاکسی", "پوشاک")
        )

        assertEquals(TransactionType.EXPENSE, result.type)
        // 50,000 Toman = 500,000 Rial
        assertEquals(500_000L, result.amount)
        assertEquals("خوراک", result.suggestedCategoryName)
    }

    @Test
    fun testParseIncomeTransaction() {
        val result = VoiceTransactionParser.parse(
            text = "واریز دو میلیون حقوق",
            categoryNames = listOf("حقوق", "پاداش")
        )

        assertEquals(TransactionType.INCOME, result.type)
        // 2,000,000 Toman = 20,000,000 Rial
        assertEquals(20_000_000L, result.amount)
        assertEquals("حقوق", result.suggestedCategoryName)
    }

    @Test
    fun testDirectDigitWithToman() {
        val result = VoiceTransactionParser.parse(
            text = "خرید ۱۵۰ هزار تومان لباس",
            categoryNames = listOf("پوشاک", "لباس")
        )

        assertEquals(TransactionType.EXPENSE, result.type)
        // 150,000 Toman = 1,500,000 Rial
        assertEquals(1_500_000L, result.amount)
        assertEquals("لباس", result.suggestedCategoryName)
    }
}
