package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolveStreamUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(stream: StreamItem): StreamItem {
        val debridKey = preferencesManager.getDebridKey().first()
        return streamRepository.resolveStream(stream, debridKey.takeIf { it.isNotBlank() })
    }
}
