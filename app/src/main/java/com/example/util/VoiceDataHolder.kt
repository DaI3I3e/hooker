package com.example.util

object VoiceDataHolder {
    var pendingVoiceTransaction: ParsedVoiceTransaction? = null

    fun getAndClear(): ParsedVoiceTransaction? {
        val result = pendingVoiceTransaction
        pendingVoiceTransaction = null
        return result
    }
}
