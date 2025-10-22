package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import javax.inject.Inject

/**
 * Use case for creating a new gym.
 */
class CreateGymUseCase @Inject constructor(
    private val gymRepository: GymRepository
) {
    /**
     * Create a new gym.
     *
     * @param name The name of the gym
     * @param equipmentList The list of equipment available at the gym
     * @param setAsDefault Whether to set this gym as the default
     * @return The ID of the newly created gym
     */
    suspend operator fun invoke(
        name: String,
        equipmentList: List<String>,
        setAsDefault: Boolean = false
    ): Long {
        require(name.isNotBlank()) { "Gym name cannot be blank" }
        require(equipmentList.isNotEmpty()) { "Gym must have at least one piece of equipment" }

        val gym = Gym(
            name = name.trim(),
            equipmentList = equipmentList,
            isDefault = setAsDefault
        )

        val gymId = gymRepository.insertGym(gym)

        // If this gym should be default, update all gyms
        if (setAsDefault) {
            gymRepository.setDefaultGym(gymId)
        }

        return gymId
    }
}
