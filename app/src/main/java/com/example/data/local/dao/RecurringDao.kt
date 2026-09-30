package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.relation.RecurringWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransactionEntity): Long

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun delete(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getById(id: Long): Flow<RecurringTransactionEntity?>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getByIdSync(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions")
    fun getAllEntities(): Flow<List<RecurringTransactionEntity>>

    @Transaction
    @Query("""
        SELECT r.*,
               a.name AS accountName,
               a2.name AS toAccountName,
               c.name AS categoryName,
               c.color AS categoryColor,
               c.icon AS categoryIcon
        FROM recurring_transactions r
        LEFT JOIN accounts a ON r.accountId = a.id
        LEFT JOIN accounts a2 ON r.toAccountId = a2.id
        LEFT JOIN categories c ON r.categoryId = c.id
        ORDER BY r.isActive DESC, r.nextDueDate ASC
    """)
    fun getAll(): Flow<List<RecurringWithDetails>>

    @Transaction
    @Query("""
        SELECT r.*,
               a.name AS accountName,
               a2.name AS toAccountName,
               c.name AS categoryName,
               c.color AS categoryColor,
               c.icon AS categoryIcon
        FROM recurring_transactions r
        LEFT JOIN accounts a ON r.accountId = a.id
        LEFT JOIN accounts a2 ON r.toAccountId = a2.id
        LEFT JOIN categories c ON r.categoryId = c.id
        WHERE r.isActive = 1
        ORDER BY r.nextDueDate ASC
    """)
    fun getAllActive(): Flow<List<RecurringWithDetails>>

    @Transaction
    @Query("""
        SELECT r.*,
               a.name AS accountName,
               a2.name AS toAccountName,
               c.name AS categoryName,
               c.color AS categoryColor,
               c.icon AS categoryIcon
        FROM recurring_transactions r
        LEFT JOIN accounts a ON r.accountId = a.id
        LEFT JOIN accounts a2 ON r.toAccountId = a2.id
        LEFT JOIN categories c ON r.categoryId = c.id
        WHERE r.isActive = 1 AND r.nextDueDate <= :timestamp
        ORDER BY r.nextDueDate ASC
    """)
    fun getDueBefore(timestamp: Long): Flow<List<RecurringWithDetails>>

    @Query("""
        UPDATE recurring_transactions 
        SET nextDueDate = :nextDueDate, 
            lastExecutedDate = :lastExecutedDate, 
            paidInstallments = :paidInstallments, 
            isActive = :isActive 
        WHERE id = :id
    """)
    suspend fun updateExecution(
        id: Long,
        nextDueDate: Long,
        lastExecutedDate: Long,
        paidInstallments: Int,
        isActive: Boolean
    )

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
