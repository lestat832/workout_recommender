package com.workoutapp.data.database.dao

import androidx.room.*
import com.workoutapp.data.database.entities.StravaSyncQueueEntity
import com.workoutapp.data.database.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaSyncDao {

    /**
     * Insert a new sync queue entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncQueue: StravaSyncQueueEntity)

    /**
     * Update existing sync queue entry
     */
    @Update
    suspend fun update(syncQueue: StravaSyncQueueEntity)

    /**
     * Get sync queue entry by ID
     */
    @Query("SELECT * FROM strava_sync_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StravaSyncQueueEntity?

    /**
     * Get sync queue entry by workout ID
     */
    @Query("SELECT * FROM strava_sync_queue WHERE workoutId = :workoutId LIMIT 1")
    suspend fun getByWorkoutId(workoutId: String): StravaSyncQueueEntity?

    /**
     * Get all pending sync entries
     */
    @Query("SELECT * FROM strava_sync_queue WHERE status = 'PENDING' ORDER BY queuedAt ASC")
    suspend fun getAllPending(): List<StravaSyncQueueEntity>

    /**
     * Get all failed sync entries
     */
    @Query("SELECT * FROM strava_sync_queue WHERE status = 'FAILED' ORDER BY lastAttemptAt DESC")
    suspend fun getAllFailed(): List<StravaSyncQueueEntity>

    /**
     * Get all sync entries for a workout (includes all statuses)
     */
    @Query("SELECT * FROM strava_sync_queue WHERE workoutId = :workoutId")
    fun getByWorkoutIdFlow(workoutId: String): Flow<List<StravaSyncQueueEntity>>

    /**
     * Update sync status
     */
    @Query("UPDATE strava_sync_queue SET status = :status, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark sync as completed with Strava activity ID
     */
    @Query("""
        UPDATE strava_sync_queue
        SET status = 'COMPLETED',
            stravaActivityId = :stravaActivityId,
            completedAt = :timestamp,
            lastAttemptAt = :timestamp
        WHERE id = :id
    """)
    suspend fun markCompleted(id: String, stravaActivityId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark sync as failed with error message
     */
    @Query("""
        UPDATE strava_sync_queue
        SET status = 'FAILED',
            errorMessage = :errorMessage,
            retryCount = retryCount + 1,
            lastAttemptAt = :timestamp
        WHERE id = :id
    """)
    suspend fun markFailed(id: String, errorMessage: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete sync queue entry
     */
    @Query("DELETE FROM strava_sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Delete all completed syncs older than specified timestamp
     */
    @Query("DELETE FROM strava_sync_queue WHERE status = 'COMPLETED' AND completedAt < :beforeTimestamp")
    suspend fun deleteCompletedBefore(beforeTimestamp: Long)

    /**
     * Get count of pending syncs
     */
    @Query("SELECT COUNT(*) FROM strava_sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    /**
     * Check if workout is already synced to Strava
     */
    @Query("SELECT COUNT(*) > 0 FROM strava_sync_queue WHERE workoutId = :workoutId AND status = 'COMPLETED'")
    suspend fun isWorkoutSynced(workoutId: String): Boolean

    /**
     * Get Strava activity ID for a workout (if synced)
     */
    @Query("SELECT stravaActivityId FROM strava_sync_queue WHERE workoutId = :workoutId AND status = 'COMPLETED' LIMIT 1")
    suspend fun getStravaActivityId(workoutId: String): Long?
}
