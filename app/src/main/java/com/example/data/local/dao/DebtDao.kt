package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)

    @Query("SELECT * FROM debts ORDER BY isSettled ASC, createdAt DESC")
    fun getAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY isSettled ASC, createdAt DESC")
    fun getByType(type: DebtType): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun getById(id: Long): Flow<DebtEntity?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debts WHERE type = 'OWED_TO_ME' AND isSettled = 0")
    fun getTotalOwedToMe(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debts WHERE type = 'I_OWE' AND isSettled = 0")
    fun getTotalIOwe(): Flow<Long>
}
