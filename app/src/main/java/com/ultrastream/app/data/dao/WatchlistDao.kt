package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.WatchlistItem

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchlistItem>)

    @Query("SELECT * FROM watchlist")
    suspend fun getAll(): List<WatchlistItem>

    @Query("SELECT * FROM watchlist WHERE id = :id")
    suspend fun getById(id: String): WatchlistItem?

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM watchlist")
    suspend fun deleteAll()
}
