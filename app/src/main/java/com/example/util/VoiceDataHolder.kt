package com.example.util

object VoiceDataHolder {
    var pendingVoiceTransaction: ParsedVoiceTransaction? = null

    fun clear() {
        pendingVoiceTransaction = null
    }
}
