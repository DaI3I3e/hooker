package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.util.AmountFormatter
import com.example.util.toEnglishDigits

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

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    OutlinedTextField(
        value = rawText,
        onValueChange = { newValue ->
            val cleanDigits = newValue.toEnglishDigits().filter { it.isDigit() }
            rawText = cleanDigits
            val parsed = cleanDigits.toLongOrNull() ?: 0L
            onAmountChange(parsed)
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = color
        ),
        placeholder = {
            Text(
                text = "۰",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
        },
        suffix = {
            Text(
                text = "ریال",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        },
        singleLine = true,
        visualTransformation = remember { PersianAmountVisualTransformation() },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

