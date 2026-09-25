package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import java.io.File
import java.time.ZoneId

object CsvExporter {

    /**
     * Generates a standard CSV with UTF-8 BOM (\uFEFF) for 100% compatibility with
     * Microsoft Excel and Persian characters.
     */
    fun generateTransactionsCsv(transactions: List<TransactionWithDetails>): String {
        val zoneId = ZoneId.systemDefault()
        val builder = java.lang.StringBuilder()

        // UTF-8 BOM
        builder.append('\uFEFF')

        // CSV Header
        builder.append("ردیف,تاریخ شمسی,ساعت,نوع تراکنش,مبلغ (ریال),مبلغ (تومان),حساب,به حساب,دسته‌بندی,یادداشت\n")

        transactions.forEachIndexed { index, item ->
            val tx = item.transaction
            val jalali = JalaliDate.fromTimestamp(tx.date, zoneId)
            val dateStr = jalali.format(includeDayName = false)
            val (h, m) = DateFormatter.extractHourAndMinute(tx.date)
            val timeStr = String.format("%02d:%02d", h, m)

            val typeStr = when (tx.type) {
                TransactionType.EXPENSE -> "هزینه"
                TransactionType.INCOME -> "درآمد"
                TransactionType.TRANSFER -> "انتقال"
            }

            val amountRial = tx.amount
            val amountToman = tx.amount / 10
            val account = escapeCsv(item.accountName ?: "")
            val toAccount = escapeCsv(item.toAccountName ?: "")
            val category = escapeCsv(item.categoryName ?: "")
            val note = escapeCsv(tx.note ?: "")

            builder.append("${index + 1},$dateStr,$timeStr,$typeStr,$amountRial,$amountToman,$account,$toAccount,$category,$note\n")
        }

        return builder.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    fun shareCsv(context: Context, csvContent: String, fileName: String = "fintrack_report.csv") {
        try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            file.writeText(csvContent, Charsets.UTF_8)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "گزارش تراکنش‌های فین‌ترک")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش اکسل (CSV)"))
        } catch (e: Exception) {
            // Fallback to sharing as plain text
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, csvContent)
                putExtra(Intent.EXTRA_SUBJECT, "گزارش تراکنش‌های فین‌ترک")
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش"))
        }
    }
}
