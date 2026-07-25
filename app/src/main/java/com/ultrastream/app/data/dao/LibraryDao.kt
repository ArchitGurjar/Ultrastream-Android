package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.LibraryItem

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LibraryItem>)

    @Query("SELECT * FROM library")
    suspend fun getAll(): List<LibraryItem>

    @Query("SELECT * FROM library WHERE id = :id")
    suspend fun getById(id: String): LibraryItem?

    @Delete
    suspend fun delete(item: LibraryItem)

    @Query("DELETE FROM library")
    suspend fun deleteAll()
}
