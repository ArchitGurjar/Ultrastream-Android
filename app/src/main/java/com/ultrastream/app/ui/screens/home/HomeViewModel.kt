package com.ultrastream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.HistoryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.RecommendedAddon
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.domain.usecase.GetHomeCatalogsUseCase
import com.ultrastream.app.domain.usecase.UpdateWatchProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val getHomeCatalogsUseCase: GetHomeCatalogsUseCase,
    private val updateWatchProgressUseCase: UpdateWatchProgressUseCase,
    private val metaRepository: MetaRepository,   // ✅ Added
    private val historyDao: HistoryDao           // ✅ Added
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val continueWatching = updateWatchProgressUseCase.getContinueWatching()
            val addons = addonRepository.getEnabledAddons()
            val catalogRows = getHomeCatalogsUseCase()

            // ✅ Recommendations: based on history
            val recommendations = buildRecommendations()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                continueWatching = continueWatching,
                addons = addons,
                catalogRows = catalogRows,
                recommendedAddons = getRecommendedAddons(addons),
                recommendations = recommendations
            )
        }
    }

    private suspend fun buildRecommendations(): List<MetaItem> {
        val history = historyDao.getAll().take(5)
        if (history.isEmpty()) return emptyList()
        // For simplicity, fetch meta for the first 3 history items
        val result = mutableListOf<MetaItem>()
        for (item in history) {
            val meta = metaRepository.getMeta(item.id, item.type)
            if (meta != null) result.add(meta)
        }
        return result.distinctBy { it.id }
    }

    private fun getRecommendedAddons(installed: List<Addon>): List<RecommendedAddon> {
        val builtIn = listOf(
            RecommendedAddon("Torrentio", "Torrent scraper for movies & series", "https://torrentio.strem.fun/manifest.json"),
            RecommendedAddon("Cinemeta", "Metadata provider for movies & series", "https://cinemeta.strem.fun/manifest.json"),
            RecommendedAddon("Juan Carlos 2", "Streaming addon with 4K sources", "https://juan-carlos.strem.fun/manifest.json"),
            RecommendedAddon("Orion", "Alternative scraper for premium content", "https://orion.strem.fun/manifest.json")
        )
        return builtIn.map { addon ->
            addon.copy(isInstalled = installed.any { it.url == addon.url || it.id == addon.name.lowercase() })
        }
    }

    fun refresh() = loadHomeData()

    data class HomeUiState(
        val isLoading: Boolean = false,
        val addons: List<Addon> = emptyList(),
        val continueWatching: List<Pair<HistoryItem, Int>> = emptyList(),
        val catalogRows: Map<String, List<MetaItem>> = emptyMap(),
        val recommendedAddons: List<RecommendedAddon> = emptyList(),
        val recommendations: List<MetaItem> = emptyList() // ✅ New
    )
}
