package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Initializing : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    data class Processing(val partialText: String = "") : VoiceRecognitionState()
    data class Success(val recognizedText: String) : VoiceRecognitionState()
    data class Error(val message: String, val errorCode: Int? = null) : VoiceRecognitionState()
}

class VoiceSpeechManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceSpeechManager"
        private const val READY_TIMEOUT_MS = 6000L // 6 seconds timeout if onReadyForSpeech is never called
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var readyTimeoutRunnable: Runnable? = null

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    fun isAvailable(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "isRecognitionAvailable: $available")
        return available
    }

    fun startListening() {
        Log.d(TAG, "startListening requested")
        if (!isAvailable()) {
            Log.e(TAG, "SpeechRecognizer is NOT available on this device")
            _state.value = VoiceRecognitionState.Error("تشخیص گفتار روی این دستگاه در دسترس نیست")
            return
        }

        // Clean previous session completely
        destroy()
        _state.value = VoiceRecognitionState.Initializing

        // Run on Main Thread
        mainHandler.post {
            try {
                Log.d(TAG, "Creating new SpeechRecognizer instance on main looper")
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.i(TAG, "onReadyForSpeech: SpeechRecognizer is ready for input")
                            cancelTimeout()
                            _state.value = VoiceRecognitionState.Listening
                        }

                        override fun onBeginningOfSpeech() {
                            Log.i(TAG, "onBeginningOfSpeech: User started speaking")
                            cancelTimeout()
                            _state.value = VoiceRecognitionState.Listening
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Audio sound level
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {
                            Log.d(TAG, "onBufferReceived: size=${buffer?.size ?: 0}")
                        }

                        override fun onEndOfSpeech() {
                            Log.i(TAG, "onEndOfSpeech: User stopped speaking, processing...")
                            cancelTimeout()
                            _state.value = VoiceRecognitionState.Processing()
                        }

                        override fun onError(error: Int) {
                            cancelTimeout()
                            val errorName = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                                SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                                SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                                SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                                SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                                else -> "UNKNOWN_ERROR_$error"
                            }
                            Log.e(TAG, "onError: code=$error ($errorName)")

                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "صدایی شنیده نشد، دوباره تلاش کن"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صدایی شنیده نشد"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشد"
                                SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_SERVER ->
                                    "سرویس تشخیص گفتار در دسترس نیست. می‌توانید از تایپ دستی استفاده کنید."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                    "خطای ارتباط با سرویس گفتار. می‌توانید دستی تایپ کنید."
                                SpeechRecognizer.ERROR_AUDIO -> "خطا در ضبط صدا"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص گفتار مشغول است، لطفاً کمی صبر کنید"
                                else -> "خطا در تشخیص گفتار (کد: $error)"
                            }
                            _state.value = VoiceRecognitionState.Error(message, error)
                        }

                        override fun onResults(results: Bundle?) {
                            cancelTimeout()
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Log.i(TAG, "onResults: matchesCount=${matches?.size ?: 0}, top=${matches?.firstOrNull()}")
                            val text = matches?.firstOrNull()?.trim()
                            if (!text.isNullOrBlank()) {
                                _state.value = VoiceRecognitionState.Success(text)
                            } else {
                                _state.value = VoiceRecognitionState.Error("صدایی شنیده نشد، دوباره تلاش کن")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim() ?: ""
                            Log.d(TAG, "onPartialResults: $partial")
                            if (partial.isNotBlank()) {
                                _state.value = VoiceRecognitionState.Processing(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {
                            Log.d(TAG, "onEvent: type=$eventType")
                        }
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

                Log.d(TAG, "Calling speechRecognizer.startListening with language fa-IR")
                speechRecognizer?.startListening(intent)

                // Set watchdog timeout in case onReadyForSpeech or onError never fires
                scheduleTimeout()

            } catch (e: Exception) {
                cancelTimeout()
                Log.e(TAG, "Exception during startListening: ${e.message}", e)
                _state.value = VoiceRecognitionState.Error("خطا در راه‌اندازی تشخیص گفتار: ${e.localizedMessage}")
            }
        }
    }

    private fun scheduleTimeout() {
        cancelTimeout()
        readyTimeoutRunnable = Runnable {
            if (_state.value is VoiceRecognitionState.Initializing || _state.value is VoiceRecognitionState.Idle) {
                Log.w(TAG, "Watchdog timeout reached ($READY_TIMEOUT_MS ms) without onReadyForSpeech")
                _state.value = VoiceRecognitionState.Error(
                    "پاسخی از سرویس تشخیص گفتار دریافت نشد. می‌توانید از تایپ دستی استفاده کنید."
                )
                destroy()
            }
        }
        mainHandler.postDelayed(readyTimeoutRunnable!!, READY_TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        readyTimeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
            readyTimeoutRunnable = null
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening requested")
        cancelTimeout()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Exception during stopListening: ${e.message}")
        }
    }

    fun reset() {
        Log.d(TAG, "reset requested")
        destroy()
        _state.value = VoiceRecognitionState.Idle
    }

    fun destroy() {
        Log.d(TAG, "destroy requested")
        cancelTimeout()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Exception during destroy: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }
}
