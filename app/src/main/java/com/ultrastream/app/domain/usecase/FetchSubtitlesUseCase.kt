package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.Subtitle
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchSubtitlesUseCase @Inject constructor(
    private val addonRepository: AddonRepository,
    private val stremioApi: StremioApi
) {
    suspend operator fun invoke(
        metaId: String,
        type: String,
        season: Int,
        episode: Int
    ): List<Subtitle> {
        val allSubtitles = mutableListOf<Subtitle>()
        val addons = addonRepository.getEnabledAddons()
        val idWithExtra = "$metaId:$season:$episode"

        for (addon in addons) {
            val baseUrl = buildAddonBaseUrl(addon.url)
            val url = "$baseUrl/subtitles/$type/$idWithExtra.json"
            try {
                val response = stremioApi.getSubtitles(url)
                response.subtitles?.forEach { netSub ->
                    allSubtitles.add(
                        Subtitle(
                            url = netSub.url,
                            file = netSub.file,
                            lang = netSub.lang,
                            name = netSub.name
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip
            }
        }
        return allSubtitles.distinctBy { it.url ?: it.file }
    }
}
