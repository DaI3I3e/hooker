package com.example.util

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType

data class ParsedVoiceTransaction(
    val type: TransactionType,
    val amountRial: Long?,
    val tomanAmount: Long?,
    val isTomanDetected: Boolean,
    val guessedCategory: CategoryEntity?,
    val recognizedText: String,
    val note: String? = null
)

object VoiceTransactionParser {

    private val DIGITS = mapOf(
        "صفر" to 0L,
        "یک" to 1L, "یه" to 1L,
        "دو" to 2L,
        "سه" to 3L,
        "چهار" to 4L, "چار" to 4L,
        "پنج" to 5L,
        "شش" to 6L, "شیش" to 6L,
        "هفت" to 7L,
        "هشت" to 8L,
        "نه" to 9L,
        "ده" to 10L,
        "یازده" to 11L,
        "دوازده" to 12L,
        "سیزده" to 13L,
        "چهارده" to 14L,
        "پانزده" to 15L, "پونزده" to 15L,
        "شانزده" to 16L, "شونزده" to 16L,
        "هفده" to 17L,
        "هجده" to 18L,
        "نوزده" to 19L,
        "بیست" to 20L,
        "سی" to 30L,
        "چهل" to 40L,
        "پنجاه" to 50L,
        "شصت" to 60L,
        "هفتاد" to 70L,
        "هشتاد" to 80L,
        "نود" to 90L,
        "صد" to 100L,
        "دویست" to 200L,
        "سیصد" to 300L,
        "چهارصد" to 400L,
        "پانصد" to 500L, "پونصد" to 500L,
        "ششصد" to 600L,
        "هفتصد" to 700L,
        "هشتصد" to 800L,
        "نهصد" to 900L
    )

    private val SCALES = mapOf(
        "هزار" to 1_000L,
        "میلیون" to 1_000_000L,
        "میلیارد" to 1_000_000_000L
    )

    private val EXPENSE_KEYWORDS = listOf(
        "هزینه", "خرج", "برداشت", "پرداخت", "خرید", "دادم به", "دادم", "خرج کردم"
    )

    private val INCOME_KEYWORDS = listOf(
        "درآمد", "واریز", "حقوق", "رسید", "گرفتم", "دریافت کردم", "فروختم", "دریافت"
    )

    private val CATEGORY_KEYWORD_MAP = mapOf(
        "خوراک" to listOf("خوراک", "غذا", "رستوران", "سوپرمارکت", "سوپر", "میوه", "نان", "شام", "ناهار", "صبحانه", "کیک", "گوشت", "مرغ"),
        "حمل و نقل" to listOf("تاکسی", "اسنپ", "تپسی", "بنزین", "مترو", "اتوبوس", "سوخت", "کرایه", "پارکینگ"),
        "سلامت" to listOf("دارو", "دکتر", "ویزیت", "آمپول", "بیمارستان", "درمان", "آزمایشگاه", "دندان", "پزشک", "داروخانه"),
        "قبوض" to listOf("قبض", "برق", "گاز", "تلفن", "آب", "اینترنت", "شارژ"),
        "مسکن" to listOf("اجاره", "خونه", "خانه", "رهن", "ساختمان"),
        "خرید" to listOf("لباس", "پوشاک", "فروشگاه", "خرید", "کفش", "کیف"),
        "تفریح" to listOf("فیلم", "سینما", "تفریح", "کافه", "گردش", "مسافرت", "هتل", "بلیط", "تور", "ورزش", "باشگاه"),
        "آموزش" to listOf("آموزش", "کتاب", "کلاس", "دانشگاه", "مدرسه", "شهریه"),
        "اقساط" to listOf("قسط", "وام", "بدهی", "چک")
    )

    /**
     * Parses a spoken or typed Persian transaction text into a structured result.
     */
    fun parse(
        text: String,
        availableCategories: List<CategoryEntity> = emptyList()
    ): ParsedVoiceTransaction {
        val normalized = normalizePersianText(text)

        // 1. Determine transaction type
        val type = determineTransactionType(normalized)

        // 2. Extract amount
        val (rialAmount, tomanAmount, isToman) = extractAmount(normalized)

        // 3. Match category
        val matchedCategory = matchCategory(normalized, availableCategories, type)

        return ParsedVoiceTransaction(
            type = type,
            amountRial = rialAmount,
            tomanAmount = tomanAmount,
            isTomanDetected = isToman,
            guessedCategory = matchedCategory,
            recognizedText = text.trim(),
            note = text.trim()
        )
    }

