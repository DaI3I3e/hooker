package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.TransactionType
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.ParsedVoiceTransaction
import com.example.util.VoiceTransactionParser

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    RESULT_READY,
    ERROR
}

@Composable
fun VoiceInputDialog(
    categoryNames: List<String>,
    onDismiss: () -> Unit,
    onTransactionParsed: (ParsedVoiceTransaction) -> Unit
) {
    val context = LocalContext.current

    var state by remember { mutableStateOf(VoiceInputState.IDLE) }
    var statusText by remember { mutableStateOf("برای شروع صحبت دکمه میکروفون را لمس کنید") }
    var recognizedText by remember { mutableStateOf("") }
    var parsedTransaction by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }

    val isRecognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    // System Speech Recognizer Activity Launcher (Fallback for older devices)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val spokenTexts = activityResult.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestText = spokenTexts?.firstOrNull()
            if (!bestText.isNullOrBlank()) {
                recognizedText = bestText
                state = VoiceInputState.PROCESSING
                statusText = "در حال پردازش گفتار..."
                val parsed = VoiceTransactionParser.parse(bestText, categoryNames)
                parsedTransaction = parsed
                state = VoiceInputState.RESULT_READY
                statusText = "متن با موفقیت پردازش شد"
            } else {
                state = VoiceInputState.ERROR
                statusText = "صدایی دریافت نشد"
            }
        }
    }

    fun launchSystemSpeechIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تراکنش خود را بگویید (مثلاً: هزینه پنجاه هزار تومن خوراک)")
            }
            systemSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "سرویس ورودی صوتی سیستم در دسترس نیست: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        speechRecognizer = null
    }

    fun startListeningInternal() {
        stopListening()

        if (!isRecognitionAvailable) {
            statusText = "سرویس تشخیص گفتار داخلی در دسترس نیست. از دیالوگ صوتی سیستم استفاده کنید."
            state = VoiceInputState.ERROR
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    state = VoiceInputState.LISTENING
                    statusText = "در حال شنیدن... صحبت کنید"
                }

                override fun onBeginningOfSpeech() {
                    state = VoiceInputState.LISTENING
                    statusText = "در حال دریافت صدا..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    rmsLevel = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    state = VoiceInputState.PROCESSING
                    statusText = "در حال پردازش گفتار..."
                }

                override fun onError(error: Int) {
                    state = VoiceInputState.ERROR
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "صدایی شنیده نشد، دوباره تلاش کنید"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صدایی شنیده نشد (پایان زمان)"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشده است"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "خطای اتصال شبکه جهت تشخیص گفتار"
                        SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_SERVER ->
                            "پاسخی از سرویس تشخیص گفتار دریافت نشد. می‌توانید از دیالوگ صوتی سیستم یا تایپ استفاده کنید."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس گفتار مشغول است، مجدداً امتحان کنید"
                        else -> "خطا در تشخیص گفتار (کد $error). از دکمه سیستم استفاده کنید."
                    }
                    statusText = errorMsg
                    stopListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestText = matches?.firstOrNull()

                    if (!bestText.isNullOrBlank()) {
                        recognizedText = bestText
                        val parsed = VoiceTransactionParser.parse(bestText, categoryNames)
                        parsedTransaction = parsed
                        state = VoiceInputState.RESULT_READY
                        statusText = "متن با موفقیت پردازش شد"
                    } else {
                        state = VoiceInputState.ERROR
                        statusText = "متنی استخراج نشد"
                    }
                    stopListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partials?.firstOrNull()?.let {
                        recognizedText = it
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            state = VoiceInputState.LISTENING
            statusText = "در حال آماده‌سازی..."
            recognizer.startListening(intent)
        } catch (e: Exception) {
            state = VoiceInputState.ERROR
            statusText = "خطا در راه‌اندازی ضبط: ${e.message}"
            stopListening()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            state = VoiceInputState.ERROR
            statusText = "برای ثبت صوتی، مجوز دسترسی به میکروفون الزامی است."
        }
    }

    fun requestPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListeningInternal()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopListening()
        }
    }

    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = {
            stopListening()
            onDismiss()
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            VoiceInputState.LISTENING -> MaterialTheme.colorScheme.primaryContainer
                            VoiceInputState.RESULT_READY -> IncomeColor.copy(alpha = 0.2f)
                            VoiceInputState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state == VoiceInputState.LISTENING) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }

                Icon(
                    imageVector = when (state) {
                        VoiceInputState.RESULT_READY -> Icons.Default.Check
                        VoiceInputState.ERROR -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "میکروفون",
                    tint = when (state) {
                        VoiceInputState.LISTENING -> MaterialTheme.colorScheme.primary
                        VoiceInputState.RESULT_READY -> IncomeColor
                        VoiceInputState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = "ثبت صوتی تراکنش",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Text
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = when (state) {
                        VoiceInputState.ERROR -> MaterialTheme.colorScheme.error
                        VoiceInputState.LISTENING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Recognized text display
                if (recognizedText.isNotBlank()) {
                    OutlinedTextField(
                        value = recognizedText,
                        onValueChange = {
                            recognizedText = it
                            if (it.isNotBlank()) {
                                parsedTransaction = VoiceTransactionParser.parse(it, categoryNames)
                                state = VoiceInputState.RESULT_READY
                            }
                        },
                        label = { Text("متن گفتار") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Parsed preview card
                parsedTransaction?.let { parsed ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نوع تراکنش:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (parsed.type == TransactionType.EXPENSE) "هزینه" else "درآمد",
                                    fontWeight = FontWeight.Bold,
                                    color = if (parsed.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "مبلغ استخراجی:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = AmountFormatter.formatAmountWithCurrency(parsed.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (!parsed.suggestedCategoryName.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "دسته‌بندی پیشنهادی:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = parsed.suggestedCategoryName,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Microphone Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state == VoiceInputState.LISTENING) {
                        Button(
                            onClick = { stopListening() },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("پایان صحبت")
                        }
                    } else {
                        Button(
                            onClick = { requestPermissionAndStart() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state == VoiceInputState.RESULT_READY) "تکرار ضبط" else "شروع گفتار")
                        }
                    }
                }

                // System Speech Recognizer Button (CRITICAL for older phones)
                OutlinedButton(
                    onClick = {
                        stopListening()
                        launchSystemSpeechIntent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ورودی صوتی سیستم (گوگل / دیالوگ اندروید)", fontSize = 12.sp)
                }

                // Hints
                Text(
                    text = "نمونه: «هزینه پنجاه هزار تومن خوراک» یا «واریز دو میلیون حقوق»",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            if (parsedTransaction != null && (parsedTransaction?.amount ?: 0L) > 0L) {
                Button(
                    onClick = {
                        parsedTransaction?.let { onTransactionParsed(it) }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تأیید و باز کردن فرم")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    stopListening()
                    onDismiss()
                }
            ) {
                Text("انصراف")
            }
        }
    )
}
