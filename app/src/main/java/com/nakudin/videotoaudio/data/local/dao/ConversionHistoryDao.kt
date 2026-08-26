package com.nakudin.videotoaudio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nakudin.videotoaudio.data.local.entity.ConversionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ConversionHistoryEntity): Long

    @Update
    suspend fun update(item: ConversionHistoryEntity)

    @Delete
    suspend fun delete(item: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversion_history")
    suspend fun deleteAll()

    /** Reactive stream of all history records, newest first. */
    @Query("SELECT * FROM conversion_history ORDER BY conversionDate DESC")
    fun observeAll(): Flow<List<ConversionHistoryEntity>>

    @Query("SELECT * FROM conversion_history WHERE id = :id")
    suspend fun getById(id: Long): ConversionHistoryEntity?
}
