package com.workoutapp.domain.usecase

import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.remote.strava.StravaApiClient
import com.workoutapp.data.repository.StravaAuthRepository
import com.workoutapp.domain.repository.WorkoutRepository
import javax.inject.Inject

class DeleteWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val stravaSyncDao: StravaSyncDao,
    private val stravaAuthRepository: StravaAuthRepository,
    private val profileComputerUseCase: ProfileComputerUseCase
) {
    suspend operator fun invoke(workoutId: String) {
        // Attempt Strava deletion if synced
        val stravaActivityId = stravaSyncDao.getStravaActivityId(workoutId)
        if (stravaActivityId != null) {
            try {
                val token = stravaAuthRepository.getValidAccessToken().getOrNull()
                if (token != null) {
                    StravaApiClient.api.deleteActivity("Bearer $token", stravaActivityId)
                }
            } catch (_: Exception) {
                // Strava delete is best-effort — proceed with local delete
            }
        }

        // Clean up all sync queue entries for this workout
        stravaSyncDao.deleteByWorkoutId(workoutId)

        // Delete workout + exercises from local DB
        workoutRepository.deleteWorkout(workoutId)

        // Recompute training profile (handles zero-workout case)
        profileComputerUseCase.recomputeFullProfile()
    }
}
