package com.example.util

object NumberToWords {
    private val yekan = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val dahgan = arrayOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val dahyek = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val sadgan = arrayOf("", "یکصد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val steps = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun convert(amount: Long, suffix: String = "تومان"): String {
        if (amount == 0L) return "صفر $suffix"
        if (amount < 0) return "منفی " + convert(-amount, suffix)

        val parts = mutableListOf<String>()
        var temp = amount
        var stepIndex = 0

        while (temp > 0) {
            val chunk = (temp % 1000).toInt()
            if (chunk > 0) {
                val chunkText = convertChunk(chunk)
                val stepName = steps.getOrNull(stepIndex) ?: ""
                val part = if (stepName.isNotEmpty()) "$chunkText $stepName" else chunkText
                parts.add(0, part)
            }
            temp /= 1000
            stepIndex++
        }

        return parts.joinToString(" و ") + (if (suffix.isNotEmpty()) " $suffix" else "")
    }

    private fun convertChunk(number: Int): String {
        val s = number / 100
        val d = (number % 100) / 10
        val y = number % 10
        val list = mutableListOf<String>()

        if (s > 0) list.add(sadgan[s])

        if (d == 1) {
            list.add(dahyek[y])
        } else {
            if (d > 0) list.add(dahgan[d])
            if (y > 0) list.add(yekan[y])
        }

        return list.joinToString(" و ")
    }
}
