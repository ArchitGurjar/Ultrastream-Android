package com.ultrastream.app.domain.usecase

import android.content.Context
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.utils.M3UExporter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportM3UUseCase @Inject constructor(
    private val context: Context
) {
    suspend operator fun invoke(
        streams: List<StreamItem>,
        title: String,
        fileName: String = "playlist.m3u"
    ): File? {
        val exporter = M3UExporter(context)
        return exporter.exportToM3U(streams, title, fileName)
    }

    fun shareM3U(file: File) {
        val exporter = M3UExporter(context)
        exporter.shareM3U(file)
    }
}
