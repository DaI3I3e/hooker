package com.example.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

data class BankInfo(
    val id: String,
    val namePersian: String,
    val nameEnglish: String,
    val drawableRes: Int,
    val color: Long,
    val secondaryColor: Long,
    val cardPrefixes: List<String>,
    val shortCode: String
)

object BankLogos {
    val banks = listOf(
        BankInfo(
            id = "melli",
            namePersian = "بانک ملی",
            nameEnglish = "Bank Melli Iran",
            drawableRes = R.drawable.ic_bank_melli,
            color = 0xFF0066B3,
            secondaryColor = 0xFF003366,
            cardPrefixes = listOf("603799", "170019"),
            shortCode = "ملی"
        ),
        BankInfo(
            id = "mellat",
            namePersian = "بانک ملت",
            nameEnglish = "Bank Mellat",
            drawableRes = R.drawable.ic_bank_mellat,
            color = 0xFFBE1E2D,
            secondaryColor = 0xFFE30613,
            cardPrefixes = listOf("610433", "991975"),
            shortCode = "ملت"
        ),
        BankInfo(
            id = "saderat",
            namePersian = "بانک صادرات",
            nameEnglish = "Bank Saderat Iran",
            drawableRes = R.drawable.ic_bank_saderat,
            color = 0xFF192A56,
            secondaryColor = 0xFF273C75,
            cardPrefixes = listOf("603769", "903769"),
            shortCode = "صادرات"
        ),
        BankInfo(
            id = "tejarat",
            namePersian = "بانک تجارت",
            nameEnglish = "Tejarat Bank",
            drawableRes = R.drawable.ic_bank_tejarat,
            color = 0xFF005696,
            secondaryColor = 0xFF0984E3,
            cardPrefixes = listOf("585983", "627353"),
            shortCode = "تجارت"
        ),
        BankInfo(
            id = "sepah",
            namePersian = "بانک سپه",
            nameEnglish = "Bank Sepah",
            drawableRes = R.drawable.ic_bank_sepah,
            color = 0xFFDAA520,
            secondaryColor = 0xFFB8860B,
            cardPrefixes = listOf("589210", "627381"),
            shortCode = "سپه"
        ),
        BankInfo(
            id = "keshavarzi",
            namePersian = "بانک کشاورزی",
            nameEnglish = "Keshavarzi Bank",
            drawableRes = R.drawable.ic_bank_keshavarzi,
            color = 0xFF2E7D32,
            secondaryColor = 0xFF1B5E20,
            cardPrefixes = listOf("603770", "639217"),
            shortCode = "کشاورزی"
        ),
        BankInfo(
            id = "maskan",
            namePersian = "بانک مسکن",
            nameEnglish = "Maskan Bank",
            drawableRes = R.drawable.ic_bank_maskan,
            color = 0xFFFF6F00,
            secondaryColor = 0xFFE65100,
            cardPrefixes = listOf("628023"),
            shortCode = "مسکن"
        ),
        BankInfo(
            id = "saman",
            namePersian = "بانک سامان",
            nameEnglish = "Saman Bank",
            drawableRes = R.drawable.ic_bank_saman,
            color = 0xFF0083CA,
            secondaryColor = 0xFF80D8FF,
            cardPrefixes = listOf("621986"),
            shortCode = "سامان"
        ),
        BankInfo(
            id = "parsian",
            namePersian = "بانک پارسیان",
            nameEnglish = "Parsian Bank",
            drawableRes = R.drawable.ic_bank_parsian,
            color = 0xFF8E0000,
            secondaryColor = 0xFF5C0000,
            cardPrefixes = listOf("622106", "639194", "627884"),
            shortCode = "پارسیان"
        ),
        BankInfo(
            id = "pasargad",
            namePersian = "بانک پاسارگاد",
            nameEnglish = "Pasargad Bank",
            drawableRes = R.drawable.ic_bank_pasargad,
            color = 0xFF212121,
            secondaryColor = 0xFFFFD700,
            cardPrefixes = listOf("502229", "639347"),
            shortCode = "پاسارگاد"
        ),
        BankInfo(
            id = "blubank",
            namePersian = "بلو بانک",
            nameEnglish = "Blu Bank",
            drawableRes = R.drawable.ic_bank_blubank,
            color = 0xFF007AFF,
            secondaryColor = 0xFF5856D6,
            cardPrefixes = listOf(),
            shortCode = "بلو"
        ),
        BankInfo(
            id = "resalat",
            namePersian = "بانک قرض‌الحسنه رسالت",
            nameEnglish = "Resalat Bank",
            drawableRes = R.drawable.ic_bank_resalat,
            color = 0xFF00796B,
            secondaryColor = 0xFF004D40,
            cardPrefixes = listOf("504172"),
            shortCode = "رسالت"
        ),
        BankInfo(
            id = "mehr_iran",
            namePersian = "بانک مهر ایران",
            nameEnglish = "Qarz Al-Hasaneh Mehr Iran",
            drawableRes = R.drawable.ic_bank_mehr_iran,
            color = 0xFF00897B,
            secondaryColor = 0xFF26A69A,
            cardPrefixes = listOf("606373"),
            shortCode = "مهر"
        ),
        BankInfo(
            id = "ayandeh",
            namePersian = "بانک آینده",
            nameEnglish = "Ayandeh Bank",
            drawableRes = R.drawable.ic_bank_ayandeh,
            color = 0xFF6D4C41,
            secondaryColor = 0xFF4E342E,
            cardPrefixes = listOf("636214"),
            shortCode = "آینده"
        ),
        BankInfo(
            id = "shahr",
            namePersian = "بانک شهر",
            nameEnglish = "Shahr Bank",
            drawableRes = R.drawable.ic_bank_shahr,
            color = 0xFFC62828,
            secondaryColor = 0xFFB71C1C,
            cardPrefixes = listOf("502806", "504706"),
            shortCode = "شهر"
        ),
        BankInfo(
            id = "dey",
            namePersian = "بانک دی",
            nameEnglish = "Dey Bank",
            drawableRes = R.drawable.ic_bank_dey,
            color = 0xFF6A1B9A,
            secondaryColor = 0xFF4A148C,
            cardPrefixes = listOf("502938"),
            shortCode = "دی"
        ),
        BankInfo(
            id = "refah",
            namePersian = "بانک رفاه",
            nameEnglish = "Refah Bank",
            drawableRes = R.drawable.ic_bank_refah,
            color = 0xFF00838F,
            secondaryColor = 0xFF006064,
            cardPrefixes = listOf("589463"),
            shortCode = "رفاه"
        )
    )

