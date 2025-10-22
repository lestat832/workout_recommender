package com.workoutapp.domain.mapper

import com.workoutapp.data.remote.strava.StravaActivityRequest
import com.workoutapp.domain.formatter.StravaDescriptionFormatter
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Maps Workout domain model to Strava API request format
 *
 * Handles:
 * - Activity naming (💪 PUSH Workout / 🔥 PULL Workout)
 * - Date formatting (ISO-8601 format: yyyy-MM-dd'T'HH:mm:ss'Z')
 * - Duration calculation (seconds)
 * - Description formatting (Format A with muscle group organization)
 */
object WorkoutToStravaMapper {

    /**
     * Convert a Workout to StravaActivityRequest
     *
     * @param workout The workout to convert
     * @param startTime Optional workout start time (millis since epoch)
     * @param endTime Optional workout end time (millis since epoch)
     * @return StravaActivityRequest ready to send to API
     */
    fun mapToActivityRequest(
        workout: Workout,
        startTime: Long? = null,
        endTime: Long? = null
    ): StravaActivityRequest {
        // Use provided times or fall back to workout date
        val actualStartTime = startTime ?: workout.date.time
        val actualEndTime = endTime ?: (actualStartTime + estimateDuration(workout))

        return StravaActivityRequest(
            name = formatActivityName(workout.type),
            type = "WeightTraining",
            sportType = "WeightTraining",
            startDateLocal = formatDateToIso8601(actualStartTime),
            elapsedTime = calculateElapsedTimeSeconds(actualStartTime, actualEndTime),
            description = StravaDescriptionFormatter.format(
                workout = workout,
                startTime = actualStartTime,
                endTime = actualEndTime
            ),
            trainer = false,
            commute = false
        )
    }

    /**
     * Format activity name with emoji based on workout type
     */
    private fun formatActivityName(workoutType: WorkoutType): String {
        return when (workoutType) {
            WorkoutType.PUSH -> "💪 PUSH Workout"
            WorkoutType.PULL -> "🔥 PULL Workout"
        }
    }

    /**
     * Format timestamp to ISO-8601 format for Strava API
     * Format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * Example: "2025-10-21T14:30:00Z"
     */
    private fun formatDateToIso8601(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Calculate elapsed time in seconds
     */
    private fun calculateElapsedTimeSeconds(startTime: Long, endTime: Long): Int {
        val durationMillis = endTime - startTime
        val durationSeconds = (durationMillis / 1000).toInt()
        return durationSeconds.coerceAtLeast(60) // Minimum 1 minute
    }

    /**
     * Estimate workout duration if not provided
     * Based on number of exercises and sets
     *
     * Estimation logic:
     * - Base time: 5 minutes (warm-up/setup)
     * - Per exercise: 3 minutes
     * - Per set: 1.5 minutes (includes rest time)
     */
    private fun estimateDuration(workout: Workout): Long {
        val baseTimeMillis = 5 * 60 * 1000L // 5 minutes
        val exerciseTimeMillis = workout.exercises.size * 3 * 60 * 1000L // 3 min per exercise

        val totalSets = workout.exercises.sumOf { workoutExercise ->
            workoutExercise.sets.count { it.completed }
        }
        val setTimeMillis = totalSets * 90 * 1000L // 1.5 minutes per set (including rest)

        return baseTimeMillis + exerciseTimeMillis + setTimeMillis
    }
}
