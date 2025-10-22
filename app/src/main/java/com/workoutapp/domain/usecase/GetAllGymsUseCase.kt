package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all gyms.
 */
class GetAllGymsUseCase @Inject constructor(
    private val gymRepository: GymRepository
) {
    /**
     * Get all gyms as a Flow for reactive updates.
     * Gyms are ordered by default status (default first) then by name.
     */
    fun flow(): Flow<List<Gym>> {
        return gymRepository.getAllGymsFlow()
    }

    /**
     * Get all gyms (one-time query).
     * Gyms are ordered by default status (default first) then by name.
     */
    suspend fun get(): List<Gym> {
        return gymRepository.getAllGyms()
    }
}
