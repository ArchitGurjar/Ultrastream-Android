package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.WatchProgress
import com.ultrastream.app.data.models.WatchedEpisode

@Dao
interface WatchProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: WatchProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<WatchProgress>)

    @Query("SELECT * FROM watch_progress WHERE id = :id")
    suspend fun getById(id: String): WatchProgress?

    @Query("SELECT * FROM watch_progress")
    suspend fun getAll(): List<WatchProgress>

    @Delete
    suspend fun delete(progress: WatchProgress)

    @Query("DELETE FROM watch_progress")
    suspend fun deleteAll()
}

@Dao
interface WatchedEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ep: WatchedEpisode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eps: List<WatchedEpisode>)

    @Query("SELECT * FROM watched_episodes WHERE episodeKey = :key")
    suspend fun getByKey(key: String): WatchedEpisode?

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAll(): List<WatchedEpisode>

    @Delete
    suspend fun delete(ep: WatchedEpisode)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAll()
}
