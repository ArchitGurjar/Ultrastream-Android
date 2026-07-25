package com.ultrastream.app.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {

    fun parseM3U(content: String): List<M3UItem> {
        val lines = content.lines()
        val items = mutableListOf<M3UItem>()
        var currentTitle: String? = null
        var currentGroup: String? = null
        var currentLogo: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:")) {
                val titleMatch = Regex(",([^,]*)$").find(trimmed)
                currentTitle = titleMatch?.groupValues?.get(1)?.trim()
                val groupMatch = Regex("group-title=\"([^\"]*)\"").find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1)
                val logoMatch = Regex("tvg-logo=\"([^\"]*)\"").find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1)
            } else if (!trimmed.startsWith("#")) {
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
                    trimmed.startsWith("magnet:") || trimmed.startsWith("/")) {
                    items.add(
                        M3UItem(
                            url = trimmed,
                            title = currentTitle ?: "Unknown",
                            group = currentGroup,
                            logo = currentLogo
                        )
                    )
                }
                currentTitle = null
                currentGroup = null
                currentLogo = null
            }
        }
        return items
    }

    data class M3UItem(
        val url: String,
        val title: String,
        val group: String?,
        val logo: String?
    )
}
