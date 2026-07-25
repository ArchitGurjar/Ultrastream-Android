package com.ultrastream.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ultrastream.app.data.models.StreamItem
import java.io.File

class M3UExporter(private val context: Context) {

    fun exportToM3U(
        streams: List<StreamItem>,
        title: String,
        fileName: String = "playlist.m3u"
    ): File? {
        return try {
            val content = buildM3UContent(streams, title)
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun buildM3UContent(streams: List<StreamItem>, title: String): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST: $title")

        streams.forEach { stream ->
            val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
            if (url.isNullOrBlank()) return@forEach
            if (url.startsWith("magnet:")) return@forEach
            val name = stream.title ?: stream.name ?: "Stream"
            sb.appendLine("#EXTINF:-1,${escapeM3UString(name)}")
            sb.appendLine(url)
        }

        return sb.toString()
    }

    fun shareM3U(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/x-mpegurl"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Playlist"))
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun escapeM3UString(text: String): String {
        return text.replace(",", "\\,")
    }
}
