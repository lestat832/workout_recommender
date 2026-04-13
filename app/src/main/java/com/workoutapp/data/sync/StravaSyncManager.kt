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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaSyncManager @Inject constructor(
    private val stravaSyncDao: StravaSyncDao,
    private val stravaAuthRepository: StravaAuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun queueWorkout(workoutId: String) {
        if (!stravaAuthRepository.isAuthenticated()) return
        if (stravaSyncDao.isWorkoutSynced(workoutId)) return

        val entry = StravaSyncQueueEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            status = SyncStatus.PENDING
        )
        stravaSyncDao.insert(entry)

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
