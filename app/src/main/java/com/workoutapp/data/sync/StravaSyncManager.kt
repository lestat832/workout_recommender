package com.workoutapp.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.database.entities.SyncStatus
import com.workoutapp.data.database.entities.StravaSyncQueueEntity
import com.workoutapp.data.repository.StravaAuthRepository
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StravaSyncManager"

@Singleton
class StravaSyncManager @Inject constructor(
    private val stravaSyncDao: StravaSyncDao,
    private val stravaAuthRepository: StravaAuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun queueWorkout(workoutId: String) {
        if (!stravaAuthRepository.isAuthenticated()) {
            Log.d(TAG, "Skipping sync for $workoutId: Strava not authenticated")
            return
        }
        val existing = stravaSyncDao.getByWorkoutId(workoutId)
        if (existing != null) {
            when (existing.status) {
                SyncStatus.COMPLETED -> {
                    Log.d(TAG, "Skipping sync for $workoutId: already synced")
                    return
                }
                SyncStatus.PENDING, SyncStatus.IN_PROGRESS -> {
                    Log.d(TAG, "Skipping sync for $workoutId: already queued (status=${existing.status})")
                    return
                }
                SyncStatus.FAILED -> {
                    // Reset failed row to PENDING for retry
                    Log.d(TAG, "Resetting failed sync for $workoutId to PENDING")
                    stravaSyncDao.updateStatus(existing.id, SyncStatus.PENDING)
                    // Fall through to enqueue worker
                }
            }
        } else {
            val entry = StravaSyncQueueEntity(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                status = SyncStatus.PENDING
            )
            stravaSyncDao.insert(entry)
            Log.d(TAG, "Queued workout $workoutId for Strava sync (entry ${entry.id})")
        }

        val workRequest = OneTimeWorkRequestBuilder<StravaSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "strava_sync",
                ExistingWorkPolicy.APPEND,
                workRequest
            )
    }
}
