package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_playlists")
data class SmartPlaylist(
    @PrimaryKey val id: String,
    val metaId: String,
    val metaName: String,
    val poster: String?,
    val season: Int,
    val addon: String,
    val total: Int,
    val fetched: Int,
    val status: String,
    val episodesJson: String
)

data class PlaylistEpisode(
    val epNum: Int,
    val epName: String,
    val title: String,
    val stream: StreamItem?,
    val isMissing: Boolean = false
)
