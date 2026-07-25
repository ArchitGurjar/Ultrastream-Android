package com.ultrastream.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.models.*

@Database(
    entities = [
        Addon::class,
        LibraryItem::class,
        WatchlistItem::class,
        HistoryItem::class,
        CachedMeta::class,
        SmartPlaylist::class,
        Profile::class,
        WatchProgress::class,
        WatchedEpisode::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addonDao(): AddonDao
    abstract fun libraryDao(): LibraryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun historyDao(): HistoryDao
    abstract fun cachedMetaDao(): CachedMetaDao
    abstract fun smartPlaylistDao(): SmartPlaylistDao
    abstract fun profileDao(): ProfileDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
}
