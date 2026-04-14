package com.workoutapp.domain.repository

import java.util.Date

/**
 * Persists 4-week block periodization state per gym in SharedPreferences.
 * No DB schema changes — pure runtime state, orphaned keys harmless on rollback.
 */
interface BlockStateRepository {
    /**
     * Returns (blockStartDate, blockNumber) or null if no state persisted yet.
     * Does NOT write on read — callers handle null by using a synthetic default.
     * State is only persisted via setState/advanceBlock on workout completion.
     */
    suspend fun getState(gymId: Long): Pair<Date, Int>?

    /**
     * Persists block state. Called on first workout completion to initialize,
     * or on extended-absence comeback to reset to a fresh block.
     */
    suspend fun setState(gymId: Long, blockStartDate: Date, blockNumber: Int)

    /**
     * Advances the block: sets start date to the next Monday after today,
     * increments block number by 1. Called after completing a scheduled
     * week-4 deload workout.
     */
    suspend fun advanceBlock(gymId: Long)
}
