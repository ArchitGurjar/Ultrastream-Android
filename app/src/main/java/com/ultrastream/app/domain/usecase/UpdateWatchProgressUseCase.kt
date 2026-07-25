package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.HistoryItem
import com.ultrastream.app.data.models.WatchProgress
import com.ultrastream.app.data.models.WatchedEpisode
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.dao.WatchProgressDao
import com.ultrastream.app.data.dao.WatchedEpisodeDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateWatchProgressUseCase @Inject constructor(
    private val watchProgressDao: WatchProgressDao,
    private val historyDao: HistoryDao,
    private val watchedEpisodeDao: WatchedEpisodeDao
) {
    suspend fun updateProgress(id: String, percent: Int) {
        val existing = watchProgressDao.getById(id)
        if (existing != null) {
            val updated = existing.copy(percent = percent, lastUpdate = System.currentTimeMillis())
            watchProgressDao.insert(updated)
        } else {
            watchProgressDao.insert(WatchProgress(id = id, percent = percent))
        }
    }

    suspend fun markEpisodeWatched(metaId: String, season: Int, episode: Int) {
        val key = "${metaId}_s${season}_e${episode}"
        watchedEpisodeDao.insert(WatchedEpisode(episodeKey = key, watched = true))
    }

    suspend fun isEpisodeWatched(metaId: String, season: Int, episode: Int): Boolean {
        val key = "${metaId}_s${season}_e${episode}"
        return watchedEpisodeDao.getByKey(key) != null
    }

    suspend fun addToHistory(metaId: String, type: String, name: String, poster: String?) {
        val existing = historyDao.getById(metaId)
        if (existing != null) {
            // Update timestamp
            val updated = existing.copy(timestamp = System.currentTimeMillis())
            historyDao.insert(updated)
        } else {
            historyDao.insert(
                HistoryItem(
                    id = metaId,
                    type = type,
                    name = name,
                    poster = poster,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getContinueWatching(): List<Pair<HistoryItem, Int>> {
        val history = historyDao.getAll().take(10)
        return history.mapNotNull { item ->
            val progress = watchProgressDao.getById(item.id)
            if (progress != null && progress.percent > 0) {
                item to progress.percent
            } else null
        }
    }
}
