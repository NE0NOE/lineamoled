package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LineMaskDao {
    @Query("SELECT * FROM line_masks ORDER BY createdAt ASC")
    fun observeAllLines(): Flow<List<LineMaskEntity>>

    @Query("SELECT * FROM line_masks ORDER BY createdAt ASC")
    suspend fun getAllLinesOnce(): List<LineMaskEntity>

    @Query("SELECT COUNT(*) FROM line_masks")
    suspend fun getLineCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: LineMaskEntity): Long

    @Update
    suspend fun updateLine(line: LineMaskEntity)

    @Delete
    suspend fun deleteLine(line: LineMaskEntity)

    @Query("DELETE FROM line_masks WHERE id = :id")
    suspend fun deleteLineById(id: Long)
}
