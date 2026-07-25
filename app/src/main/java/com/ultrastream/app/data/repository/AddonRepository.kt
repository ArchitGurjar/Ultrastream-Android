package com.ultrastream.app.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.ultrastream.app.data.dao.AddonDao
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.models.Extra
import com.ultrastream.app.network.StremioApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddonRepository @Inject constructor(
    private val addonDao: AddonDao,
    private val stremioApi: StremioApi,
    private val moshi: Moshi
) {

    private val catalogListType = Types.newParameterizedType(List::class.java, Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<Catalog>>(catalogListType)

    suspend fun installAddon(url: String): Addon? {
        val manifest = try {
            stremioApi.getManifest(url)
        } catch (e: Exception) {
            return null
        }
        if (manifest.id.isBlank()) return null

        val existing = addonDao.getById(manifest.id)
        if (existing != null) return existing

        val netCatalogs = manifest.catalogs ?: emptyList()
        val mappedCatalogs = netCatalogs.map { netCat ->
            Catalog(
                type = netCat.type,
                id = netCat.id,
                name = netCat.name,
                extraSupported = netCat.extraSupported,
                extra = netCat.extra?.map {
                    Extra(
                        name = it.name,
                        isRequired = it.isRequired,
                        options = it.options
                    )
                }
            )
        }

        val catalogsJson = catalogAdapter.toJson(mappedCatalogs)
        val addon = Addon(
            id = manifest.id,
            url = url,
            name = manifest.name ?: manifest.id,
            catalogs = catalogsJson,
            enabled = true,
            required = false
        )
        addonDao.insert(addon)
        return addon
    }

    suspend fun getAllAddons(): List<Addon> = addonDao.getAll()

    suspend fun getEnabledAddons(): List<Addon> {
        return addonDao.getAll().filter { it.enabled }
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonDao.updateEnabled(id, enabled)
    }

    suspend fun removeAddon(id: String) {
        addonDao.deleteById(id)
    }

    suspend fun insertRawAddons(addons: List<Addon>) {
        addonDao.insertAll(addons)
    }
}
