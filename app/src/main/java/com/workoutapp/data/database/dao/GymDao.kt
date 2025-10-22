package com.workoutapp.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workoutapp.data.database.entities.GymEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for gym operations.
 */
@Dao
interface GymDao {

    /**
     * Insert a new gym.
     * @return The ID of the newly inserted gym
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gym: GymEntity): Long

    /**
     * Update an existing gym.
     */
    @Update
    suspend fun update(gym: GymEntity)

    /**
     * Delete a gym.
     */
    @Delete
    suspend fun delete(gym: GymEntity)

    /**
     * Get all gyms as a Flow for reactive updates.
     */
    @Query("SELECT * FROM gyms ORDER BY isDefault DESC, name ASC")
    fun getAllGymsFlow(): Flow<List<GymEntity>>

    /**
     * Get all gyms (one-time query).
     */
    @Query("SELECT * FROM gyms ORDER BY isDefault DESC, name ASC")
    suspend fun getAllGyms(): List<GymEntity>

    /**
     * Get a gym by ID.
     */
    @Query("SELECT * FROM gyms WHERE id = :gymId")
    suspend fun getGymById(gymId: Long): GymEntity?

    /**
     * Get the default gym.
     */
    @Query("SELECT * FROM gyms WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultGym(): GymEntity?

    /**
     * Get the default gym as a Flow.
     */
    @Query("SELECT * FROM gyms WHERE isDefault = 1 LIMIT 1")
    fun getDefaultGymFlow(): Flow<GymEntity?>

    /**
     * Set a gym as default and unset all others.
     * This is a two-step process to ensure only one default gym exists.
     */
    @Query("UPDATE gyms SET isDefault = CASE WHEN id = :gymId THEN 1 ELSE 0 END")
    suspend fun setDefaultGym(gymId: Long)

    /**
     * Check if any gyms exist.
     */
    @Query("SELECT COUNT(*) FROM gyms")
    suspend fun getGymCount(): Int

    /**
     * Delete all gyms (used for testing).
     */
    @Query("DELETE FROM gyms")
    suspend fun deleteAllGyms()
}
