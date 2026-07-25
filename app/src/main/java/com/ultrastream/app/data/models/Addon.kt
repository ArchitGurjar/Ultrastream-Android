package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addons")
data class Addon(
    @PrimaryKey val id: String,
    val url: String,
    val name: String,
    val catalogs: String,
    val enabled: Boolean = true,
    val required: Boolean = false
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extraSupported: List<String>? = null,
    val extra: List<Extra>? = null
)

data class Extra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)
