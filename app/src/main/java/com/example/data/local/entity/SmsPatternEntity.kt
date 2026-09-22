package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_patterns")
data class SmsPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountIdentifier: String,
    val sampleSms: String,
    val extractedFields: String, // JSON serialized List<ExtractedField>
    val bankName: String?,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExtractedField(
    val text: String,
    val lineIndex: Int,
    val fieldType: FieldType? = FieldType.IGNORE
)

enum class FieldType(val persianLabel: String) {
    BANK_NAME("نام بانک"),
    ACCOUNT_IDENTIFIER("شناسه حساب"),
    AMOUNT("مبلغ تراکنش"),
    TRANSACTION_TYPE("نوع تراکنش"),
    SIGN("نوع تراکنش (واریز/برداشت)"),
    DATE_YEAR("سال تاریخ"),
    DATE_MONTH("ماه تاریخ"),
    DATE_DAY("روز تاریخ"),
    TIME_HOUR("ساعت"),
    TIME_MINUTE("دقیقه"),
    BALANCE("مانده"),
    DESCRIPTION("توضیحات"),
    IGNORE("نادیده بگیر")
}
