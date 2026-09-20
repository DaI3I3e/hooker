package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    val monthName: String
        get() = getMonthName(month)

    fun format(includeDayName: Boolean = false): String {
        val yearStr = year.toString().toPersianDigits()
        val dayStr = day.toString().toPersianDigits()
        return if (includeDayName) {
            val localDate = toLocalDate()
            val dayOfWeekName = getDayOfWeekName(localDate.dayOfWeek.value)
            "$dayOfWeekName $dayStr $monthName $yearStr"
        } else {
            "$dayStr $monthName $yearStr"
        }
    }

    fun toLocalDate(): LocalDate {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        return LocalDate.of(gy, gm, gd)
    }

    fun toStartOfDayTimestamp(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun toEndOfDayTimestamp(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return toLocalDate().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
    }

    companion object {
        val MONTH_NAMES = arrayOf(
            "فروردین",
            "اردیبهشت",
            "خرداد",
            "تیر",
            "مرداد",
            "شهریور",
            "مهر",
            "آبان",
            "آذر",
            "دی",
            "بهمن",
            "اسفند"
        )

        fun getMonthName(month: Int): String {
            val index = (month - 1).coerceIn(0, 11)
            return MONTH_NAMES.getOrElse(index) { "" }
        }

        private val DAY_OF_WEEK_NAMES = arrayOf(
            "",
            "دوشنبه",
            "سه‌شنبه",
            "چهارشنبه",
            "پنج‌شنبه",
            "جمعه",
            "شنبه",
            "یکشنبه"
        )

        fun getDayOfWeekName(dayOfWeekValue: Int): String {
            return DAY_OF_WEEK_NAMES.getOrNull(dayOfWeekValue) ?: ""
        }

        fun fromTimestamp(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): JalaliDate {
            val localDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            val (jy, jm, jd) = gregorianToJalali(localDate.year, localDate.monthValue, localDate.dayOfMonth)
            return JalaliDate(jy, jm, jd)
        }

        fun today(zoneId: ZoneId = ZoneId.systemDefault()): JalaliDate {
            val localDate = LocalDate.now(zoneId)
            val (jy, jm, jd) = gregorianToJalali(localDate.year, localDate.monthValue, localDate.dayOfMonth)
            return JalaliDate(jy, jm, jd)
        }

        fun getJalaliMonthLength(year: Int, month: Int): Int {
            return when {
                month in 1..6 -> 31
                month in 7..11 -> 30
                month == 12 -> if (isLeapYear(year)) 30 else 29
                else -> 30
            }
        }

        fun isLeapYear(year: Int): Boolean {
            val r = year % 33
            return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
        }

        fun getPersianDayOfWeekIndex(year: Int, month: Int, day: Int): Int {
            val (gy, gm, gd) = jalaliToGregorian(year, month, day)
            val localDate = LocalDate.of(gy, gm, gd)
            return when (localDate.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> 0
                java.time.DayOfWeek.SUNDAY -> 1
                java.time.DayOfWeek.MONDAY -> 2
                java.time.DayOfWeek.TUESDAY -> 3
                java.time.DayOfWeek.WEDNESDAY -> 4
                java.time.DayOfWeek.THURSDAY -> 5
                java.time.DayOfWeek.FRIDAY -> 6
            }
        }

        fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
            return getPersianDayOfWeekIndex(year, month, 1)
        }

        fun getStartOfCurrentMonth(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val current = today(zoneId)
            return JalaliDate(current.year, current.month, 1).toStartOfDayTimestamp(zoneId)
        }

        fun getEndOfCurrentMonth(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val current = today(zoneId)
            val lastDay = getJalaliMonthLength(current.year, current.month)
            return JalaliDate(current.year, current.month, lastDay).toEndOfDayTimestamp(zoneId)
        }

        fun getStartOfPreviousMonth(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val current = today(zoneId)
            val (prevYear, prevMonth) = if (current.month == 1) {
                Pair(current.year - 1, 12)
            } else {
                Pair(current.year, current.month - 1)
            }
            return JalaliDate(prevYear, prevMonth, 1).toStartOfDayTimestamp(zoneId)
        }

        fun getEndOfPreviousMonth(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val current = today(zoneId)
            val (prevYear, prevMonth) = if (current.month == 1) {
                Pair(current.year - 1, 12)
            } else {
                Pair(current.year, current.month - 1)
            }
            val lastDay = getJalaliMonthLength(prevYear, prevMonth)
            return JalaliDate(prevYear, prevMonth, lastDay).toEndOfDayTimestamp(zoneId)
        }

        fun getStartOfLastMonths(months: Int, zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val current = today(zoneId)
            var y = current.year
            var m = current.month - (months - 1)
            while (m <= 0) {
                m += 12
                y -= 1
            }
            return JalaliDate(y, m, 1).toStartOfDayTimestamp(zoneId)
        }

        fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
            val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            val gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400)
            for (i in 0 until gm) {
                days += gDaysInMonth[i]
            }
            if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
                days++
            }
            days += gd

            var jy = -1595 + (33 * (days / 12053))
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461

            if (days > 365) {
                jy += ((days - 1) / 365)
                days = (days - 1) % 365
            }

            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + (days / 31)
                jd = 1 + (days % 31)
            } else {
                jm = 7 + ((days - 186) / 30)
                jd = 1 + ((days - 186) % 30)
            }
            return intArrayOf(jy, jm, jd)
        }

        fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
            val jy2 = jy + 1595
            var days = -355668 + (365 * jy2) + (jy2 / 33) * 8 + (((jy2 % 33) + 3) / 4) + jd
            if (jm < 7) {
                days += (jm - 1) * 31
            } else {
                days += ((jm - 7) * 30) + 186
            }

            var gy = 400 * (days / 146097)
            days %= 146097

            if (days > 36524) {
                gy += 100 * (--days / 36524)
                days %= 36524
                if (days >= 365) days++
            }

            gy += 4 * (days / 1461)
            days %= 1461

            if (days > 365) {
                gy += (days - 1) / 365
                days = (days - 1) % 365
            }

            var gd = days + 1
            val isLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
            val salA = intArrayOf(0, 31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var gm = 0
            while (gm < 13 && gd > salA[gm]) {
                gd -= salA[gm]
                gm++
            }
            return intArrayOf(gy, gm, gd)
        }
    }
}

fun String.toPersianDigits(): String {
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder()
    for (ch in this) {
        val index = englishDigits.indexOf(ch)
        if (index != -1) {
            sb.append(persianDigits[index])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun Int.toPersianDigits(): String = this.toString().toPersianDigits()
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()
