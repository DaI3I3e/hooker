package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SmsPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: SmsPatternEntity): Long

    @Update
    suspend fun update(pattern: SmsPatternEntity)

    @Delete
    suspend fun delete(pattern: SmsPatternEntity)

    @Query("DELETE FROM sms_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sms_patterns ORDER BY id DESC")
    fun getAll(): Flow<List<SmsPatternEntity>>

    @Query("SELECT * FROM sms_patterns WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActive(): Flow<List<SmsPatternEntity>>

    @Query("SELECT * FROM sms_patterns WHERE isActive = 1 ORDER BY id DESC")
    suspend fun getAllActiveList(): List<SmsPatternEntity>

    @Query("SELECT * FROM sms_patterns WHERE accountIdentifier = :identifier ORDER BY id DESC")
    fun getByAccountIdentifier(identifier: String): Flow<List<SmsPatternEntity>>

    @Query("SELECT * FROM sms_patterns WHERE id = :id")
    fun getById(id: Long): Flow<SmsPatternEntity?>
}
