package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavingsGoalEntity
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

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAll(): Flow<List<SavingsGoalEntity>>

    @Query("UPDATE savings_goals SET currentAmount = :newAmount, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateAmount(id: Long, newAmount: Long, isCompleted: Boolean)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}
