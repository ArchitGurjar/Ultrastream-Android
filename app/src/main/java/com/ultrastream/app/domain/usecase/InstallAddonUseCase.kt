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
        var safeUrl = rawUrl.trim()
        // Normalize stremio:// to https://
        if (safeUrl.startsWith("stremio://")) {
            safeUrl = safeUrl.replace("stremio://", "https://")
        } else if (!safeUrl.startsWith("http://") && !safeUrl.startsWith("https://")) {
            safeUrl = "https://$safeUrl"
        }
        // Remove trailing slash
        if (safeUrl.endsWith("/")) {
            safeUrl = safeUrl.dropLast(1)
        }
        // Ensure manifest.json
        if (!safeUrl.endsWith("/manifest.json") && !safeUrl.endsWith("manifest.json")) {
            safeUrl = if (safeUrl.endsWith("/")) "$safeUrl" + "manifest.json" else "$safeUrl/manifest.json"
        }
        return addonRepository.installAddon(safeUrl)
    }
}
