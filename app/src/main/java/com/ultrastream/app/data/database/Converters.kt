package com.ultrastream.app.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.*

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromCatalogList(value: List<Catalog>): String {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toCatalogList(value: String): List<Catalog> {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeList(value: List<PlaylistEpisode>): String {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<PlaylistEpisode> {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStreamItem(value: StreamItem?): String? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStreamItem(value: String?): StreamItem? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.fromJson(value)
    }
}
