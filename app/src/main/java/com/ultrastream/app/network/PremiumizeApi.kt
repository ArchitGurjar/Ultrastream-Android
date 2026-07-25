package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PremiumizeApi {
    @GET("transfer/create")
    suspend fun createTransfer(
        @Header("Authorization") auth: String,
        @Query("src") src: String
    ): PremiumizeTransferResponse

    @GET("transfer/list")
    suspend fun getTransferStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeStatusResponse

    @GET("item/details")
    suspend fun getItemDetails(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeItemResponse
}

data class PremiumizeTransferResponse(
    val status: String = "",
    val id: String = "",
    val name: String = "",
    val message: String = ""
)

data class PremiumizeStatusResponse(
    val status: String = "",
    val message: String = "",
    val id: String = "",
    val transfers: List<PremiumizeTransferItem> = emptyList()
)

data class PremiumizeTransferItem(
    val id: String = "",
    val name: String = "",
    val status: String = "",
    val message: String = "",
    val progress: Double = 0.0,
    val file_id: String = "",
    val folder_id: String = "",
    val size: Long = 0L,
    val created_at: String = "",
    val finished_at: String = "",
    val files: List<PremiumizeFileInfo> = emptyList()
)

data class PremiumizeFileInfo(
    val id: String = "",
    val name: String = "",
    val size: Long = 0L,
    val link: String = "",
    val stream_link: String = "",
    val type: String = "",
    val path: String = ""
)

data class PremiumizeItemResponse(
    val status: String = "",
    val message: String = "",
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val size: Long = 0L,
    val link: String = "",
    val stream_link: String = "",
    val content: List<PremiumizeContent> = emptyList()
)

data class PremiumizeContent(
    val id: String = "",
    val name: String = "",
    val size: Long = 0L,
    val link: String = "",
    val stream_link: String = "",
    val type: String = "",
    val path: String = ""
)
