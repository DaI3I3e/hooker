package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    data class Processing(val partialText: String = "") : VoiceRecognitionState()
    data class Success(val recognizedText: String) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

class VoiceSpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            _state.value = VoiceRecognitionState.Error("تشخیص گفتار روی این دستگاه در دسترس نیست")
            return
        }

        destroy()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = VoiceRecognitionState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _state.value = VoiceRecognitionState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Optional audio level visualizer
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _state.value = VoiceRecognitionState.Processing()
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "صدایی شناسایی نشد، لطفاً دوباره امتحان کنید"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "زمان ضبط به پایان رسید بدون دریافت صدا"
                            SpeechRecognizer.ERROR_AUDIO -> "خطا در ضبط صدا"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطا در ارتباط تشخیص گفتار"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز دسترسی به میکروفون داده نشده است"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص گفتار مشغول است"
                            else -> "خطایی در تشخیص گفتار رخ داد"
                        }
                        _state.value = VoiceRecognitionState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            _state.value = VoiceRecognitionState.Success(text)
                        } else {
                            _state.value = VoiceRecognitionState.Error("متنی از گفتار دریافت نشد")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            _state.value = VoiceRecognitionState.Processing(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = VoiceRecognitionState.Error("خطا در راه‌اندازی تشخیص گفتار: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun reset() {
        destroy()
        _state.value = VoiceRecognitionState.Idle
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
