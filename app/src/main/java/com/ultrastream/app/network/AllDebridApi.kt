package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AllDebridApi {
    @GET("magnet/upload")
    suspend fun uploadMagnet(
        @Header("Authorization") auth: String,
        @Query("magnet") magnet: String
    ): AllDebridUploadResponse

    @GET("magnet/status")
    suspend fun getMagnetStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridStatusResponse

    @GET("magnet/link")
    suspend fun getMagnetLink(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridLinkResponse
}

data class AllDebridUploadResponse(
    val status: Boolean = false,
    val message: String = "",
    val data: AllDebridUploadData? = null
)

data class AllDebridUploadData(
    val id: String = "",
    val name: String = "",
    val size: Long = 0L,
    val hash: String = "",
    val links: List<String> = emptyList()
)

data class AllDebridStatusResponse(
    val status: Boolean = false,
    val message: String = "",
    val data: AllDebridStatusData? = null
)

data class AllDebridStatusData(
    val id: String = "",
    val magnets: List<AllDebridMagnetItem> = emptyList()
)

data class AllDebridMagnetItem(
    val id: String = "",
    val filename: String = "",
    val size: Long = 0L,
    val status: String = "",
    val statusCode: Int = 0,
    val downloaded: Long = 0L,
    val uploaded: Long = 0L,
    val speed: Long = 0L,
    val seeders: Int = 0,
    val files: List<AllDebridFileLink> = emptyList()
)

data class AllDebridFileLink(
    val link: String = "",
    val filename: String = "",
    val size: Long = 0L,
    val stream: List<AllDebridStreamInfo> = emptyList()
)

data class AllDebridStreamInfo(
    val url: String = "",
    val quality: String = "",
    val language: String = "",
    val subtitles: List<AllDebridSubtitle> = emptyList()
)

data class AllDebridSubtitle(
    val url: String = "",
    val lang: String = ""
)

data class AllDebridLinkResponse(
    val status: Boolean = false,
    val message: String = "",
    val data: AllDebridLinkData? = null
)

data class AllDebridLinkData(
    val link: String = "",
    val filename: String = "",
    val filesize: Long = 0L,
    val host: String = "",
    val hostDomain: String = "",
    val stream: List<AllDebridStreamInfo> = emptyList()
)
