package com.example.util

import com.example.data.local.entity.TransactionType

data class ParsedVoiceTransaction(
    val type: TransactionType,
    val amount: Long, // In Rial
    val suggestedCategoryName: String?,
    val note: String?,
    val rawText: String
)

object VoiceTransactionParser {

    private val DIGITS_MAP = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val WORDS_TO_NUM = mapOf(
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
        "ششصد" to 600L, "شیشصد" to 600L,
        "هفتصد" to 700L,
        "هشتصد" to 800L,
        "نهصد" to 900L
    )

    private val EXPENSE_KEYWORDS = listOf(
        "هزینه", "خرج", "برداشت", "پرداخت", "پرداخت کردم", "خرید", "خریدم", "دادم", "دادم به", "خرج کردم", "کرایه"
    )

    private val INCOME_KEYWORDS = listOf(
        "درآمد", "واریز", "حقوق", "رسید", "گرفتم", "دریافت کردم", "دریافت", "فروختم", "واریزی", "پاداش"
    )

    /**
     * Parses spoken Persian text and extracts transaction details.
     * All output amounts are converted to Rial.
     */
    fun parse(text: String, categoryNames: List<String> = emptyList()): ParsedVoiceTransaction {
        val cleanText = normalizeText(text)

        // 1. Determine Transaction Type
        val type = determineTransactionType(cleanText)

        // 2. Determine Unit (Toman vs Rial) - default in spoken Persian is Toman
        val isToman = cleanText.contains("تومان") || cleanText.contains("تومن") || !cleanText.contains("ریال")

        // 3. Extract Amount
        val rawAmount = extractAmount(cleanText)
        val amountInRial = if (isToman) rawAmount * 10L else rawAmount

        // 4. Match Category
        val matchedCategory = matchCategory(cleanText, categoryNames)

        // 5. Build clean note
        val note = buildNote(cleanText, matchedCategory)

        return ParsedVoiceTransaction(
            type = type,
            amount = amountInRial,
            suggestedCategoryName = matchedCategory,
            note = note,
            rawText = text
        )
    }

    private fun normalizeText(text: String): String {
        var res = text.replace("ي", "ی").replace("ك", "ک")
        // Standardize Persian digits to Latin digits for direct parsing
        val sb = StringBuilder()
        for (ch in res) {
            sb.append(DIGITS_MAP[ch] ?: ch)
        }
        res = sb.toString()
        // Replace punctuation with spaces
        res = res.replace(Regex("[،؛\\.,!?؟\\-]"), " ")
        return res.trim()
    }

    private fun determineTransactionType(text: String): TransactionType {
        // Check income keywords
        for (kw in INCOME_KEYWORDS) {
            if (text.contains(kw)) return TransactionType.INCOME
        }
        // Default is EXPENSE
        return TransactionType.EXPENSE
    }

    /**
     * Extracts numerical value from spoken Persian text.
     * Handles complex combinations:
     * - "دو میلیون و پانصد هزار" -> 2,500,000
     * - "هشتاد و سه هزار" -> 83,000
     * - "یک میلیون و دویست" -> 1,200,000
     * - "دو میلیارد" -> 2,000,000,000
     * - "۷۵۰ هزار" -> 750,000
     * - "50000" -> 50,000
     */
    fun extractAmount(text: String): Long {
        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }

        // First check for direct full digit match like "50000" or "۵۰۰۰۰"
        for (token in tokens) {
            if (token.matches(Regex("\\d{4,}"))) {
                return token.toLongOrNull() ?: 0L
            }
        }

        // Segment words and process scales
        var totalAmount = 0L
        var currentBillions = 0L
        var currentMillions = 0L
        var currentThousands = 0L
        var currentSmall = 0L

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            // Skip "و" (and) conjunction
            if (token == "و") {
                i++
                continue
            }

            // Check if token is a direct number like "750" or "۲۵"
            val directNumber = token.toLongOrNull()
            if (directNumber != null) {
                currentSmall += directNumber
                i++
                continue
            }

            // Check if token is in WORDS_TO_NUM
            val wordVal = WORDS_TO_NUM[token]
            if (wordVal != null) {
                currentSmall += wordVal
                i++
                continue
            }

            // Check scales
            when (token) {
                "میلیارد" -> {
                    val multiplier = if (currentSmall == 0L) 1L else currentSmall
                    currentBillions += multiplier * 1_000_000_000L
                    currentSmall = 0L
                }
                "میلیون" -> {
                    val multiplier = if (currentSmall == 0L) 1L else currentSmall
                    currentMillions += multiplier * 1_000_000L
                    currentSmall = 0L
                }
                "هزار" -> {
                    val multiplier = if (currentSmall == 0L) 1L else currentSmall
                    currentThousands += multiplier * 1_000L
                    currentSmall = 0L
                }
            }
            i++
        }

        // Heuristic: If we had millions like "یک میلیون و دویست" (no "هزار" explicitly mentioned after 200),
        // 200 after million means 200,000!
        if (currentMillions > 0 && currentThousands == 0L && currentSmall in 1..999) {
            currentThousands = currentSmall * 1000L
            currentSmall = 0L
        }

        totalAmount = currentBillions + currentMillions + currentThousands + currentSmall
        return totalAmount
    }

    private fun matchCategory(text: String, categoryNames: List<String>): String? {
        // 1. Direct match with user categories
        for (cat in categoryNames) {
            val cleanCat = normalizeText(cat)
            if (cleanCat.isNotBlank() && text.contains(cleanCat)) {
                return cat
            }
        }

        // 2. Common category synonyms mapping
        val synonyms = mapOf(
            "خوراک" to listOf("غذا", "رستوران", "ناهار", "شام", "صبحانه", "سوپرمارکت", "میوه", "نان", "کافه"),
            "حمل و نقل" to listOf("تاکسی", "اسنپ", "تپسی", "بنزین", "مترو", "اتوبوس", "کرایه"),
            "پوشاک" to listOf("لباس", "کفش", "شلوار", "پیراهن"),
            "خانه" to listOf("اجاره", "قبض", "برق", "آب", "گاز", "شارژ"),
            "سلامت" to listOf("دارو", "دکتر", "پزشک", "داروخانه", "درمان"),
            "تفریح" to listOf("سینما", "کتاب", "بازی", "ورزش", "استخر"),
            "حقوق" to listOf("حقوق", "دستمزد", "پاداش", "عیدی")
        )

        for ((targetCat, words) in synonyms) {
            for (w in words) {
                if (text.contains(w)) {
                    // Check if user has this targetCat or synonym in categories
                    val matched = categoryNames.find { it.contains(targetCat) || it.contains(w) }
                    return matched ?: targetCat
                }
            }
        }

        return null
    }

    private fun buildNote(text: String, matchedCategory: String?): String? {
        // Clean out known keywords to generate clean note
        var cleaned = text
        for (kw in EXPENSE_KEYWORDS + INCOME_KEYWORDS) {
            cleaned = cleaned.replace(kw, "")
        }
        cleaned = cleaned.replace("تومان", "").replace("تومن", "").replace("ریال", "")
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return if (cleaned.length > 2) cleaned else matchedCategory
    }
}
