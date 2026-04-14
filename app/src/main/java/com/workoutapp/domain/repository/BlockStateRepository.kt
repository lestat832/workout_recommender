package com.workoutapp.domain.repository

import java.util.Date

/**
 * Persists 4-week block periodization state per gym in SharedPreferences.
 * No DB schema changes — pure runtime state, orphaned keys harmless on rollback.
 */
interface BlockStateRepository {
    /**
     * Returns (blockStartDate, blockNumber) for this gym.
     * If no state exists, initializes to (today, 1) and returns it.
     */
    suspend fun getState(gymId: Long): Pair<Date, Int>

    /**
     * Advances the block: sets start date to the next Monday after today,
     * increments block number by 1. Called after completing a deload workout.
     */
    suspend fun advanceBlock(gymId: Long)
}
