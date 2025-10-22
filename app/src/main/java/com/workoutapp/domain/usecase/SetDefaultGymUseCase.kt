package com.workoutapp.domain.usecase

import com.workoutapp.domain.repository.GymRepository
import javax.inject.Inject

/**
 * Use case for setting a gym as the default.
 */
class SetDefaultGymUseCase @Inject constructor(
    private val gymRepository: GymRepository
) {
    /**
     * Set a gym as the default.
     * All other gyms will be unmarked as default.
     *
     * @param gymId The ID of the gym to set as default
     */
    suspend operator fun invoke(gymId: Long) {
        require(gymId > 0) { "Invalid gym ID" }

        // Verify the gym exists
        val gym = gymRepository.getGymById(gymId)
        requireNotNull(gym) { "Gym not found with ID: $gymId" }

        // Set as default (this will unset all others)
        gymRepository.setDefaultGym(gymId)
    }
}
