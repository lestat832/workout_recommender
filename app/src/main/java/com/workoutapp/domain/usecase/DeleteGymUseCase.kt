package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import javax.inject.Inject

/**
 * Use case for deleting a gym.
 */
class DeleteGymUseCase @Inject constructor(
    private val gymRepository: GymRepository,
    private val getAllGymsUseCase: GetAllGymsUseCase
) {
    /**
     * Delete a gym.
     *
     * If the gym is the default gym, another gym will be automatically set as default.
     * Cannot delete if it's the only gym remaining.
     *
     * @param gym The gym to delete
     * @throws IllegalStateException if this is the only gym
     */
    suspend operator fun invoke(gym: Gym) {
        val allGyms = getAllGymsUseCase.get()

        // Prevent deletion of the last gym
        require(allGyms.size > 1) { "Cannot delete the only gym. At least one gym is required." }

        // Delete the gym
        gymRepository.deleteGym(gym)

        // If we deleted the default gym, set another gym as default
        if (gym.isDefault) {
            val remainingGyms = getAllGymsUseCase.get()
            if (remainingGyms.isNotEmpty()) {
                gymRepository.setDefaultGym(remainingGyms.first().id)
            }
        }
    }
}
