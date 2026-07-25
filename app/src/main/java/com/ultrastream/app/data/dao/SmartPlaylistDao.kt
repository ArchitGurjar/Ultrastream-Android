package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.SmartPlaylist

@Dao
interface SmartPlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: SmartPlaylist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<SmartPlaylist>)

    @Query("SELECT * FROM smart_playlists")
    suspend fun getAll(): List<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE id = :id")
    suspend fun getById(id: String): SmartPlaylist?

    @Delete
    suspend fun delete(playlist: SmartPlaylist)

    @Query("DELETE FROM smart_playlists")
    suspend fun deleteAll()

    @Query("UPDATE smart_playlists SET fetched = :fetched, status = :status, episodesJson = :episodesJson WHERE id = :id")
    suspend fun updatePlaylist(id: String, fetched: Int, status: String, episodesJson: String)

    @Query("UPDATE smart_playlists SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE smart_playlists SET fetched = :fetched WHERE id = :id")
    suspend fun updateFetched(id: String, fetched: Int)
}
