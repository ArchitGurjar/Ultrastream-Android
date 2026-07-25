package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.LibraryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.dao.LibraryDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageLibraryUseCase @Inject constructor(
    private val libraryDao: LibraryDao
) {
    suspend fun addToLibrary(meta: MetaItem) {
        val item = LibraryItem(
            id = meta.id,
            type = meta.type,
            name = meta.name,
            poster = meta.poster,
            background = meta.background,
            imdbRating = meta.imdbRating,
            year = meta.year,
            releaseInfo = meta.releaseInfo,
            released = meta.released,
            description = meta.description,
            genre = meta.genre?.joinToString(","),
            runtime = meta.runtime,
            cast = meta.cast?.joinToString(","),
            imdbId = meta.imdbId,
            timestamp = System.currentTimeMillis()
        )
        libraryDao.insert(item)
    }

    suspend fun removeFromLibrary(id: String) {
        val item = libraryDao.getById(id) ?: return
        libraryDao.delete(item)
    }

    suspend fun isInLibrary(id: String): Boolean {
        return libraryDao.getById(id) != null
    }

    suspend fun getAllLibrary(): List<LibraryItem> = libraryDao.getAll()
}
