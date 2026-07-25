package com.ultrastream.app.ui.screens.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.domain.usecase.InstallAddonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager,
    private val installAddonUseCase: InstallAddonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    init {
        loadAddons()
        observeDebridKey()
        observeDebridProvider()
    }

    private fun observeDebridKey() {
        viewModelScope.launch {
            preferencesManager.getDebridKey().collect { key ->
                _uiState.value = _uiState.value.copy(debridKey = key)
            }
        }
    }

    private fun observeDebridProvider() {
        viewModelScope.launch {
            preferencesManager.getDebridProvider().collect { provider ->
                _uiState.value = _uiState.value.copy(debridProvider = provider)
            }
        }
    }

    fun loadAddons() {
        viewModelScope.launch {
            val addons = addonRepository.getAllAddons()
            _uiState.value = _uiState.value.copy(addons = addons)
        }
    }

    suspend fun installAddon(rawUrl: String): Boolean {
        val addon = installAddonUseCase(rawUrl)
        if (addon != null) {
            loadAddons()
            return true
        }
        return false
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonRepository.toggleAddon(id, enabled)
        loadAddons()
    }

    suspend fun removeAddon(id: String) {
        addonRepository.removeAddon(id)
        loadAddons()
    }

    suspend fun saveDebridKey(key: String) {
        preferencesManager.setDebridKey(key)
        _uiState.value = _uiState.value.copy(debridKey = key)
    }

    suspend fun saveDebridProvider(provider: String) {
        preferencesManager.setDebridProvider(provider)
        _uiState.value = _uiState.value.copy(debridProvider = provider)
    }

    fun exportAddonsJson(): String {
        return try {
            val addons = _uiState.value.addons
            val exportList = addons.map {
                val catAdapter = moshi.adapter<List<Catalog>>(
                    Types.newParameterizedType(List::class.java, Catalog::class.java)
                )
                val parsedCatalogs = try { catAdapter.fromJson(it.catalogs) } catch (e: Exception) { emptyList() }
                StremioAddonExport(it.id, it.url, it.name, parsedCatalogs, it.enabled, it.required)
            }
            val type = Types.newParameterizedType(List::class.java, StremioAddonExport::class.java)
            moshi.adapter<List<StremioAddonExport>>(type).toJson(exportList)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun importAddonsJson(json: String): Boolean {
        return try {
            val type = Types.newParameterizedType(List::class.java, StremioAddonExport::class.java)
            val importList = moshi.adapter<List<StremioAddonExport>>(type).fromJson(json) ?: return false

            val newAddons = importList.map {
                val catAdapter = moshi.adapter<List<Catalog>>(
                    Types.newParameterizedType(List::class.java, Catalog::class.java)
                )
                val catsJson = catAdapter.toJson(it.catalogs ?: emptyList())
                Addon(it.id, it.url, it.name, catsJson, it.enabled, it.required)
            }
            addonRepository.insertRawAddons(newAddons)
            loadAddons()
            true
        } catch (e: Exception) {
            false
        }
    }

    data class AddonsUiState(
        val addons: List<Addon> = emptyList(),
        val debridKey: String = "",
        val debridProvider: String = "realdebrid"
    )
}

data class StremioAddonExport(
    val id: String,
    val url: String,
    val name: String,
    val catalogs: List<Catalog>? = emptyList(),
    val enabled: Boolean = true,
    val required: Boolean = false
)
