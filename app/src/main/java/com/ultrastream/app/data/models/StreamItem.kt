package com.ultrastream.app.data.models

data class StreamItem(
    val url: String?,
    val streamUrl: String?,
    val externalUrl: String?,
    val title: String?,
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val addonName: String?,
    val subtitles: List<Subtitle>?,
    val isLive: Boolean = false
)

data class Subtitle(
    val url: String?,
    val file: String?,
    val lang: String?,
    val name: String?
)
