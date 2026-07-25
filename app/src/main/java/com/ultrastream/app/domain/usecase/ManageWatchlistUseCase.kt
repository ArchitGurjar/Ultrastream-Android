package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.WatchlistItem
import com.ultrastream.app.data.dao.WatchlistDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageWatchlistUseCase @Inject constructor(
    private val watchlistDao: WatchlistDao
) {
    suspend fun addToWatchlist(meta: MetaItem) {
        val item = WatchlistItem(
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
        watchlistDao.insert(item)
    }

    suspend fun removeFromWatchlist(id: String) {
        val item = watchlistDao.getById(id) ?: return
        watchlistDao.delete(item)
    }

    suspend fun isInWatchlist(id: String): Boolean {
        return watchlistDao.getById(id) != null
    }

    suspend fun getAllWatchlist(): List<WatchlistItem> = watchlistDao.getAll()
}
