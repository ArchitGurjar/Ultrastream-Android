package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String
)

data class RecommendedAddon(
    val name: String,
    val description: String,
    val url: String,
    val isInstalled: Boolean = false
)