    fun getBankById(id: String?): BankInfo? {
        if (id.isNullOrBlank()) return null
        return banks.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getDrawableForBank(id: String?): Int? {
        return getBankById(id)?.drawableRes
    }

    fun findBankByCardNumber(cardNumber: String?): BankInfo? {
        if (cardNumber.isNullOrBlank()) return null
        val cleanCard = cardNumber.replace("-", "").replace(" ", "").trim()
        if (cleanCard.length < 6) return null
        val prefix = cleanCard.take(6)
        return banks.find { bank -> bank.cardPrefixes.contains(prefix) }
    }

    fun findBankByName(name: String?): BankInfo? {
        if (name.isNullOrBlank()) return null
        val cleanName = name.replace("بانک", "").trim()
        return banks.find { bank ->
            bank.namePersian.contains(cleanName, ignoreCase = true) ||
            cleanName.contains(bank.shortCode, ignoreCase = true) ||
            name.contains(bank.nameEnglish, ignoreCase = true)
        }
    }

    fun detectBank(cardNumber: String?, accountName: String?): BankInfo? {
        // First check card number
        val byCard = findBankByCardNumber(cardNumber)
        if (byCard != null) return byCard

        // Second check account name
        val byName = findBankByName(accountName)
        if (byName != null) return byName

        return null
    }
}

@Composable
fun BankLogoBadge(
    bankId: String? = null,
    logoImage: String? = null,
    accountName: String? = null,
    cardNumber: String? = null,
    size: Dp = 44.dp,
    shapeRadius: Dp = 12.dp,
    fallbackColor: Color = Color(0xFF1B5E20),
    modifier: Modifier = Modifier
) {
    val bitmap = remember(logoImage) {
        if (!logoImage.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(logoImage, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(shapeRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = accountName ?: "لوگوی حساب",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(shapeRadius - 2.dp))
            )
        }
        return
    }

    val detectedBank = BankLogos.getBankById(bankId)
        ?: BankLogos.detectBank(cardNumber, accountName)

    if (detectedBank != null) {
        val primaryColor = Color(detectedBank.color)
        val secColor = Color(detectedBank.secondaryColor)

        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, secColor)
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(shapeRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = detectedBank.drawableRes),
                contentDescription = detectedBank.namePersian,
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(CircleShape)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
                .background(fallbackColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = accountName ?: "بانک",
                tint = fallbackColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