    /**
     * Normalizes Arabic chars, punctuation, and Persian digits to Latin for consistent parsing.
     */
    fun normalizePersianText(input: String): String {
        var res = input
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace("،", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("!", " ")
            .replace("؟", " ")
            .replace("?", " ")
            .replace("\u200C", " ") // Zero-width non-joiner to space for easier tokenization

        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        persianDigits.forEachIndexed { i, c ->
            res = res.replace(c, ('0' + i))
        }

        return res.trim().replace(Regex("\\s+"), " ")
    }

    fun determineTransactionType(normalizedText: String): TransactionType {
        val words = normalizedText.split(" ")
        for (kw in INCOME_KEYWORDS) {
            if (normalizedText.contains(kw) || words.contains(kw)) {
                return TransactionType.INCOME
            }
        }
        for (kw in EXPENSE_KEYWORDS) {
            if (normalizedText.contains(kw) || words.contains(kw)) {
                return TransactionType.EXPENSE
            }
        }
        // Default is EXPENSE
        return TransactionType.EXPENSE
    }

    /**
     * Extracts numerical amount from Persian sentence.
     * Returns Triple(rialAmount, tomanAmount, isTomanDetected)
     */
    fun extractAmount(normalizedText: String): Triple<Long?, Long?, Boolean> {
        val words = normalizedText.split(" ")
        val hasRial = words.contains("ریال")
        val hasToman = words.contains("تومان") || words.contains("تومن")

        // Find contiguous segments of number words
        val sequences = mutableListOf<List<String>>()
        var currentSeq = mutableListOf<String>()

        for (w in words) {
            if (isNumberToken(w)) {
                currentSeq.add(w)
            } else if (w == "و" && currentSeq.isNotEmpty()) {
                currentSeq.add(w)
            } else {
                if (currentSeq.isNotEmpty()) {
                    while (currentSeq.isNotEmpty() && currentSeq.last() == "و") {
                        currentSeq.removeAt(currentSeq.lastIndex)
                    }
                    if (currentSeq.isNotEmpty()) {
                        sequences.add(currentSeq.toList())
                    }
                    currentSeq = mutableListOf()
                }
            }
        }
        if (currentSeq.isNotEmpty()) {
            while (currentSeq.isNotEmpty() && currentSeq.last() == "و") {
                currentSeq.removeAt(currentSeq.lastIndex)
            }
            if (currentSeq.isNotEmpty()) {
                sequences.add(currentSeq.toList())
            }
        }

        if (sequences.isEmpty()) {
            return Triple(null, null, false)
        }

        // Evaluate all number sequences and select the most significant amount
        var bestVal = 0L
        for (seq in sequences) {
            val tokens = seq.filter { it != "و" }
            val parsedVal = parseNumberTokens(tokens)
            if (parsedVal > bestVal) {
                bestVal = parsedVal
            }
        }

        if (bestVal <= 0L) {
            return Triple(null, null, false)
        }

        // Currency unit conversion:
        // If user says "ریال" -> base amount is Rial
        // If user says "تومان" / "تومن" or says nothing -> base amount is Toman (Rial = amount * 10)
        return if (hasRial && !hasToman) {
            val rial = bestVal
            val toman = bestVal / 10
            Triple(rial, toman, false)
        } else {
            val toman = bestVal
            val rial = bestVal * 10
            Triple(rial, toman, true)
        }
    }

    private fun isNumberToken(token: String): Boolean {
        if (token.toLongOrNull() != null) return true
        if (DIGITS.containsKey(token)) return true
        if (SCALES.containsKey(token)) return true
        return false
    }

    /**
     * Parses a list of number tokens (e.g., ["هشتاد", "سه", "هزار"] or ["50000"] or ["2", "میلیون"])
     */
    fun parseNumberTokens(tokens: List<String>): Long {
        var total = 0L
        var current = 0L

        for (t in tokens) {
            val directNum = t.toLongOrNull()
            if (directNum != null) {
                current += directNum
            } else if (DIGITS.containsKey(t)) {
                current += DIGITS[t] ?: 0L
            } else if (SCALES.containsKey(t)) {
                val scale = SCALES[t] ?: 1L
                if (current == 0L) {
                    current = 1L
                }
                total += current * scale
                current = 0L
            }
        }

        // Colloquial Persian handling: e.g. "یک میلیون و دویست" implies 1,200,000 (دویست هزار)
        if (total >= 1_000_000L && current in 1L..999L) {
            total += current * 1_000L
        } else {
            total += current
        }

        return total
    }

    /**
     * Matches sentence against actual user categories or predefined category keywords.
     */
    fun matchCategory(
        normalizedText: String,
        categories: List<CategoryEntity>,
        type: TransactionType
    ): CategoryEntity? {
        val words = normalizedText.split(" ")

        // 1. Direct match with user's existing category names
        val exactMatch = categories.find { cat ->
            val catNormalized = normalizePersianText(cat.name).lowercase()
            normalizedText.contains(catNormalized) || words.contains(catNormalized)
        }
        if (exactMatch != null) return exactMatch

        // 2. Keyword dictionary match mapped to existing categories
        for ((catKey, keywords) in CATEGORY_KEYWORD_MAP) {
            val hasKeyword = keywords.any { kw ->
                normalizedText.contains(kw) || words.contains(kw)
            }
            if (hasKeyword) {
                val found = categories.find { cat ->
                    val catName = cat.name.lowercase()
                    catName.contains(catKey) || catKey.contains(catName)
                }
                if (found != null) return found
            }
        }

        return null
    }
}
