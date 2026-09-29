package com.example.util

import java.security.MessageDigest

object SmsHashUtil {
    fun calculateHash(sender: String?, amount: Long?, date: Long, text: String): String {
        val normalizedSender = (sender ?: "").trim()
        val normalizedAmount = amount?.toString() ?: "0"
        val normalizedText = text.replace(Regex("\\s+"), " ").trim()
        val raw = "$normalizedSender|$normalizedAmount|$date|$normalizedText"

        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
