package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.CachedMeta

@Dao
interface CachedMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: CachedMeta)

    @Query("SELECT * FROM cached_meta WHERE cacheKey = :key")
    suspend fun getByKey(key: String): CachedMeta?

    @Query("DELETE FROM cached_meta")
    suspend fun deleteAll()

    @Query("SELECT * FROM cached_meta")
    suspend fun getAll(): List<CachedMeta>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metas: List<CachedMeta>)
}
