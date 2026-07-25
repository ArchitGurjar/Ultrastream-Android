package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetHomeCatalogsUseCase @Inject constructor(
    private val addonRepository: AddonRepository,
    private val stremioApi: StremioApi
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val catalogListType = Types.newParameterizedType(List::class.java, com.ultrastream.app.data.models.Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<com.ultrastream.app.data.models.Catalog>>(catalogListType)

    suspend operator fun invoke(): Map<String, List<MetaItem>> {
        val addons = addonRepository.getEnabledAddons()
        if (addons.isEmpty()) return emptyMap()

        val catalogRows = mutableMapOf<String, List<MetaItem>>()

        coroutineScope {
            val deferred = addons.flatMap { addon ->
                val catalogs = catalogAdapter.fromJson(addon.catalogs) ?: emptyList()
                val baseUrl = buildAddonBaseUrl(addon.url)

                catalogs.map { cat ->
                    async {
                        val rowId = "${addon.id}_${cat.type}_${cat.id}"
                        try {
                            val url = "$baseUrl/catalog/${cat.type}/${cat.id}.json"
                            val response = stremioApi.getCatalog(url)
                            val items = response.metas?.mapNotNull { meta ->
                                try {
                                    MetaItem(
                                        id = meta.id,
                                        type = meta.type,
                                        name = meta.name,
                                        poster = meta.poster,
                                        background = meta.background,
                                        imdbRating = meta.imdbRating,
                                        year = meta.year,
                                        releaseInfo = meta.releaseInfo,
                                        released = meta.released,
                                        description = meta.description,
                                        genre = meta.genre,
                                        runtime = meta.runtime,
                                        cast = meta.cast,
                                        imdbId = meta.imdb_id,
                                        videos = meta.videos?.map {
                                            Video(
                                                season = it.season,
                                                episode = it.episode,
                                                name = it.name,
                                                title = it.title,
                                                description = it.description,
                                                thumbnail = it.thumbnail,
                                                url = it.url
                                            )
                                        }
                                    )
                                } catch (e: Exception) { null }
                            } ?: emptyList()
                            catalogRows[rowId] = items.take(20) // Limit per row
                        } catch (e: Exception) {
                            // Skip this catalog
                        }
                    }
                }
            }
            deferred.awaitAll()
        }

        return catalogRows.toSortedMap(compareBy { it })
    }
}
