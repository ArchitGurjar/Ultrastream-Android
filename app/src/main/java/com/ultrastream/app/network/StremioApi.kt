package com.ultrastream.app.network

import com.ultrastream.app.data.models.Catalog
import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApi {
    @GET
    suspend fun getManifest(@Url url: String): ManifestResponse

    @GET
    suspend fun getCatalog(@Url url: String): CatalogResponse

    @GET
    suspend fun getMeta(@Url url: String): MetaResponse

    @GET
    suspend fun getStreams(@Url url: String): StreamResponse

    @GET
    suspend fun getSubtitles(@Url url: String): SubtitleResponse
}

data class ManifestResponse(
    val id: String,
    val name: String,
    val catalogs: List<Catalog>?,
    val resources: List<String>?,
    val types: List<String>?,
    val version: String?
)

data class CatalogResponse(
    val metas: List<Meta>? = emptyList()
)

data class MetaResponse(
    val meta: Meta?
)

data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: List<String>?,
    val runtime: String?,
    val cast: List<String>?,
    val imdb_id: String?,
    val videos: List<Video>? = null
)

data class Video(
    val season: Int?,
    val episode: Int?,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val url: String?
)

data class StreamResponse(
    val streams: List<Stream>? = emptyList()
)

data class Stream(
    val url: String?,
    val streamUrl: String?,
    val externalUrl: String?,
    val title: String?,
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val subtitles: List<StreamSubtitle>?,
    val isLive: Boolean = false
)

data class StreamSubtitle(
    val url: String?,
    val file: String?,
    val lang: String?,
    val name: String?
)

data class SubtitleResponse(
    val subtitles: List<StreamSubtitle>? = emptyList()
)
