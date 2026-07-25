package com.ultrastream.app.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LinkVerifier @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun verifyLink(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .addHeader("User-Agent", "UltraStream/1.0 (Android)")
                    .addHeader("Referer", "https://ultrastream.app/")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val isValid = response.code in 200..299
                response.close()
                isValid
            } catch (e: Exception) {
                false
            }
        }
    }
}
