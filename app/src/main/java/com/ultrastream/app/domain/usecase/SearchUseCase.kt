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
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchUseCase @Inject constructor(
    private val addonRepository: AddonRepository,
    private val stremioApi: StremioApi
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val catalogListType = Types.newParameterizedType(List::class.java, com.ultrastream.app.data.models.Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<com.ultrastream.app.data.models.Catalog>>(catalogListType)

    suspend operator fun invoke(
        query: String,
        filter: String = "all",
        sort: String = "popular"
    ): List<MetaItem> {
        if (query.length < 2) return emptyList()

        val addons = addonRepository.getEnabledAddons()
        val types = when (filter) {
            "all" -> listOf("movie", "series", "anime", "tv")
            else -> listOf(filter)
        }

        return coroutineScope {
            val deferred = addons.map { addon ->
                async {
                    val results = mutableListOf<MetaItem>()
                    val baseUrl = buildAddonBaseUrl(addon.url)
                    val catalogs = catalogAdapter.fromJson(addon.catalogs) ?: emptyList()

                    for (type in types) {
                        val searchableCatalog = catalogs.firstOrNull { cat ->
                            cat.type == type && (cat.extraSupported?.contains("search") == true ||
                                cat.extra?.any { it.name == "search" } == true)
                        } ?: continue

                        val encodedQuery = URLEncoder.encode(query, "UTF-8")
                            .replace("+", "%20")
                        val searchUrl = "$baseUrl/catalog/$type/${searchableCatalog.id}/search=$encodedQuery.json"

                        try {
                            val response = stremioApi.getCatalog(searchUrl)
                            response.metas?.forEach { meta ->
                                results.add(
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
                                )
                            }
                        } catch (e: Exception) {
                            // Skip this catalog
                        }
                    }
                    results
                }
            }

            val allResults = deferred.awaitAll().flatten()
            val unique = allResults.distinctBy { it.id }

            when (sort) {
                "rating" -> unique.sortedByDescending { it.imdbRating?.toDoubleOrNull() ?: 0.0 }
                "year" -> unique.sortedByDescending { it.year?.toIntOrNull() ?: 0 }
                else -> unique
            }
        }
    }
}
