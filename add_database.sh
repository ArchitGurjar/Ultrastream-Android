#!/bin/bash
cd ~/projects/Ultrastreaming || exit

mkdir -p app/src/main/java/com/ultrastream/app/data/database
mkdir -p app/src/main/java/com/ultrastream/app/data/dao

echo "📁 Creating Converters.kt..."
cat > app/src/main/java/com/ultrastream/app/data/database/Converters.kt <<'EOF'
package com.ultrastream.app.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.*

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromCatalogList(value: List<Catalog>): String {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toCatalogList(value: String): List<Catalog> {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeList(value: List<PlaylistEpisode>): String {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<PlaylistEpisode> {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStreamItem(value: StreamItem?): String? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStreamItem(value: String?): StreamItem? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.fromJson(value)
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/data/database/Converters.kt

echo "📁 Creating AppDatabase.kt..."
cat > app/src/main/java/com/ultrastream/app/data/database/AppDatabase.kt <<'EOF'
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
EOF
git add app/src/main/java/com/ultrastream/app/data/database/AppDatabase.kt

echo "📁 Creating AddonDao.kt..."
cat > app/src/main/java/com/ultrastream/app/data/dao/AddonDao.kt <<'EOF'
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.Addon

@Dao
interface AddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(addon: Addon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addons: List<Addon>)

    @Query("SELECT * FROM addons")
    suspend fun getAll(): List<Addon>

    @Query("SELECT * FROM addons WHERE id = :id")
    suspend fun getById(id: String): Addon?

    @Query("DELETE FROM addons WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE addons SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM addons")
    suspend fun deleteAll()
}
EOF
git add app/src/main/java/com/ultrastream/app/data/dao/AddonDao.kt

echo "✅ All database files created and staged successfully!"
