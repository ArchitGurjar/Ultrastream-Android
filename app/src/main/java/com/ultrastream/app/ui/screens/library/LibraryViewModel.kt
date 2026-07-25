package com.ultrastream.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.dao.LibraryDao
import com.ultrastream.app.data.dao.WatchlistDao
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.dao.SmartPlaylistDao
import com.ultrastream.app.data.models.SmartPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao,
    private val smartPlaylistDao: SmartPlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

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

    data class LibraryUiState(
        val isLoading: Boolean = false,
        val library: List<LibraryItem> = emptyList(),
        val watchlist: List<WatchlistItem> = emptyList(),
        val history: List<HistoryItem> = emptyList(),
        val smartPlaylists: List<SmartPlaylist> = emptyList()
    )
}
