package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a workout queued for sync to Strava
 * Tracks sync status, retry attempts, and Strava activity mapping
 */
@Entity(tableName = "strava_sync_queue")
data class StravaSyncQueueEntity(
    @PrimaryKey
    val id: String,

    /**
     * ID of the workout to sync
     */
    val workoutId: String,

    /**
     * Sync status: PENDING, IN_PROGRESS, COMPLETED, FAILED
     */
    val status: SyncStatus,

    /**
     * Strava activity ID (null until successfully synced)
     */
    val stravaActivityId: Long? = null,

    /**
     * When this sync was queued
     */
    val queuedAt: Long = System.currentTimeMillis(),

    /**
     * Last sync attempt timestamp
     */
    val lastAttemptAt: Long? = null,

    /**
     * When sync completed successfully (null if not completed)
     */
    val completedAt: Long? = null,

    /**
     * Number of retry attempts
     */
    val retryCount: Int = 0,

    /**
     * Error message from last failed attempt
     */
    val errorMessage: String? = null,

    /**
     * Whether this is an update to existing activity (vs new creation)
     */
    val isUpdate: Boolean = false
)

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
