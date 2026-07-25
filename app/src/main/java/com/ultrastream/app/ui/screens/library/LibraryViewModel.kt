package com.ultrastream.app.ui.screens.library

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.dao.LibraryDao
import com.ultrastream.app.data.dao.SmartPlaylistDao
import com.ultrastream.app.data.dao.WatchlistDao
import com.ultrastream.app.data.models.*
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.utils.LinkVerifier
import com.ultrastream.app.utils.M3UExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao,
    private val smartPlaylistDao: SmartPlaylistDao,
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager,
    private val linkVerifier: LinkVerifier,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val episodeListType = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
    private val episodeAdapter = moshi.adapter<List<PlaylistEpisode>>(episodeListType)

    init {
        loadLibraryData()
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val library = libraryDao.getAll()
            val watchlist = watchlistDao.getAll()
            val history = historyDao.getAll()
            val smartPlaylists = smartPlaylistDao.getAll()
            _uiState.value = _uiState.value.copy(
                library = library,
                watchlist = watchlist,
                history = history,
                smartPlaylists = smartPlaylists,
                isLoading = false
            )
        }
    }

    fun refresh() = loadLibraryData()

    // --- Smart Playlist Functions ---

    fun exportPlaylistM3U(playlist: SmartPlaylist) {
        viewModelScope.launch {
            try {
                val episodes = parsePlaylistEpisodes(playlist)
                val workingStreams = episodes.mapNotNull { it.stream }.filter { stream ->
                    val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                    !url.isNullOrBlank() && !url.startsWith("magnet:")
                }
                if (workingStreams.isEmpty()) {
                    Toast.makeText(context, "No valid streams to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val exporter = M3UExporter(context)
                val file = exporter.exportToM3U(workingStreams, playlist.metaName, "playlist_${playlist.id}.m3u")
                if (file != null) {
                    exporter.shareM3U(file)
                } else {
                    Toast.makeText(context, "Failed to create M3U", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting playlist: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playAll(playlist: SmartPlaylist) {
        viewModelScope.launch {
            val episodes = parsePlaylistEpisodes(playlist)
            val firstWorking = episodes.firstOrNull { it.stream != null }?.stream
            if (firstWorking != null) {
                _uiState.value = _uiState.value.copy(playStream = firstWorking to playlist.metaName)
            } else {
                Toast.makeText(context, "No playable streams", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun parsePlaylistEpisodes(playlist: SmartPlaylist): List<PlaylistEpisode> {
        return try {
            episodeAdapter.fromJson(playlist.episodesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun retryMissingEpisodes(playlist: SmartPlaylist) {
        viewModelScope.launch {
            Toast.makeText(context, "Retrying missing episodes...", Toast.LENGTH_SHORT).show()
            val episodes = parsePlaylistEpisodes(playlist)
            val missingIndices = episodes.mapIndexedNotNull { index, ep -> if (ep.isMissing) index else null }
            if (missingIndices.isEmpty()) {
                Toast.makeText(context, "No missing episodes to retry", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val addonUrls = addonRepository.getEnabledAddons().map { it.url }
            val hindiPriority = preferencesManager.getHindiPriority().first()
            val debridKey = preferencesManager.getDebridKey().first()

            val updatedEpisodes = episodes.toMutableList()
            for (index in missingIndices) {
                val ep = updatedEpisodes[index]
                val season = playlist.season
                val epNum = ep.epNum

                val streams = streamRepository.getStreams(
                    metaId = playlist.metaId,
                    metaType = "series",
                    season = season,
                    episode = epNum,
                    addonUrls = addonUrls,
                    hindiPriority = hindiPriority,
                    debridKey = debridKey.takeIf { it.isNotBlank() }
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

                updatedEpisodes[index] = updatedEpisodes[index].copy(
                    stream = bestWorkingStream,
                    isMissing = bestWorkingStream == null
                )
            }

            val newJson = episodeAdapter.toJson(updatedEpisodes)
            val fetchedCount = updatedEpisodes.count { !it.isMissing }
            smartPlaylistDao.updatePlaylist(
                id = playlist.id,
                fetched = fetchedCount,
                status = if (fetchedCount == playlist.total) "Ready" else "Partial",
                episodesJson = newJson
            )

            loadLibraryData()
            Toast.makeText(context, "Retry completed. ${fetchedCount}/${playlist.total} episodes found.", Toast.LENGTH_LONG).show()
        }
    }

    fun manualPickEpisode(playlist: SmartPlaylist, episode: PlaylistEpisode) {
        viewModelScope.launch {
            val addonUrls = addonRepository.getEnabledAddons().map { it.url }
            val hindiPriority = preferencesManager.getHindiPriority().first()
            val debridKey = preferencesManager.getDebridKey().first()

            val streams = streamRepository.getStreams(
                metaId = playlist.metaId,
                metaType = "series",
                season = playlist.season,
                episode = episode.epNum,
                addonUrls = addonUrls,
                hindiPriority = hindiPriority,
                debridKey = debridKey.takeIf { it.isNotBlank() }
            )

            if (streams.isNotEmpty()) {
                var selectedStream: StreamItem? = null
                for (stream in streams) {
                    val sUrl = stream.url ?: stream.streamUrl ?: stream.externalUrl
                    if (sUrl != null && !sUrl.startsWith("magnet:")) {
                        if (linkVerifier.verifyLink(sUrl)) {
                            selectedStream = stream
                            break
                        }
                    }
                }
                if (selectedStream != null) {
                    val episodes = parsePlaylistEpisodes(playlist).toMutableList()
                    val index = episodes.indexOfFirst { it.epNum == episode.epNum }
                    if (index != -1) {
                        episodes[index] = episodes[index].copy(stream = selectedStream, isMissing = false)
                        val newJson = episodeAdapter.toJson(episodes)
                        smartPlaylistDao.updatePlaylist(
                            id = playlist.id,
                            fetched = episodes.count { !it.isMissing },
                            status = if (episodes.none { it.isMissing }) "Ready" else "Partial",
                            episodesJson = newJson
                        )
                        loadLibraryData()
                        Toast.makeText(context, "Episode E${episode.epNum} updated manually", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No working stream found for manual pick", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No streams available for this episode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playEpisode(episode: PlaylistEpisode) {
        val stream = episode.stream
        if (stream != null) {
            _uiState.value = _uiState.value.copy(playStream = stream to episode.epName)
        } else {
            Toast.makeText(context, "No stream available", Toast.LENGTH_SHORT).show()
        }
    }

    data class LibraryUiState(
        val isLoading: Boolean = false,
        val library: List<LibraryItem> = emptyList(),
        val watchlist: List<WatchlistItem> = emptyList(),
        val history: List<HistoryItem> = emptyList(),
        val smartPlaylists: List<SmartPlaylist> = emptyList(),
        val playStream: Pair<StreamItem, String>? = null
    )
}
