package com.workoutapp.data.sync

import android.content.Context
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
        val pending = stravaSyncDao.getAllPending() +
            stravaSyncDao.getAllFailed().filter { it.retryCount < 3 }
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

                val tokenResult = stravaAuthRepository.getValidAccessToken()
                val token = tokenResult.getOrThrow()

                val request = WorkoutToStravaMapper.mapToActivityRequest(workout)
                val response = api.createActivity("Bearer $token", request)

                if (response.isSuccessful) {
                    val activityId = response.body()?.id
                        ?: throw Exception("Empty response body from Strava")
                    stravaSyncDao.markCompleted(
                        entry.id, activityId, System.currentTimeMillis()
                    )
                } else {
                    throw Exception("Strava API error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                stravaSyncDao.markFailed(
                    entry.id, e.message ?: "Unknown error", System.currentTimeMillis()
                )
                if (entry.retryCount < 3) anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
