package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.repository.AddonRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallAddonUseCase @Inject constructor(
    private val addonRepository: AddonRepository
) {
    suspend operator fun invoke(rawUrl: String): Addon? {
        // Normalize URL
        var safeUrl = rawUrl.trim()
        if (safeUrl.startsWith("stremio://")) {
            safeUrl = safeUrl.replace("stremio://", "https://")
        } else if (!safeUrl.startsWith("http")) {
            safeUrl = "https://$safeUrl"
        }
        if (safeUrl.endsWith("/")) {
            safeUrl = safeUrl.dropLast(1)
        }
        // Ensure manifest.json is at the end
        if (!safeUrl.endsWith("/manifest.json") && !safeUrl.endsWith("manifest.json")) {
            safeUrl = if (safeUrl.endsWith("/")) "$safeUrl" + "manifest.json" else "$safeUrl/manifest.json"
        }
        return addonRepository.installAddon(safeUrl)
    }
}
