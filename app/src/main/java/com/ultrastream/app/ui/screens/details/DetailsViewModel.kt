package com.ultrastream.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.*
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getMetaUseCase: GetMetaUseCase,
    private val getStreamsUseCase: GetStreamsUseCase,
    private val resolveStreamUseCase: ResolveStreamUseCase,
    private val createSmartPlaylistUseCase: CreateSmartPlaylistUseCase,
    private val fetchSubtitlesUseCase: FetchSubtitlesUseCase,
    private val manageLibraryUseCase: ManageLibraryUseCase,
    private val manageWatchlistUseCase: ManageWatchlistUseCase,
    private val updateWatchProgressUseCase: UpdateWatchProgressUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _filteredEpisodes = MutableStateFlow<List<Video>>(emptyList())
    val filteredEpisodes: StateFlow<List<Video>> = _filteredEpisodes.asStateFlow()

    private val _availableSeasons = MutableStateFlow<List<Int>>(emptyList())
    val availableSeasons: StateFlow<List<Int>> = _availableSeasons.asStateFlow()

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason: StateFlow<Int?> = _selectedSeason.asStateFlow()

    private val _isAllSeasons = MutableStateFlow(false)
    val isAllSeasons: StateFlow<Boolean> = _isAllSeasons.asStateFlow()

    fun loadMeta(id: String, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val meta = getMetaUseCase(id, type)
            if (meta != null) {
                val inLibrary = manageLibraryUseCase.isInLibrary(id)
                val inWatchlist = manageWatchlistUseCase.isInWatchlist(id)
                _uiState.value = _uiState.value.copy(
                    meta = meta,
                    inLibrary = inLibrary,
                    inWatchlist = inWatchlist,
                    isLoading = false,
                    error = null
                )
                filterAndSortEpisodes(meta.videos)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Meta not found"
                )
            }
        }
    }

    private fun filterAndSortEpisodes(episodes: List<Video>?) {
        if (episodes == null) {
            _filteredEpisodes.value = emptyList()
            _availableSeasons.value = emptyList()
            return
        }
        val seen = mutableSetOf<String>()
        val seasonMap = mutableMapOf<Int, MutableList<Video>>()

        episodes.forEach { ep ->
            if (ep.season == null || ep.episode == null) return@forEach
            if (ep.season == 0 || ep.episode == 0) return@forEach
            val name = ep.name ?: ep.title ?: ""
            if (listOf(480, 720, 1080, 2160, 264, 265).contains(ep.episode) && name.isBlank()) return@forEach
            val key = "S${ep.season}E${ep.episode}"
            if (seen.contains(key)) return@forEach
            seen.add(key)
            seasonMap.getOrPut(ep.season) { mutableListOf() }.add(ep)
        }

        seasonMap.values.forEach { list -> list.sortBy { it.episode ?: 0 } }
        val all = seasonMap.keys.sorted().flatMap { seasonMap[it] ?: emptyList() }
        val seasons = all.mapNotNull { it.season }.distinct().sorted()
        _availableSeasons.value = seasons
        _filteredEpisodes.value = all
        if (seasons.isNotEmpty() && _selectedSeason.value == null && !_isAllSeasons.value) {
            _selectedSeason.value = seasons.first()
        }
        applySeasonFilter()
    }

    fun toggleAllSeasons() {
        _isAllSeasons.value = !_isAllSeasons.value
        if (_isAllSeasons.value) {
            _selectedSeason.value = null
        } else {
            val seasons = _availableSeasons.value
            if (seasons.isNotEmpty()) _selectedSeason.value = seasons.first()
        }
        applySeasonFilter()
    }

    fun selectSeason(season: Int?) {
        _selectedSeason.value = season
        if (season != null) _isAllSeasons.value = false
        applySeasonFilter()
    }

    private fun applySeasonFilter() {
        val all = _filteredEpisodes.value
        if (all.isEmpty()) return
        val result = if (_isAllSeasons.value || _selectedSeason.value == null) {
            all
        } else {
            all.filter { it.season == _selectedSeason.value }
        }
        _filteredEpisodes.value = result
    }

    fun selectSeasonAndLoad(season: Int?) {
        selectSeason(season)
        loadStreamsForCurrentSelection()
    }

    fun selectEpisode(episode: Int) {
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
        loadStreamsForCurrentSelection()
    }

    private fun loadStreamsForCurrentSelection() {
        val meta = _uiState.value.meta ?: return
        loadStreams(meta.id, meta.type, _selectedSeason.value, _uiState.value.selectedEpisode)
    }

    fun loadStreams(id: String, type: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(streamsLoading = true, streams = emptyList())
            try {
                val streams = getStreamsUseCase(id, type, season, episode)
                _uiState.value = _uiState.value.copy(
                    streams = streams,
                    streamsLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    streams = emptyList(),
                    streamsLoading = false,
                    error = e.message ?: "Failed to load streams"
                )
            }
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        viewModelScope.launch {
            val current = _uiState.value.inLibrary
            if (current) {
                manageLibraryUseCase.removeFromLibrary(meta.id)
                _uiState.value = _uiState.value.copy(inLibrary = false)
            } else {
                manageLibraryUseCase.addToLibrary(meta)
                _uiState.value = _uiState.value.copy(inLibrary = true)
            }
        }
    }

    fun toggleWatchlist(meta: MetaItem) {
        viewModelScope.launch {
            val current = _uiState.value.inWatchlist
            if (current) {
                manageWatchlistUseCase.removeFromWatchlist(meta.id)
                _uiState.value = _uiState.value.copy(inWatchlist = false)
            } else {
                manageWatchlistUseCase.addToWatchlist(meta)
                _uiState.value = _uiState.value.copy(inWatchlist = true)
            }
        }
    }

    fun playStream(stream: StreamItem, title: String, onResolved: (StreamItem, String) -> Unit) {
        viewModelScope.launch {
            val resolved = resolveStreamUseCase(stream)
            onResolved(resolved, title)
        }
    }

    suspend fun createSmartPlaylist(meta: MetaItem, season: Int): Boolean {
        return createSmartPlaylistUseCase(meta, season)
    }

    suspend fun fetchSubtitles(metaId: String, type: String, season: Int, episode: Int): List<Subtitle> {
        return fetchSubtitlesUseCase(metaId, type, season, episode)
    }

    suspend fun updateProgress(id: String, percent: Int) {
        updateWatchProgressUseCase.updateProgress(id, percent)
    }

    suspend fun addToHistory(metaId: String, type: String, name: String, poster: String?) {
        updateWatchProgressUseCase.addToHistory(metaId, type, name, poster)
    }

    data class DetailsUiState(
        val isLoading: Boolean = false,
        val meta: MetaItem? = null,
        val inLibrary: Boolean = false,
        val inWatchlist: Boolean = false,
        val watchProgress: WatchProgress? = null,
        val error: String? = null,
        val streams: List<StreamItem> = emptyList(),
        val streamsLoading: Boolean = false,
        val selectedSeason: Int? = null,
        val selectedEpisode: Int? = null
    )
}
