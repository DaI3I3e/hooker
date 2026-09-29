package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SavingsGoalEntryEntity
import com.example.data.local.relation.SavingsGoalWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getById(id: Long): Flow<SavingsGoalWithDetails?>

    @Transaction
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SavingsGoalWithDetails>>

    @Query("SELECT * FROM savings_goals")
    fun getAllGoalsRaw(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SavingsGoalEntryEntity): Long

    @Query("SELECT * FROM savings_goal_entries WHERE goalId = :goalId ORDER BY date DESC")
    fun getEntriesForGoal(goalId: Long): Flow<List<SavingsGoalEntryEntity>>

    @Query("SELECT * FROM savings_goal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<SavingsGoalEntryEntity>>

    @Delete
    suspend fun deleteEntry(entry: SavingsGoalEntryEntity)

    @Query("DELETE FROM savings_goal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM savings_goal_entries WHERE transactionId = :transactionId")
    suspend fun deleteEntryByTransactionId(transactionId: Long)

    @Query("UPDATE savings_goals SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletedStatus(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM savings_goal_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}
