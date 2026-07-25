package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.Profile
import com.ultrastream.app.data.dao.ProfileDao
import com.ultrastream.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageProfilesUseCase @Inject constructor(
    private val profileDao: ProfileDao,
    private val preferencesManager: PreferencesManager
) {
    suspend fun getAllProfiles(): List<Profile> = profileDao.getAll()

    suspend fun getCurrentProfileId(): String {
        return preferencesManager.getCurrentProfile().first()
    }

    suspend fun createProfile(name: String): Profile {
        val id = name.lowercase().replace(" ", "_")
        val profile = Profile(id = id, name = name, avatar = "")
        profileDao.insert(profile)
        return profile
    }

    suspend fun switchProfile(profileId: String) {
        preferencesManager.setCurrentProfile(profileId)
    }

    suspend fun deleteProfile(profileId: String) {
        val current = getCurrentProfileId()
        if (profileId == current) return // Cannot delete active profile
        val profile = profileDao.getById(profileId) ?: return
        profileDao.delete(profile)
    }
}
