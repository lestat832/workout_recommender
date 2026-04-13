package com.workoutapp.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.database.entities.SyncStatus
import com.workoutapp.data.remote.strava.StravaApiClient
import com.workoutapp.data.repository.StravaAuthRepository
import com.workoutapp.domain.mapper.WorkoutToStravaMapper
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StravaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val stravaSyncDao: StravaSyncDao,
    private val workoutRepository: WorkoutRepository,
    private val stravaAuthRepository: StravaAuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Recover stale IN_PROGRESS rows (worker died mid-sync)
        val stale = stravaSyncDao.getStaleInProgress()
        for (entry in stale) {
            Log.d(TAG, "Recovering stale IN_PROGRESS entry ${entry.id} for workout ${entry.workoutId}")
            stravaSyncDao.updateStatus(entry.id, SyncStatus.PENDING)
        }

        val pending = stravaSyncDao.getAllPending() +
            stravaSyncDao.getAllFailed().filter { it.retryCount < 3 }
        Log.d(TAG, "StravaSyncWorker started: ${pending.size} entries to process")
        if (pending.isEmpty()) return Result.success()

        val api = StravaApiClient.api
        var anyFailed = false

        for (entry in pending) {
            stravaSyncDao.updateStatus(
                entry.id, SyncStatus.IN_PROGRESS, System.currentTimeMillis()
            )

            try {
                val workout = workoutRepository.getWorkoutById(entry.workoutId)
                    ?: throw Exception("Workout not found: ${entry.workoutId}")

                Log.d(TAG, "Syncing workout ${entry.workoutId} (format=${workout.format}, exercises=${workout.exercises.size})")

                val tokenResult = stravaAuthRepository.getValidAccessToken()
                val token = tokenResult.getOrThrow()
                Log.d(TAG, "Token acquired for workout ${entry.workoutId}")

                val request = WorkoutToStravaMapper.mapToActivityRequest(workout)
                val response = api.createActivity("Bearer $token", request)

                if (response.isSuccessful) {
                    val activityId = response.body()?.id
                        ?: throw Exception("Empty response body from Strava")
                    stravaSyncDao.markCompleted(
                        entry.id, activityId, System.currentTimeMillis()
                    )
                    Log.d(TAG, "Synced workout ${entry.workoutId} -> Strava activity $activityId")
                } else {
                    throw Exception("Strava API error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed for workout ${entry.workoutId}: ${e.message}", e)
                stravaSyncDao.markFailed(
                    entry.id, e.message ?: "Unknown error", System.currentTimeMillis()
                )
                if (entry.retryCount < 3) anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "StravaSyncWorker"
    }
}
