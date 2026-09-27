package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.ChequeStatus
import com.example.data.local.entity.ChequeType
import kotlinx.coroutines.flow.Flow

@Dao
interface ChequeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cheque: ChequeEntity): Long

    @Update
    suspend fun update(cheque: ChequeEntity)

    @Delete
    suspend fun delete(cheque: ChequeEntity)

    @Query("DELETE FROM cheques WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM cheques WHERE id = :id")
    fun getById(id: Long): Flow<ChequeEntity?>

    @Query("SELECT * FROM cheques ORDER BY status ASC, dueDate ASC")
    fun getAll(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE type = :type ORDER BY status ASC, dueDate ASC")
    fun getByType(type: ChequeType): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE status = 'PENDING' ORDER BY dueDate ASC")
    fun getPending(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE status = 'PENDING' AND dueDate <= :timestamp ORDER BY dueDate ASC")
    fun getDueBefore(timestamp: Long): Flow<List<ChequeEntity>>

    @Query("UPDATE cheques SET status = :status, clearedDate = :clearedDate WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ChequeStatus, clearedDate: Long?)

    @Query("DELETE FROM cheques")
    suspend fun deleteAll()
}
