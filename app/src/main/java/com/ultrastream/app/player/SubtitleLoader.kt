package com.ultrastream.app.player

import android.content.Context
import android.net.Uri
import com.ultrastream.app.data.models.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleLoader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun loadSubtitleFile(context: Context, subtitle: Subtitle): File? {
        val url = subtitle.url ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "UltraStream/1.0 (Android)")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext null
                }
                val body = response.body?.bytes()
                response.close()
                if (body == null || body.isEmpty()) return@withContext null

                val fileName = "subtitle_${System.currentTimeMillis()}.srt"
                val file = File(context.cacheDir, fileName)
                file.writeBytes(body)
                file
            } catch (e: Exception) {
                null
            }
        }
    }
}
