package com.example.util

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTransactionParserTest {

    private val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "خوراک", icon = "restaurant", color = 0xFF4CAF50.toInt(), type = CategoryType.EXPENSE),
        CategoryEntity(id = 2L, name = "حمل و نقل", icon = "directions_car", color = 0xFF2196F3.toInt(), type = CategoryType.EXPENSE),
        CategoryEntity(id = 3L, name = "سلامت", icon = "local_hospital", color = 0xFFE91E63.toInt(), type = CategoryType.EXPENSE),
        CategoryEntity(id = 4L, name = "حقوق", icon = "payments", color = 0xFF8BC34A.toInt(), type = CategoryType.INCOME),
        CategoryEntity(id = 5L, name = "مسکن", icon = "home", color = 0xFFFF9800.toInt(), type = CategoryType.EXPENSE)
    )

    @Test
    fun testPersianWordNumbers() {
        // "هشتاد و سه هزار" = 83,000 Toman = 830,000 Rial
        val parsed83k = VoiceTransactionParser.parse("هشتاد و سه هزار", sampleCategories)
        assertEquals(83_000L, parsed83k.tomanAmount)
        assertEquals(830_000L, parsed83k.amountRial)

        // "یک میلیون و دویست" = 1,200,000 Toman = 12,000,000 Rial
        val parsed1m200 = VoiceTransactionParser.parse("یک میلیون و دویست", sampleCategories)
        assertEquals(1_200_000L, parsed1m200.tomanAmount)
        assertEquals(12_000_000L, parsed1m200.amountRial)

        // "دو میلیارد" = 2,000,000,000 Toman = 20,000,000,000 Rial
        val parsed2b = VoiceTransactionParser.parse("دو میلیارد", sampleCategories)
        assertEquals(2_000_000_000L, parsed2b.tomanAmount)
        assertEquals(20_000_000_000L, parsed2b.amountRial)

        // "۷۵۰ هزار" = 750,000 Toman = 7,500,000 Rial
        val parsed750k = VoiceTransactionParser.parse("۷۵۰ هزار", sampleCategories)
        assertEquals(750_000L, parsed750k.tomanAmount)
        assertEquals(7_500_000L, parsed750k.amountRial)
    }

    @Test
    fun testDirectNumbersAndTomanConversion() {
        // "هزینه 50000 خوراک" -> default Toman -> 50,000 Toman = 500,000 Rial
        val res = VoiceTransactionParser.parse("هزینه 50000 خوراک", sampleCategories)
        assertEquals(TransactionType.EXPENSE, res.type)
        assertEquals(500_000L, res.amountRial)
        assertEquals(50_000L, res.tomanAmount)
        assertTrue(res.isTomanDetected)
        assertNotNull(res.guessedCategory)
        assertEquals("خوراک", res.guessedCategory?.name)
    }

    @Test
    fun testExplicitRialConversion() {
        // Explicit "ریال" -> no multiplication by 10
        val res = VoiceTransactionParser.parse("خرید ۳۵۰۰۰۰ ریال", sampleCategories)
        assertEquals(TransactionType.EXPENSE, res.type)
        assertEquals(350_000L, res.amountRial)
        assertEquals(35_000L, res.tomanAmount)
        assertEquals(false, res.isTomanDetected)
    }

    @Test
    fun testIncomeDetection() {
        // "واریز دو میلیون حقوق" -> Income, 2,000,000 Toman = 20,000,000 Rial, Category حقوق
        val res = VoiceTransactionParser.parse("واریز دو میلیون حقوق", sampleCategories)
        assertEquals(TransactionType.INCOME, res.type)
        assertEquals(20_000_000L, res.amountRial)
        assertEquals(2_000_000L, res.tomanAmount)
        assertNotNull(res.guessedCategory)
        assertEquals("حقوق", res.guessedCategory?.name)
    }

    @Test
    fun testColloquialKeywordsCategoryMatching() {
        // "خرج صد و پنجاه هزار رستوران" -> Category خوراک
        val res = VoiceTransactionParser.parse("خرج صد و پنجاه هزار رستوران", sampleCategories)
        assertEquals(TransactionType.EXPENSE, res.type)
        assertEquals(1_500_000L, res.amountRial)
        assertEquals(150_000L, res.tomanAmount)
        assertNotNull(res.guessedCategory)
        assertEquals("خوراک", res.guessedCategory?.name)

        // "اسنپ بیست و پنج هزار تومن" -> Category حمل و نقل
        val res2 = VoiceTransactionParser.parse("اسنپ بیست و پنج هزار تومن", sampleCategories)
        assertEquals(TransactionType.EXPENSE, res2.type)
        assertEquals(250_000L, res2.amountRial)
        assertEquals("حمل و نقل", res2.guessedCategory?.name)
    }
}
