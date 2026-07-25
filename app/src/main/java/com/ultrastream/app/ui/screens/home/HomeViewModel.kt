package com.ultrastream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.HistoryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.RecommendedAddon
import com.ultrastream.app.data.repository.AddonRepository
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
    private val updateWatchProgressUseCase: UpdateWatchProgressUseCase
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

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                continueWatching = continueWatching,
                addons = addons,
                catalogRows = catalogRows,
                recommendedAddons = getRecommendedAddons(addons)
            )
        }
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
        val recommendedAddons: List<RecommendedAddon> = emptyList()
    )
}
