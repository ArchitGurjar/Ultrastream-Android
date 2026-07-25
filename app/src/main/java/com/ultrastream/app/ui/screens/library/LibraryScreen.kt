package com.ultrastream.app.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.PlaylistEpisode
import com.ultrastream.app.data.models.SmartPlaylist
import com.ultrastream.app.ui.components.GridSection
import com.ultrastream.app.ui.components.HScrollRow
import com.ultrastream.app.ui.components.SectionHeader
import com.ultrastream.app.ui.components.SmartPlaylistCard
import com.ultrastream.app.ui.components.bottomsheets.SmartPlaylistDetailSheet
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPlaylist by remember { mutableStateOf<SmartPlaylist?>(null) }
    var showPlaylistDetail by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Smart Playlists
            item {
                SectionHeader(title = "Smart Playlists")
                if (uiState.smartPlaylists.isEmpty()) {
                    Text("No smart playlists", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    HScrollRow {
                        uiState.smartPlaylists.forEach { playlist ->
                            SmartPlaylistCard(
                                playlist = playlist,
                                onClick = {
                                    selectedPlaylist = playlist
                                    showPlaylistDetail = true
                                },
                                onExportM3u = { pl ->
                                    scope.launch {
                                        viewModel.exportPlaylistM3U(pl)
                                    }
                                },
                                onPlayAll = { pl ->
                                    scope.launch {
                                        viewModel.playAll(pl)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Library
            item {
                SectionHeader(title = "Library")
                if (uiState.library.isEmpty()) {
                    Text("No saved items", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.library.map { lib ->
                        MetaItem(
                            id = lib.id,
                            type = lib.type,
                            name = lib.name,
                            poster = lib.poster,
                            background = lib.background,
                            imdbRating = lib.imdbRating,
                            year = lib.year,
                            releaseInfo = lib.releaseInfo,
                            released = lib.released,
                            description = lib.description,
                            genre = lib.genre?.split(","),
                            runtime = lib.runtime,
                            cast = lib.cast?.split(","),
                            imdbId = lib.imdbId,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }

            // Watchlist
            item {
                SectionHeader(title = "Watchlist")
                if (uiState.watchlist.isEmpty()) {
                    Text("No watchlist items", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.watchlist.map { wl ->
                        MetaItem(
                            id = wl.id,
                            type = wl.type,
                            name = wl.name,
                            poster = wl.poster,
                            background = wl.background,
                            imdbRating = wl.imdbRating,
                            year = wl.year,
                            releaseInfo = wl.releaseInfo,
                            released = wl.released,
                            description = wl.description,
                            genre = wl.genre?.split(","),
                            runtime = wl.runtime,
                            cast = wl.cast?.split(","),
                            imdbId = wl.imdbId,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }

            // History
            item {
                SectionHeader(title = "History")
                if (uiState.history.isEmpty()) {
                    Text("No history", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.history.map { hist ->
                        MetaItem(
                            id = hist.id,
                            type = "movie",
                            name = hist.name,
                            poster = hist.poster,
                            background = null,
                            imdbRating = null,
                            year = null,
                            releaseInfo = null,
                            released = null,
                            description = null,
                            genre = null,
                            runtime = null,
                            cast = null,
                            imdbId = null,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }
        }
    }

    // Smart Playlist Detail Sheet
    if (showPlaylistDetail && selectedPlaylist != null) {
        val playlist = selectedPlaylist!!
        // Parse episodes from playlist JSON using viewModel
        val episodes = remember(playlist) {
            viewModel.parsePlaylistEpisodes(playlist)
        }
        SmartPlaylistDetailSheet(
            playlist = playlist,
            episodes = episodes,
            onDismiss = { showPlaylistDetail = false },
            onRetryMissing = {
                scope.launch {
                    viewModel.retryMissingEpisodes(playlist)
                    showPlaylistDetail = false
                }
            },
            onManualPick = { episode ->
                scope.launch {
                    viewModel.manualPickEpisode(playlist, episode)
                    // Don't dismiss to allow user to see the pick result, but we can close after selection
                    // For simplicity, we'll close after selection, or we could open another sheet.
                    // We'll just call the viewModel and keep sheet open until user dismisses manually.
                }
            },
            onPlayEpisode = { episode ->
                scope.launch {
                    viewModel.playEpisode(episode)
                    showPlaylistDetail = false
                }
            }
        )
    }
}
