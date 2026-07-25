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
