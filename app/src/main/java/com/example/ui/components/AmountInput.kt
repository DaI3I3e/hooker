package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AmountFormatter
import com.example.util.NumberToWords
import com.example.util.toEnglishDigits
import com.example.util.toPersianDigits

class PersianAmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawDigits = text.text.toEnglishDigits().filter { it.isDigit() }
        if (rawDigits.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val amount = rawDigits.toLongOrNull() ?: 0L
        val formatted = AmountFormatter.format(amount, includeCurrency = false)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, rawDigits.length)
                if (clamped == 0) return 0
                var digitCount = 0
                for (i in formatted.indices) {
                    if (formatted[i].isDigit() || formatted[i] in '۰'..'۹') {
                        digitCount++
                    }
                    if (digitCount == clamped) {
                        return i + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                if (clamped == 0) return 0
                var digitCount = 0
                for (i in 0 until clamped) {
                    if (formatted[i].isDigit() || formatted[i] in '۰'..'۹') {
                        digitCount++
                    }
                }
                return digitCount
            }
        }

        return TransformedText(
            AnnotatedString(formatted),
            offsetMapping
        )
    }
}

@Composable
fun AmountInput(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var rawText by remember(amount) {
        mutableStateOf(if (amount == 0L) "" else amount.toString())
    }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawText,
            onValueChange = { newValue ->
                val cleanDigits = newValue.toEnglishDigits().filter { it.isDigit() }
                rawText = cleanDigits
                val parsed = cleanDigits.toLongOrNull() ?: 0L
                onAmountChange(parsed)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            ),
            placeholder = {
                Text(
                    text = "۰",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            suffix = {
                Text(
                    text = "ریال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            },
            singleLine = true,
            visualTransformation = remember { PersianAmountVisualTransformation() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // نمایش حروفی مبلغ به فارسی جهت جلوگیری از خطای تعداد صفرها
        AnimatedVisibility(
            visible = amount > 0L,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.08f)
            ) {
                Text(
                    text = NumberToWords.convert(amount, suffix = "ریال"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // کلیدهای کمکی مبالغ پرکاربرد و افزودن سه صفر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // کلید افزودن سه صفر (+۰۰۰)
            AssistChip(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newAmount = if (amount == 0L) 1000L else amount * 1000L
                    rawText = newAmount.toString()
                    onAmountChange(newAmount)
                },
                label = { Text("+۰۰۰", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            // کلیدهای مبالغ رایج (۵۰، ۱۰۰، ۵۰۰ هزار و ۱ میلیون)
            listOf(50_000L, 100_000L, 500_000L, 1_000_000L).forEach { addVal ->
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val newAmount = amount + addVal
                        rawText = newAmount.toString()
                        onAmountChange(newAmount)
                    },
                    label = {
                        Text(
                            "+ " + AmountFormatter.format(addVal, includeCurrency = false),
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // کلید پاک کردن
            if (amount > 0L) {
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        rawText = ""
                        onAmountChange(0L)
                    },
                    label = { Text("پاک کردن", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

