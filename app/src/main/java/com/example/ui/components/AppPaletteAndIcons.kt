package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val AppIconsList = listOf(
    "restaurant",
    "directions_car",
    "shopping_bag",
    "receipt_long",
    "local_hospital",
    "sports_esports",
    "home",
    "category",
    "work",
    "card_giftcard",
    "sell",
    "flight",
    "school",
    "pets",
    "fitness_center",
    "phone_iphone",
    "wifi",
    "bolt",
    "water_drop",
    "local_gas_station",
    "local_parking",
    "movie",
    "music_note",
    "camera_alt",
    "brush",
    "child_care",
    "dry_cleaning",
    "spa",
    "local_cafe",
    "local_grocery_store",
    "account_balance_wallet",
    "store",
    "attach_money",
    "credit_card"
)

val AppColorPalette = listOf(
    Color(0xFFE53935), // 1. Red
    Color(0xFFC2185B), // 2. Crimson/Rose
    Color(0xFFD81B60), // 3. Pink
    Color(0xFF8E24AA), // 4. Purple
    Color(0xFF5E35B1), // 5. Deep Purple
    Color(0xFF3949AB), // 6. Indigo
    Color(0xFF1E88E5), // 7. Blue
    Color(0xFF039BE5), // 8. Light Blue
    Color(0xFF00ACC1), // 9. Cyan
    Color(0xFF006064), // 10. Deep Cyan
    Color(0xFF00897B), // 11. Teal
    Color(0xFF43A047), // 12. Green
    Color(0xFF1B5E20), // 13. Dark Green
    Color(0xFF7CB342), // 14. Light Green
    Color(0xFF558B2F), // 15. Olive
    Color(0xFFC0CA33), // 16. Lime
    Color(0xFFFDD835), // 17. Yellow
    Color(0xFFFFB300), // 18. Amber
    Color(0xFFFB8C00), // 19. Orange
    Color(0xFFF4511E), // 20. Deep Orange
    Color(0xFFFF7F50), // 21. Coral
    Color(0xFF6D4C41), // 22. Brown
    Color(0xFF757575), // 23. Grey
    Color(0xFF546E7A), // 24. Blue Grey
    Color(0xFF212121), // 25. Dark Charcoal
    Color(0xFFFFD700)  // 26. Gold
)

data class BankIconInfo(
    val id: String,
    val name: String,
    val shortLabel: String
)

val IranianBankIcons = listOf(
    BankIconInfo("melli", "بانک ملی", "ملی"),
    BankIconInfo("resalat", "بانک رسالت", "رسالت"),
    BankIconInfo("mellat", "بانک ملت", "ملت"),
    BankIconInfo("saderat", "بانک صادرات", "صادرات"),
    BankIconInfo("tejarat", "بانک تجارت", "تجارت"),
    BankIconInfo("pasargad", "بانک پاسارگاد", "پاسارگاد"),
    BankIconInfo("sepeh", "بانک سپه", "سپه"),
    BankIconInfo("keshavarzi", "بانک کشاورزی", "کشاورزی"),
    BankIconInfo("refah", "بانک رفاه", "رفاه"),
    BankIconInfo("shahr", "بانک شهر", "شهر"),
    BankIconInfo("khavarmiyaneh", "خاورمیانه", "خاورمیانه"),
    BankIconInfo("parsian", "بانک پارسیان", "پارسیان"),
    BankIconInfo("sarmaye", "بانک سرمایه", "سرمایه"),
    BankIconInfo("karafarin", "بانک کارآفرین", "کارآفرین"),
    BankIconInfo("CASH", "نقدی", "CASH"),
    BankIconInfo("wallet", "کیف پول", "کیف‌پول"),
    BankIconInfo("credit_card", "کارت اعتباری", "کارت")
)

fun getBankShortLabel(iconId: String): String? {
    return IranianBankIcons.find { it.id == iconId }?.shortLabel
}

@Composable
fun AccountIconDisplay(
    iconName: String,
    tint: Color = Color.White,
    fontSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier
) {
    val bankLabel = getBankShortLabel(iconName)
    if (bankLabel != null && bankLabel != "CASH" && bankLabel != "کیف‌پول" && bankLabel != "کارت") {
        androidx.compose.material3.Text(
            text = bankLabel,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = modifier
        )
    } else {
        androidx.compose.material3.Icon(
            imageVector = getAppIcon(iconName),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

fun getAppIcon(name: String?): ImageVector {
    return when (name) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_bag" -> Icons.Default.ShoppingBag
        "receipt_long", "receipt" -> Icons.Default.ReceiptLong
        "local_hospital", "medical_services" -> Icons.Default.LocalHospital
        "sports_esports" -> Icons.Default.SportsEsports
        "home" -> Icons.Default.Home
        "category" -> Icons.Default.Category
        "work" -> Icons.Default.Work
        "card_giftcard" -> Icons.Default.CardGiftcard
        "sell" -> Icons.Default.Sell
        "flight" -> Icons.Default.Flight
        "school" -> Icons.Default.School
        "pets" -> Icons.Default.Pets
        "fitness_center" -> Icons.Default.FitnessCenter
        "phone_iphone" -> Icons.Default.PhoneIphone
        "wifi" -> Icons.Default.Wifi
        "bolt" -> Icons.Default.Bolt
        "water_drop" -> Icons.Default.WaterDrop
        "local_gas_station" -> Icons.Default.LocalGasStation
        "local_parking" -> Icons.Default.LocalParking
        "movie" -> Icons.Default.Movie
        "music_note" -> Icons.Default.MusicNote
        "camera_alt" -> Icons.Default.CameraAlt
        "brush" -> Icons.Default.Brush
        "child_care" -> Icons.Default.ChildCare
        "dry_cleaning" -> Icons.Default.DryCleaning
        "spa" -> Icons.Default.Spa
        "local_cafe" -> Icons.Default.LocalCafe
        "local_grocery_store" -> Icons.Default.LocalGroceryStore
        "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
        "store" -> Icons.Default.Store
        "attach_money" -> Icons.Default.AttachMoney
        "credit_card" -> Icons.Default.CreditCard
        "CASH", "payments" -> Icons.Default.Payments
        "account_balance" -> Icons.Default.AccountBalance
        else -> Icons.Default.MoreHoriz
    }
}
