package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

data class RawCategorySummary(
    @Embedded val category: CategoryEntity,
    val totalAmount: Long,
    val transactionCount: Int
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getAllWithDetails(): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.id = :id
    """)
    fun getByIdWithDetails(id: Long): Flow<TransactionWithDetails?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): Flow<TransactionEntity?>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
        LIMIT :limit
    """)
    fun getRecent(limit: Int): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.accountId = :accountId OR t.toAccountId = :accountId
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getByAccount(accountId: Long): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :start AND t.date <= :end
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'INCOME' AND date >= :todayStart AND date <= :todayEnd
    """)
    fun getTodayIncome(todayStart: Long, todayEnd: Long): Flow<Long?>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' AND date >= :todayStart AND date <= :todayEnd
    """)
    fun getTodayExpense(todayStart: Long, todayEnd: Long): Flow<Long?>

    @Query("""
        SELECT 
          c.*,
          COALESCE(SUM(t.amount), 0) AS totalAmount,
          COUNT(t.id) AS transactionCount
        FROM categories c
        INNER JOIN transactions t ON t.categoryId = c.id
        WHERE t.date >= :start AND t.date <= :end AND t.type = :type
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategorySummaryRaw(start: Long, end: Long, type: TransactionType): Flow<List<RawCategorySummary>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'INCOME' AND date >= :start AND date <= :end
    """)
    fun getTotalIncome(start: Long, end: Long): Flow<Long?>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' AND date >= :start AND date <= :end
    """)
    fun getTotalExpense(start: Long, end: Long): Flow<Long?>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT smsHash FROM transactions WHERE smsHash IS NOT NULL")
    fun getAllSmsHashes(): Flow<List<String>>

    @Query("SELECT smsHash FROM transactions WHERE smsHash IS NOT NULL")
    suspend fun getAllSmsHashesList(): List<String>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
    """)
    suspend fun getAllWithDetailsList(): List<TransactionWithDetails>

    @Query("""
        SELECT 
          t.*,
          a1.name AS accountName,
          a2.name AS toAccountName,
          c.name AS categoryName,
          c.color AS categoryColor,
          c.icon AS categoryIcon,
          a1.logoResName AS accountLogo,
          a1.cardNumber AS accountCardNumber
        FROM transactions t
        LEFT JOIN accounts a1 ON t.accountId = a1.id
        LEFT JOIN accounts a2 ON t.toAccountId = a2.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.amount = :amount AND t.date >= :dayStart AND t.date <= :dayEnd
    """)
    suspend fun findMatchingTransactions(amount: Long, dayStart: Long, dayEnd: Long): List<TransactionWithDetails>
}
