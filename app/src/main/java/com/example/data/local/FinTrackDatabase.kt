package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        com.example.data.local.entity.DebtEntity::class,
        com.example.data.local.entity.SmsPatternEntity::class,
        com.example.data.local.entity.ChequeEntity::class,
        com.example.data.local.entity.RecurringTransactionEntity::class,
        com.example.data.local.entity.SavingsGoalEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class FinTrackDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): com.example.data.local.dao.DebtDao
    abstract fun smsPatternDao(): com.example.data.local.dao.SmsPatternDao
    abstract fun chequeDao(): com.example.data.local.dao.ChequeDao
    abstract fun recurringDao(): com.example.data.local.dao.RecurringDao
    abstract fun savingsGoalDao(): com.example.data.local.dao.SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: FinTrackDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN logoResName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN logoImage TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN monthlyBudget INTEGER NOT NULL DEFAULT 0")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cheques` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sayadNumber` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `issueDate` INTEGER,
                        `partyName` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `accountId` INTEGER,
                        `status` TEXT NOT NULL,
                        `note` TEXT,
                        `clearedDate` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER,
                        `categoryId` INTEGER,
                        `period` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `nextDueDate` INTEGER NOT NULL,
                        `lastExecutedDate` INTEGER,
                        `isInstallment` INTEGER NOT NULL,
                        `totalInstallments` INTEGER,
                        `paidInstallments` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_toAccountId` ON `recurring_transactions` (`toAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` INTEGER NOT NULL,
                        `currentAmount` INTEGER NOT NULL,
                        `targetDate` INTEGER,
                        `color` INTEGER NOT NULL,
                        `icon` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): FinTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinTrackDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                SeedData.insertDefaults(
                                    accountDao = database.accountDao(),
                                    categoryDao = database.categoryDao()
                                )
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
