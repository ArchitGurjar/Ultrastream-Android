package com.ultrastream.app.utils

import android.util.Log
import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
import com.ultrastream.app.network.RealDebridApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DebridHelper"

@Singleton
class DebridHelper @Inject constructor(
    private val realDebridApi: RealDebridApi,
    private val allDebridApi: AllDebridApi,
    private val premiumizeApi: PremiumizeApi
) {

    enum class DebridProvider {
        REAL_DEBRID, ALL_DEBRID, PREMIUMIZE
    }

    suspend fun resolveStreamUrl(
        url: String,
        debridKey: String?,
        provider: DebridProvider = DebridProvider.REAL_DEBRID
    ): String {
        if (debridKey.isNullOrBlank()) {
            Log.d(TAG, "No debrid key provided, returning original URL")
            return url
        }

        return try {
            when (provider) {
                DebridProvider.REAL_DEBRID -> resolveRealDebrid(url, debridKey)
                DebridProvider.ALL_DEBRID -> resolveAllDebrid(url, debridKey)
                DebridProvider.PREMIUMIZE -> resolvePremiumize(url, debridKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Debrid resolution failed for $provider: ${e.message}", e)
            url
        }
    }

    private suspend fun resolveRealDebrid(url: String, apiKey: String): String {
        val auth = "Bearer $apiKey"
        return if (url.startsWith("magnet:")) {
            resolveRealDebridMagnet(url, auth)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolveRealDebridMagnet(magnet, auth)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolveRealDebridMagnet(magnet: String, auth: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val availability = realDebridApi.checkInstantAvailability(auth, hash)
            if (availability.isNotEmpty()) {
                val cached = availability.values.firstOrNull { it.isNotEmpty() }
                if (cached != null) {
                    val addResponse = realDebridApi.addMagnet(auth, magnet)
                    val torrentId = addResponse.id
                    realDebridApi.selectFiles(auth, torrentId, "all")
                    var status = realDebridApi.getTorrentStatus(auth, torrentId)
                    var attempts = 0
                    while (attempts < 60 && currentCoroutineContext().isActive) {
                        delay(1000)
                        status = realDebridApi.getTorrentStatus(auth, torrentId)
                        attempts++
                    }
                    if (status.status == "downloaded" || status.status == "ready") {
                        val link = status.links.firstOrNull()
                        if (link != null) {
                            val unrestricted = realDebridApi.unrestrictLink(auth, link)
                            return unrestricted.link
                        }
                    }
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "Real-Debrid magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private suspend fun resolveAllDebrid(url: String, apiKey: String): String {
        return if (url.startsWith("magnet:")) {
            resolveAllDebridMagnet(url, apiKey)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolveAllDebridMagnet(magnet, apiKey)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolveAllDebridMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val uploadResponse = allDebridApi.uploadMagnet(apiKey, magnet)
            if (!uploadResponse.status || uploadResponse.data == null || uploadResponse.data.id.isEmpty()) {
                Log.e(TAG, "AllDebrid upload failed: ${uploadResponse.message}")
                return magnet
            }
            val torrentId = uploadResponse.data.id
            var statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
            var attempts = 0
            while (attempts < 60 && currentCoroutineContext().isActive) {
                val magnets = statusResponse.data?.magnets ?: emptyList()
                val first = magnets.firstOrNull()
                if (first != null && first.status == "Completed") {
                    break
                }
                delay(2000)
                statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
                attempts++
            }
            val magnetItem = statusResponse.data?.magnets?.firstOrNull()
            if (magnetItem != null && magnetItem.status == "Completed") {
                val linkResponse = allDebridApi.getMagnetLink(apiKey, torrentId)
                if (linkResponse.status && linkResponse.data != null && linkResponse.data.link.isNotEmpty()) {
                    return linkResponse.data.link
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "AllDebrid magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private suspend fun resolvePremiumize(url: String, apiKey: String): String {
        return if (url.startsWith("magnet:")) {
            resolvePremiumizeMagnet(url, apiKey)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolvePremiumizeMagnet(magnet, apiKey)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolvePremiumizeMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val transferResponse = premiumizeApi.createTransfer(apiKey, magnet)
            if (transferResponse.status != "success" || transferResponse.id.isEmpty()) {
                Log.e(TAG, "Premiumize transfer creation failed: ${transferResponse.message}")
                return magnet
            }
            val transferId = transferResponse.id
            var statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
            var attempts = 0
            while (attempts < 60 && currentCoroutineContext().isActive) {
                val transfers = statusResponse.transfers ?: emptyList()
                val first = transfers.firstOrNull()
                if (first != null && first.status == "finished") {
                    break
                }
                delay(2000)
                statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
                attempts++
            }
            val transferItem = statusResponse.transfers?.firstOrNull()
            if (transferItem != null && transferItem.status == "finished") {
                val itemResponse = premiumizeApi.getItemDetails(apiKey, transferId)
                if (itemResponse.status == "success" && itemResponse.link.isNotEmpty()) {
                    return itemResponse.link
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "Premiumize magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private fun extractHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1) ?: ""
    }

    fun applyDebridParams(url: String, debridKey: String): String {
        if (debridKey.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}realdebrid=$debridKey"
    }
}
