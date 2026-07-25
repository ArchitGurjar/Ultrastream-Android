package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val id: String,
    val percent: Int,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Entity(tableName = "watched_episodes")
data class WatchedEpisode(
    @PrimaryKey val episodeKey: String,
    val watched: Boolean = true
)
