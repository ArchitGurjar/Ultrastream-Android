package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetStreamsUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        metaId: String,
        metaType: String,
        season: Int? = null,
        episode: Int? = null
    ): List<StreamItem> {
        val addons = addonRepository.getEnabledAddons()
        if (addons.isEmpty()) return emptyList()

        val addonUrls = addons.map { it.url }
        val hindiPriority = preferencesManager.getHindiPriority().first()
        val debridKey = preferencesManager.getDebridKey().first()

        return streamRepository.getStreams(
            metaId = metaId,
            metaType = metaType,
            season = season,
            episode = episode,
            addonUrls = addonUrls,
            hindiPriority = hindiPriority,
            debridKey = if (debridKey.isNotBlank()) debridKey else null
        )
    }
}
