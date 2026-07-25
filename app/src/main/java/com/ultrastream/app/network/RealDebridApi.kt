package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RealDebridApi {
    @GET("torrents/instantAvailability")
    suspend fun checkInstantAvailability(
        @Header("Authorization") auth: String,
        @Query("hash") hash: String
    ): Map<String, Map<String, List<String>>>

    @GET("torrents/addMagnet")
    suspend fun addMagnet(
        @Header("Authorization") auth: String,
        @Query("magnet") magnet: String
    ): AddTorrentResponse

    @GET("torrents/selectFiles")
    suspend fun selectFiles(
        @Header("Authorization") auth: String,
        @Query("id") torrentId: String,
        @Query("files") files: String = "all"
    ): String

    @GET("torrents/status")
    suspend fun getTorrentStatus(
        @Header("Authorization") auth: String,
        @Query("id") torrentId: String
    ): TorrentStatus

    @GET("torrents/unrestrictLink")
    suspend fun unrestrictLink(
        @Header("Authorization") auth: String,
        @Query("link") link: String
    ): UnrestrictedLink
}

data class AddTorrentResponse(val id: String, val uri: String)
data class TorrentStatus(val id: String, val status: String, val links: List<String>)
data class UnrestrictedLink(val link: String)
