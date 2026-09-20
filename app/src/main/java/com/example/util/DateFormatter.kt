package com.example.util

object DateFormatter {
    fun formatShort(timestamp: Long): String {
        val jalali = JalaliDate.fromTimestamp(timestamp)
        val dayStr = jalali.day.toString().padStart(2, '0').toPersianDigits()
        val monthStr = jalali.month.toString().padStart(2, '0').toPersianDigits()
        val yearStr = (jalali.year % 100).toString().padStart(2, '0').toPersianDigits()
        return "$yearStr/$monthStr/$dayStr"
    }

    fun formatLong(timestamp: Long): String {
        val jalali = JalaliDate.fromTimestamp(timestamp)
        return jalali.format(includeDayName = true)
    }

    fun formatMedium(timestamp: Long): String {
        val jalali = JalaliDate.fromTimestamp(timestamp)
        return jalali.format(includeDayName = false)
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "هرگز"
        val jalali = JalaliDate.fromTimestamp(timestamp)
        val zonedDateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
        val hour = zonedDateTime.hour.toString().padStart(2, '0').toPersianDigits()
        val minute = zonedDateTime.minute.toString().padStart(2, '0').toPersianDigits()
        val dateStr = jalali.format(includeDayName = false)
        return "$dateStr - $hour:$minute"
    }

    fun formatTime(hour: Int, minute: Int): String {
        val hStr = hour.toString().padStart(2, '0').toPersianDigits()
        val mStr = minute.toString().padStart(2, '0').toPersianDigits()
        return "$hStr:$mStr"
    }

    fun combineDateAndTime(dateTimestamp: Long, hour: Int, minute: Int): Long {
        val zoneId = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(dateTimestamp).atZone(zoneId)
        val combined = zonedDateTime.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0).atZone(zoneId)
        return combined.toInstant().toEpochMilli()
    }

    fun extractHourAndMinute(timestamp: Long): Pair<Int, Int> {
        val zoneId = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId)
        return Pair(zonedDateTime.hour, zonedDateTime.minute)
    }
}
