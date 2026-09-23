package com.example.util

object DateFormatter {
    fun formatShort(timestamp: Long): String {
        return try {
            val validTs = if (timestamp > 0L) timestamp else System.currentTimeMillis()
            val jalali = JalaliDate.fromTimestamp(validTs)
            val dayStr = jalali.day.toString().padStart(2, '0').toPersianDigits()
            val monthStr = jalali.month.toString().padStart(2, '0').toPersianDigits()
            val yearStr = (jalali.year % 100).toString().padStart(2, '0').toPersianDigits()
            "$yearStr/$monthStr/$dayStr"
        } catch (e: Exception) {
            "–"
        }
    }

    fun formatLong(timestamp: Long): String {
        return try {
            val validTs = if (timestamp > 0L) timestamp else System.currentTimeMillis()
            val jalali = JalaliDate.fromTimestamp(validTs)
            jalali.format(includeDayName = true)
        } catch (e: Exception) {
            "–"
        }
    }

    fun formatMedium(timestamp: Long): String {
        return try {
            val validTs = if (timestamp > 0L) timestamp else System.currentTimeMillis()
            val jalali = JalaliDate.fromTimestamp(validTs)
            jalali.format(includeDayName = false)
        } catch (e: Exception) {
            "–"
        }
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "هرگز"
        return try {
            val jalali = JalaliDate.fromTimestamp(timestamp)
            val zonedDateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
            val hour = zonedDateTime.hour.toString().padStart(2, '0').toPersianDigits()
            val minute = zonedDateTime.minute.toString().padStart(2, '0').toPersianDigits()
            val dateStr = jalali.format(includeDayName = false)
            "$dateStr - $hour:$minute"
        } catch (e: Exception) {
            "–"
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val hStr = hour.coerceIn(0, 23).toString().padStart(2, '0').toPersianDigits()
        val mStr = minute.coerceIn(0, 59).toString().padStart(2, '0').toPersianDigits()
        return "$hStr:$mStr"
    }

    fun combineDateAndTime(dateTimestamp: Long, hour: Int, minute: Int): Long {
        return try {
            val validTs = if (dateTimestamp > 0L) dateTimestamp else System.currentTimeMillis()
            val zoneId = java.time.ZoneId.systemDefault()
            val zonedDateTime = java.time.Instant.ofEpochMilli(validTs).atZone(zoneId)
            val combined = zonedDateTime.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0).atZone(zoneId)
            combined.toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun extractHourAndMinute(timestamp: Long): Pair<Int, Int> {
        return try {
            val validTs = if (timestamp > 0L) timestamp else System.currentTimeMillis()
            val zoneId = java.time.ZoneId.systemDefault()
            val zonedDateTime = java.time.Instant.ofEpochMilli(validTs).atZone(zoneId)
            Pair(zonedDateTime.hour, zonedDateTime.minute)
        } catch (e: Exception) {
            Pair(12, 0)
        }
    }
}
