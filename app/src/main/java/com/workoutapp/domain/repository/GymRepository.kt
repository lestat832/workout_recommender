package com.workoutapp.domain.repository

import com.workoutapp.domain.model.Gym
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for gym operations.
 */
interface GymRepository {

    /**
     * Get all gyms as a Flow for reactive updates.
     */
    fun getAllGymsFlow(): Flow<List<Gym>>

    /**
     * Get all gyms (one-time query).
     */
    suspend fun getAllGyms(): List<Gym>

    /**
     * Get a gym by ID.
     */
    suspend fun getGymById(gymId: Long): Gym?

    /**
     * Get the default gym.
     */
    suspend fun getDefaultGym(): Gym?

    /**
     * Get the default gym as a Flow.
     */
    fun getDefaultGymFlow(): Flow<Gym?>

    /**
     * Insert a new gym.
     * @return The ID of the newly inserted gym
     */
    suspend fun insertGym(gym: Gym): Long

    /**
     * Update an existing gym.
     */
    suspend fun updateGym(gym: Gym)

    /**
     * Delete a gym.
     */
    suspend fun deleteGym(gym: Gym)

    /**
     * Set a gym as default and unset all others.
     */
    suspend fun setDefaultGym(gymId: Long)

    /**
     * Check if any gyms exist.
     */
    suspend fun hasGyms(): Boolean
}
