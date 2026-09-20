package com.example.util

import com.example.R

object BankUtils {
    fun getBankLogo(bankName: String?): Int {
        if (bankName.isNullOrBlank()) return R.drawable.ic_bank_default
        val name = bankName.lowercase()
        return when {
            name.contains("ملی") || name.contains("melli") -> R.drawable.ic_bank_melli
            name.contains("رسالت") || name.contains("resalat") -> R.drawable.ic_bank_resalat
            name.contains("تجارت") || name.contains("tejarat") -> R.drawable.ic_bank_tejarat
            name.contains("ملت") || name.contains("mellat") -> R.drawable.ic_bank_mellat
            name.contains("دی") || name.contains("dey") -> R.drawable.ic_bank_dey
            name.contains("صادرات") || name.contains("saderat") -> R.drawable.ic_bank_saderat
            name.contains("پاسارگاد") || name.contains("pasargad") -> R.drawable.ic_bank_pasargad
            name.contains("سپه") || name.contains("sepah") -> R.drawable.ic_bank_sepah
            name.contains("کشاورزی") || name.contains("keshavarzi") -> R.drawable.ic_bank_keshavarzi
            name.contains("رفاه") || name.contains("refah") -> R.drawable.ic_bank_refah
            name.contains("شهر") || name.contains("shahr") -> R.drawable.ic_bank_shahr
            name.contains("پارسیان") || name.contains("parsian") -> R.drawable.ic_bank_parsian
            else -> R.drawable.ic_bank_default
        }
    }
}
