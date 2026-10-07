package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.CategoryEntity
import com.example.util.ParsedVoiceTransaction
import com.example.util.VoiceRecognitionState
import com.example.util.VoiceSpeechManager
import com.example.util.VoiceTransactionParser

@Composable
fun VoiceInputDialog(
    onDismissRequest: () -> Unit,
    categories: List<CategoryEntity>,
    onVoiceParsed: (ParsedVoiceTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speechManager = remember { VoiceSpeechManager(context) }
    val voiceState by speechManager.state.collectAsState()

    var showManualTypeDialog by remember { mutableStateOf(false) }
    var manualTypedText by remember { mutableStateOf("") }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            permissionDeniedMessage = null
            speechManager.startListening()
        } else {
            permissionDeniedMessage = "برای استفاده از ورودی صوتی، دسترسی به میکروفون ضروری است."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    // Explicit runtime permission check on entry
    LaunchedEffect(Unit) {
        if (!speechManager.isAvailable()) {
            // Speech recognition not supported on device
            return@LaunchedEffect
        }
        val currentGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        hasAudioPermission = currentGranted
        if (currentGranted) {
            speechManager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Handle success
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceRecognitionState.Success) {
            val text = (voiceState as VoiceRecognitionState.Success).recognizedText
            val parsed = VoiceTransactionParser.parse(text, categories)
            onVoiceParsed(parsed)
            onDismissRequest()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Manual typing fallback dialog
    if (showManualTypeDialog) {
        AlertDialog(
            onDismissRequest = { showManualTypeDialog = false },
            title = {
                Text(
                    text = "تایپ متن تراکنش",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "جمله یا متن تراکنش خود را بنویسید (مانند: هزینه ۵۰ هزار تومن خوراک):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualTypedText,
                        onValueChange = { manualTypedText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: واریز یک میلیون و پانصد حقوق") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = manualTypedText.trim()
                        if (text.isNotBlank()) {
                            val parsed = VoiceTransactionParser.parse(text, categories)
                            onVoiceParsed(parsed)
                            showManualTypeDialog = false
                            speechManager.destroy()
                            onDismissRequest()
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("استخراج و ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTypeDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = {
            speechManager.destroy()
            onDismissRequest()
        },
        confirmButton = {},
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        speechManager.destroy()
                        showManualTypeDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تایپ به جای گفتن")
                }

                OutlinedButton(
                    onClick = {
                        speechManager.destroy()
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("انصراف")
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ثبت با گفتار (صوتی)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        speechManager.destroy()
                        onDismissRequest()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!speechManager.isAvailable()) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "تشخیص گفتار روی این دستگاه در دسترس نیست",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showManualTypeDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Keyboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تایپ دستی متن")
                    }
                } else if (permissionDeniedMessage != null) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = permissionDeniedMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                permissionDeniedMessage = null
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("اعطای مجوز")
                        }

                        OutlinedButton(
                            onClick = {
                                showManualTypeDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تایپ دستی")
                        }
                    }
                } else {
                    when (val currentVoiceState = voiceState) {
                        is VoiceRecognitionState.Idle, is VoiceRecognitionState.Initializing -> {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "میکروفون",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "در حال آماده‌سازی و اتصال به میکروفون...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        is VoiceRecognitionState.Listening -> {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "در حال گوش دادن",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "در حال شنیدن... صحبت کنید",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "مثال: «هزینه پنجاه هزار تومن خوراک» یا «واریز دو میلیون حقوق»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp
                            )
                        }

                        is VoiceRecognitionState.Processing -> {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "پردازش صدا",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "در حال پردازش گفتار...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (currentVoiceState.partialText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = currentVoiceState.partialText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        is VoiceRecognitionState.Success -> {
                            Text(
                                text = "متن دریافت شد:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentVoiceState.recognizedText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        is VoiceRecognitionState.Error -> {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentVoiceState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        speechManager.startListening()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تلاش مجدد"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تلاش مجدد")
                                }

                                OutlinedButton(
                                    onClick = {
                                        showManualTypeDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Keyboard, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تایپ به جای گفتن")
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    )
}
