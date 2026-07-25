package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library")
data class LibraryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?,
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?,
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_meta")
data class CachedMeta(
    @PrimaryKey val cacheKey: String,
    val json: String
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: List<String>?,
    val runtime: String?,
    val cast: List<String>?,
    val imdbId: String?,
    val videos: List<Video>? = null
)

data class Video(
    val season: Int?,
    val episode: Int?,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val url: String?
)
