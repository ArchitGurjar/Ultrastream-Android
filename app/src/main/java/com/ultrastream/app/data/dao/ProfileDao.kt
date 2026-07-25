package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<Profile>)

    @Query("SELECT * FROM profiles")
    suspend fun getAll(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): Profile?

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}
