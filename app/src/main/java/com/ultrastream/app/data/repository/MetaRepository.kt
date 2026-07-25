package com.ultrastream.app.data.repository

import com.ultrastream.app.data.dao.CachedMetaDao
import com.ultrastream.app.data.models.CachedMeta
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.network.Meta
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaRepository @Inject constructor(
    private val cachedMetaDao: CachedMetaDao,
    private val addonRepository: AddonRepository,
    private val stremioApi: StremioApi,
    private val moshi: Moshi
) {

    suspend fun getMeta(id: String, type: String): MetaItem? {
        val cacheKey = "$id:$type"
        val cached = cachedMetaDao.getByKey(cacheKey)
        if (cached != null) {
            return try {
                moshi.adapter(MetaItem::class.java).fromJson(cached.json)
            } catch (e: Exception) {
                null
            }
        }

        val addons = addonRepository.getEnabledAddons()
        var mergedMeta: Meta? = null
        val allVideos = mutableListOf<Video>()

        for (addon in addons) {
            val base = buildAddonBaseUrl(addon.url)
            val fullUrl = "$base/meta/$type/$id.json"
            val meta = try {
                stremioApi.getMeta(fullUrl).meta
            } catch (e: Exception) {
                null
            }
            if (meta != null) {
                if (mergedMeta == null) {
                    mergedMeta = meta.copy(videos = null)
                } else {
                    mergedMeta = mergedMeta.copy(
                        name = mergedMeta.name.takeIf { it.isNotBlank() } ?: meta.name,
                        poster = mergedMeta.poster ?: meta.poster,
                        background = mergedMeta.background ?: meta.background,
                        imdbRating = mergedMeta.imdbRating ?: meta.imdbRating,
                        year = mergedMeta.year ?: meta.year,
                        releaseInfo = mergedMeta.releaseInfo ?: meta.releaseInfo,
                        released = mergedMeta.released ?: meta.released,
                        description = mergedMeta.description ?: meta.description,
                        genre = mergedMeta.genre ?: meta.genre,
                        runtime = mergedMeta.runtime ?: meta.runtime,
                        cast = mergedMeta.cast ?: meta.cast,
                        imdb_id = mergedMeta.imdb_id ?: meta.imdb_id
                    )
                }
                meta.videos?.let { allVideos.addAll(it) }
            }
        }

        if (mergedMeta == null) return null

        val uniqueVideos = allVideos.distinctBy { it.season?.toString() + ":" + it.episode?.toString() + ":" + it.name }
        val finalMeta = mergedMeta.copy(videos = uniqueVideos)

        val metaItem = convertToMetaItem(finalMeta)
        val json = moshi.adapter(MetaItem::class.java).toJson(metaItem)
        cachedMetaDao.insert(CachedMeta(cacheKey, json))
        return metaItem
    }

    private fun convertToMetaItem(meta: Meta): MetaItem {
        return MetaItem(
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
    }
}
