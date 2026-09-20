package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import com.example.data.local.relation.AccountWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: Long): Flow<AccountEntity?>

    @Query("""
        SELECT 
          a.*,
          (
            a.initialBalance 
            + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'INCOME'), 0)
            - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'EXPENSE'), 0)
            + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.toAccountId = a.id AND t.type = 'TRANSFER'), 0)
            - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'TRANSFER'), 0)
          ) AS currentBalance,
          (
            SELECT COUNT(*) FROM transactions t WHERE t.accountId = a.id OR t.toAccountId = a.id
          ) AS transactionCount
        FROM accounts a
        WHERE a.id = :id
    """)
    fun getAccountWithBalance(id: Long): Flow<AccountWithBalance?>

    @Query("""
        SELECT 
          a.*,
          (
            a.initialBalance 
            + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'INCOME'), 0)
            - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'EXPENSE'), 0)
            + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.toAccountId = a.id AND t.type = 'TRANSFER'), 0)
            - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'TRANSFER'), 0)
          ) AS currentBalance,
          (
            SELECT COUNT(*) FROM transactions t WHERE t.accountId = a.id OR t.toAccountId = a.id
          ) AS transactionCount
        FROM accounts a
        ORDER BY a.id ASC
    """)
    fun getAllWithBalance(): Flow<List<AccountWithBalance>>

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
