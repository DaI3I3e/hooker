package com.example.data.local

import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType

object SeedData {
    val defaultCategories = listOf(
        // Expense Categories (8)
        CategoryEntity(
            id = 1,
            name = "خوراک",
            type = CategoryType.EXPENSE,
            color = 0xFFFF5722.toInt(), // Orange
            icon = "restaurant",
            isDefault = true
        ),
        CategoryEntity(
            id = 2,
            name = "حمل‌ونقل",
            type = CategoryType.EXPENSE,
            color = 0xFF2196F3.toInt(), // Blue
            icon = "directions_car",
            isDefault = true
        ),
        CategoryEntity(
            id = 3,
            name = "خرید",
            type = CategoryType.EXPENSE,
            color = 0xFF9C27B0.toInt(), // Purple
            icon = "shopping_bag",
            isDefault = true
        ),
        CategoryEntity(
            id = 4,
            name = "قبوض",
            type = CategoryType.EXPENSE,
            color = 0xFFFFC107.toInt(), // Amber
            icon = "receipt",
            isDefault = true
        ),
        CategoryEntity(
            id = 5,
            name = "سلامت",
            type = CategoryType.EXPENSE,
            color = 0xFFE91E63.toInt(), // Pink
            icon = "medical_services",
            isDefault = true
        ),
        CategoryEntity(
            id = 6,
            name = "تفریح",
            type = CategoryType.EXPENSE,
            color = 0xFF00BCD4.toInt(), // Cyan
            icon = "sports_esports",
            isDefault = true
        ),
        CategoryEntity(
            id = 7,
            name = "مسکن",
            type = CategoryType.EXPENSE,
            color = 0xFF795548.toInt(), // Brown
            icon = "home",
            isDefault = true
        ),
        CategoryEntity(
            id = 8,
            name = "سایر هزینه",
            type = CategoryType.EXPENSE,
            color = 0xFF607D8B.toInt(), // Blue Grey
            icon = "more_horiz",
            isDefault = true
        ),

        // Income Categories (4)
        CategoryEntity(
            id = 9,
            name = "حقوق",
            type = CategoryType.INCOME,
            color = 0xFF4CAF50.toInt(), // Green
            icon = "account_balance_wallet",
            isDefault = true
        ),
        CategoryEntity(
            id = 10,
            name = "هدیه",
            type = CategoryType.INCOME,
            color = 0xFFFF9800.toInt(), // Deep Orange
            icon = "card_giftcard",
            isDefault = true
        ),
        CategoryEntity(
            id = 11,
            name = "فروش",
            type = CategoryType.INCOME,
            color = 0xFF8BC34A.toInt(), // Light Green
            icon = "store",
            isDefault = true
        ),
        CategoryEntity(
            id = 12,
            name = "سایر درآمد",
            type = CategoryType.INCOME,
            color = 0xFF009688.toInt(), // Teal
            icon = "attach_money",
            isDefault = true
        )
    )

    val defaultAccount = AccountEntity(
        id = 1,
        name = "کارت اصلی",
        type = AccountType.BANK,
        initialBalance = 0L,
        color = 0xFF1B5E20.toInt(), // Dark Green
        icon = "credit_card"
    )

    suspend fun insertDefaults(accountDao: AccountDao, categoryDao: CategoryDao) {
        accountDao.insert(defaultAccount)
        categoryDao.insertAll(defaultCategories)
    }
}
