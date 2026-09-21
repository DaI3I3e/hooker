package com.example.util

import com.example.R

object BankUtils {
    fun getBankLogo(bankName: String?): Int {
        if (bankName.isNullOrBlank()) return R.drawable.ic_bank_default
        val name = bankName.lowercase()
        return when {
            name.contains("کشاورزی") || name.contains("keshavarzi") -> R.drawable.logo_bank_keshavarzi
            name.contains("مسکن") || name.contains("maskan") -> R.drawable.logo_bank_maskan
            name.contains("مهر ایران") || name.contains("mehr") -> R.drawable.logo_bank_mehr_iran
            name.contains("ملت") || name.contains("mellat") -> R.drawable.logo_bank_mellat
            name.contains("ملی") || name.contains("melli") -> R.drawable.logo_bank_melli
            name.contains("پارسیان") || name.contains("parsian") -> R.drawable.logo_bank_parsian
            name.contains("پاسارگاد") || name.contains("pasargad") -> R.drawable.logo_bank_pasargad
            name.contains("رفاه") || name.contains("refah") -> R.drawable.logo_bank_refah
            name.contains("رسالت") || name.contains("resalat") -> R.drawable.logo_bank_resalat
            name.contains("صادرات") || name.contains("saderat") -> R.drawable.logo_bank_saderat
            name.contains("سامان") || name.contains("saman") -> R.drawable.logo_bank_saman
            name.contains("سپه") || name.contains("sepah") -> R.drawable.logo_bank_sepah
            name.contains("سینا") || name.contains("sina") -> R.drawable.logo_bank_sina
            name.contains("تجارت") || name.contains("tejarat") -> R.drawable.logo_bank_tejarat
            else -> R.drawable.ic_bank_default
        }
    }
}

