package com.example.util

import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType

object PatternExtractor {

    /**
     * Extracts parts/tokens from the SMS text line by line.
     * Accurately extracts combined date-times, pure dates, pure times, signed amounts,
     * key:value pairs, account numbers, and words into distinct ExtractedFields.
     */
    fun extractParts(smsText: String): List<ExtractedField> {
        val cleanedText = cleanSharedText(smsText)
        if (cleanedText.isBlank()) return emptyList()

        val parts = mutableListOf<ExtractedField>()
        val lines = cleanedText.lines()

        for ((lineIndex, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val tokensInLine = extractTokensFromLine(trimmedLine)
            for (token in tokensInLine) {
                if (token.isNotBlank()) {
                    parts.add(
                        ExtractedField(
                            text = token,
                            lineIndex = lineIndex,
                            fieldType = FieldType.IGNORE
                        )
                    )
                }
            }
        }

        return parts
    }

    private fun extractTokensFromLine(line: String): List<String> {
        val tokens = mutableListOf<String>()

        // Check if line is Key:Value (e.g. برداشت:680,000- or حساب:04007 or مانده:702,045,020)
        // Ensure colon is not part of a pure time (like 18:20)
        val colonIndex = line.indexOf(':')
        if (colonIndex > 0 && colonIndex < line.length - 1) {
            val prefix = line.substring(0, colonIndex).trim()
            val suffix = line.substring(colonIndex + 1).trim()
            val isPureTimePrefix = prefix.matches(Regex("""[\d۰-۹٠-٩]{1,2}""")) && suffix.matches(Regex("""[\d۰-۹٠-٩]{2}(?::[\d۰-۹٠-٩]{2})?.*"""))

            if (!isPureTimePrefix) {
                // Key part
                tokens.addAll(tokenizeSegment(prefix))
                // Value part
                tokens.addAll(tokenizeSegment(suffix))
                return tokens
            }
        }

        // Otherwise tokenize the entire line segment
        tokens.addAll(tokenizeSegment(line))
        return tokens
    }

    private fun tokenizeSegment(segment: String): List<String> {
        val tokens = mutableListOf<String>()

        val chunks = segment.split(Regex("""\s+""")).filter { it.isNotBlank() }
        for (chunk in chunks) {
            var workingChunk = chunk

            // Check date/time patterns from longest to shortest
            // Pattern 4: YY/MM/DD-HH:MM or YYYY/MM/DD-HH:MM (e.g. 05/04/21-16:34 or 1405/04/21-16:34)
            val p4 = Regex("""([\d۰-۹٠-٩]{2,4})/([\d۰-۹٠-٩]{2})/([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})""")
            val m4 = p4.find(workingChunk)
            if (m4 != null) {
                tokens.add(m4.groupValues[1])
                tokens.add(m4.groupValues[2])
                tokens.add(m4.groupValues[3])
                tokens.add(m4.groupValues[4])
                tokens.add(m4.groupValues[5])
                workingChunk = workingChunk.removeRange(m4.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Pattern 2 & 3: MM/DD_HH:MM or MM/DD-HH:MM (e.g. 05/20_18:20 or 05/12-19:46)
            val p23 = Regex("""([\d۰-۹٠-٩]{2})/([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})""")
            val m23 = p23.find(workingChunk)
            if (m23 != null) {
                tokens.add(m23.groupValues[1])
                tokens.add(m23.groupValues[2])
                tokens.add(m23.groupValues[3])
                tokens.add(m23.groupValues[4])
                workingChunk = workingChunk.removeRange(m23.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Pattern 1: MMDD-HH:MM or MMDD_HH:MM (e.g. 0525-17:28)
            val p1 = Regex("""([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})""")
            val m1 = p1.find(workingChunk)
            if (m1 != null) {
                tokens.add(m1.groupValues[1])
                tokens.add(m1.groupValues[2])
                tokens.add(m1.groupValues[3])
                tokens.add(m1.groupValues[4])
                workingChunk = workingChunk.removeRange(m1.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Pattern 5: YYYY/MM/DD or YY/MM/DD (e.g. 1405/03/01)
            val p5 = Regex("""([\d۰-۹٠-٩]{2,4})/([\d۰-۹٠-٩]{2})/([\d۰-۹٠-٩]{2})""")
            val m5 = p5.find(workingChunk)
            if (m5 != null) {
                tokens.add(m5.groupValues[1])
                tokens.add(m5.groupValues[2])
                tokens.add(m5.groupValues[3])
                workingChunk = workingChunk.removeRange(m5.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Pattern 6: HH:MM:SS (e.g. 21:05:28)
            val p6 = Regex("""([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})""")
            val m6 = p6.find(workingChunk)
            if (m6 != null) {
                tokens.add(m6.groupValues[1])
                tokens.add(m6.groupValues[2])
                tokens.add(m6.groupValues[3])
                workingChunk = workingChunk.removeRange(m6.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Pattern 7: HH:MM (e.g. 18:20)
            val p7 = Regex("""([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})""")
            val m7 = p7.find(workingChunk)
            if (m7 != null) {
                tokens.add(m7.groupValues[1])
                tokens.add(m7.groupValues[2])
                workingChunk = workingChunk.removeRange(m7.range).trim()
                if (workingChunk.isEmpty()) continue
            }

            // Signed amount with leading sign: "-6,500,000" or "+250,000,000"
            val leadingSignMatch = Regex("""^([+\-])([\d۰-۹٠-٩,،\.]+)$""").find(workingChunk)
            if (leadingSignMatch != null) {
                tokens.add(leadingSignMatch.groupValues[1])
                tokens.add(leadingSignMatch.groupValues[2])
                continue
            }

            // Signed amount with trailing sign: "680,000-" or "680,000+"
            val trailingSignMatch = Regex("""^([\d۰-۹٠-٩,،\.]+)([+\-])$""").find(workingChunk)
            if (trailingSignMatch != null) {
                tokens.add(trailingSignMatch.groupValues[1])
                tokens.add(trailingSignMatch.groupValues[2])
                continue
            }

            // Tokenize remaining sub-tokens: dotted account numbers, amounts with commas, signs, and words
            val subTokenRegex = Regex("""([+\-])|([\d۰-۹٠-٩]+(?:\.[\d۰-۹٠-٩]+)+)|([\d۰-۹٠-٩]+(?:[,\.،][\d۰-۹٠-٩]+)+)|([\d۰-۹٠-٩]+)|([^\s\d۰-۹٠-٩+\-:,/،\._()]+)""")
            val matches = subTokenRegex.findAll(workingChunk).toList()
            if (matches.isNotEmpty()) {
                for (m in matches) {
                    val v = m.value.trim()
                    if (v.isNotBlank()) tokens.add(v)
                }
            } else {
                tokens.add(workingChunk)
            }
        }

        return tokens
    }

    fun cleanSharedText(text: String): String {
        return text
            .replace("%20", " ")
            .replace(Regex("""(?<![\d۰-۹٠-٩])\+(?![\d۰-۹٠-٩])"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
