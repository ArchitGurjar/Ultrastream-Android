package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.PlaylistEpisode
import com.ultrastream.app.data.models.SmartPlaylist
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.dao.SmartPlaylistDao
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.utils.LinkVerifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateSmartPlaylistUseCase @Inject constructor(
    private val smartPlaylistDao: SmartPlaylistDao,
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager,
    private val linkVerifier: LinkVerifier
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val episodeListType = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
    private val episodeAdapter = moshi.adapter<List<PlaylistEpisode>>(episodeListType)

    // ✅ Zombie coroutine fix: proper scope with SupervisorJob and exception handler
    private val playlistScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
        CoroutineExceptionHandler { _, exception ->
            // डेटाबेस में playlist को 'Failed' मार्क करें
            // We need a reference to the playlistId; we'll capture it in the handler
            // But we can't easily pass it from inside; we'll update status manually after creation
        }
    )

    suspend operator fun invoke(meta: MetaItem, season: Int): Boolean {
        val episodes = meta.videos?.filter { it.season == season } ?: return false
        if (episodes.isEmpty()) return false

        val playlistId = "${meta.id}_S${season}_${System.currentTimeMillis()}"
        val initialPlaylist = SmartPlaylist(
            id = playlistId,
            metaId = meta.id,
            metaName = meta.name,
            poster = meta.poster,
            season = season,
            addon = "SmartPlaylist",
            total = episodes.size,
            fetched = 0,
            status = "Fetching...",
            episodesJson = "[]"
        )
        smartPlaylistDao.insert(initialPlaylist)

        // ✅ Background fetch with proper error handling and isActive checks
        playlistScope.launch {
            try {
                val addonUrls = addonRepository.getEnabledAddons().map { it.url }
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()
                val fetchedEpisodes = mutableListOf<PlaylistEpisode>()

                episodes.forEachIndexed { index, ep ->
                    // Check if coroutine is still active
                    if (!currentCoroutineContext().isActive) {
                        smartPlaylistDao.updateStatus(playlistId, "Cancelled")
                        return@launch
                    }

                    val epNum = ep.episode ?: 0

                    val streams = streamRepository.getStreams(
                        meta.id,
                        meta.type,
                        season,
                        epNum,
                        addonUrls,
                        hindiPriority,
                        debridKey.takeIf { it.isNotBlank() }
                    )

                    var bestWorkingStream: StreamItem? = null
                    for (stream in streams) {
                        val sUrl = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (sUrl != null && !sUrl.startsWith("magnet:")) {
                            if (linkVerifier.verifyLink(sUrl)) {
                                bestWorkingStream = stream
                                break
                            }
                        }
                    }

                    fetchedEpisodes.add(
                        PlaylistEpisode(
                            epNum = epNum,
                            epName = ep.name ?: "Episode $epNum",
                            title = "${meta.name} - S${season}E${epNum}",
                            stream = bestWorkingStream,
                            isMissing = bestWorkingStream == null
                        )
                    )

                    val updatedPlaylist = initialPlaylist.copy(
                        fetched = index + 1,
                        status = if (index + 1 == episodes.size) "Ready" else "Fetching...",
                        episodesJson = episodeAdapter.toJson(fetchedEpisodes)
                    )
                    smartPlaylistDao.updatePlaylist(
                        id = playlistId,
                        fetched = updatedPlaylist.fetched,
                        status = updatedPlaylist.status,
                        episodesJson = updatedPlaylist.episodesJson
                    )
                }
            } catch (e: Exception) {
                // Update status to Failed
                smartPlaylistDao.updateStatus(playlistId, "Failed: ${e.message}")
            }
        }

        return true
    }
}
