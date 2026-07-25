package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.HistoryItem

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryItem>)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryItem>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: String): HistoryItem?

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
