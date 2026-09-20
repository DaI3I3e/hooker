package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.DateFormatter
import com.example.util.JalaliDate

@Composable
fun JalaliDateRangePickerDialog(
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    onRangeSelected: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate ?: System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(initialEndDate ?: System.currentTimeMillis()) }

    var selectingStartPicker by remember { mutableStateOf(false) }
    var selectingEndPicker by remember { mutableStateOf(false) }

    if (selectingStartPicker) {
        JalaliDatePickerDialog(
            initialTimestamp = startDate,
            onDateSelected = { timestamp ->
                startDate = timestamp
                selectingStartPicker = false
            },
            onDismiss = { selectingStartPicker = false }
        )
    }

    if (selectingEndPicker) {
        JalaliDatePickerDialog(
            initialTimestamp = endDate,
            onDateSelected = { timestamp ->
                endDate = timestamp
                selectingEndPicker = false
            },
            onDismiss = { selectingEndPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب بازه تاریخ دلخواه", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("تاریخ شروع:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { selectingStartPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(DateFormatter.formatLong(startDate))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("تاریخ پایان:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { selectingEndPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(DateFormatter.formatLong(endDate))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = minOf(startDate, endDate)
                    val e = maxOf(startDate, endDate)
                    val endJalali = JalaliDate.fromTimestamp(e)
                    onRangeSelected(s, endJalali.toEndOfDayTimestamp())
                    onDismiss()
                }
            ) {
                Text("تایید", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
