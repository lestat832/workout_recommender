package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the default gym.
 */
class GetDefaultGymUseCase @Inject constructor(
    private val gymRepository: GymRepository
) {
    /**
     * Get the default gym as a Flow.
     * Returns null if no default gym is set.
     */
    fun flow(): Flow<Gym?> {
        return gymRepository.getDefaultGymFlow()
    }

    /**
     * Get the default gym (one-time query).
     * Returns null if no default gym is set.
     */
    suspend fun get(): Gym? {
        return gymRepository.getDefaultGym()
    }
}
