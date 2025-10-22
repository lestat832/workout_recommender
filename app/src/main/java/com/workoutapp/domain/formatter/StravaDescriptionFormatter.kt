package com.workoutapp.domain.formatter

import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutType
import kotlin.math.roundToInt

/**
 * Formats workout data into Strava activity description.
 * Implements Format A: Detailed with emojis and muscle group grouping.
 *
 * Example output:
 * ```
 * 💪 PUSH Workout
 *
 * Chest:
 * • Barbell Bench Press: 3×10 @ 135 lbs
 * • Incline Dumbbell Press: 3×12 @ 50 lbs
 *
 * Legs:
 * • Squats: 4×8 @ 185 lbs
 *
 * Total Volume: 12,450 lbs
 * Duration: 58 minutes
 * ```
 */
object StravaDescriptionFormatter {

    /**
     * Format workout into detailed Strava description with muscle group organization
     */
    fun format(
        workout: Workout,
        startTime: Long? = null,
        endTime: Long? = null
    ): String {
        return buildString {
            // Header with emoji
            appendLine(formatHeader(workout.type))
            appendLine()

            // Group exercises by muscle groups
            val exercisesByMuscle = groupExercisesByMuscleGroup(workout.exercises)

            // Format each muscle group section
            exercisesByMuscle.forEach { (muscleGroup, exercises) ->
                appendLine("${muscleGroup.displayName}:")
                exercises.forEach { workoutExercise ->
                    appendLine(formatExercise(workoutExercise))
                }
                appendLine()
            }

            // Total volume
            val totalVolume = calculateTotalVolume(workout.exercises)
            appendLine("Total Volume: ${formatVolume(totalVolume)}")

            // Duration (if available)
            if (startTime != null && endTime != null) {
                val durationMinutes = calculateDuration(startTime, endTime)
                appendLine("Duration: $durationMinutes minutes")
            }
        }.trim()
    }

    /**
     * Format the workout header with emoji
     */
    private fun formatHeader(workoutType: WorkoutType): String {
        return when (workoutType) {
            WorkoutType.PUSH -> "💪 PUSH Workout"
            WorkoutType.PULL -> "🔥 PULL Workout"
        }
    }

    /**
     * Group exercises by their primary muscle group
     * Exercises can have multiple muscle groups, but we use the first one for grouping
     */
    private fun groupExercisesByMuscleGroup(
        exercises: List<WorkoutExercise>
    ): Map<MuscleGroup, List<WorkoutExercise>> {
        return exercises
            .filter { it.exercise.muscleGroups.isNotEmpty() }
            .groupBy { it.exercise.muscleGroups.first() }
            .toSortedMap(compareBy { it.displayOrder })
    }

    /**
     * Format a single exercise line
     * Example: • Barbell Bench Press: 3×10 @ 135 lbs
     */
    private fun formatExercise(workoutExercise: WorkoutExercise): String {
        val exercise = workoutExercise.exercise
        val completedSets = workoutExercise.sets.filter { it.completed }

        if (completedSets.isEmpty()) {
            return "• ${exercise.name}: 0 sets"
        }

        // Group sets by reps and weight to show consolidated format
        val setGroups = completedSets.groupBy { Pair(it.reps, it.weight) }

        return if (setGroups.size == 1) {
            // All sets are the same: 3×10 @ 135 lbs
            val (reps, weight) = setGroups.keys.first()
            val setCount = completedSets.size
            "• ${exercise.name}: ${setCount}×${reps} @ ${weight.roundToInt()} lbs"
        } else {
            // Sets vary: show each set
            val setsString = completedSets
                .mapIndexed { index, set ->
                    "${set.reps} @ ${set.weight.roundToInt()} lbs"
                }
                .joinToString(", ")
            "• ${exercise.name}: $setsString"
        }
    }

    /**
     * Calculate total volume (sets × reps × weight) across all exercises
     */
    private fun calculateTotalVolume(exercises: List<WorkoutExercise>): Float {
        return exercises.sumOf { workoutExercise ->
            workoutExercise.sets
                .filter { it.completed }
                .sumOf { set ->
                    (set.reps * set.weight).toDouble()
                }
        }.toFloat()
    }

    /**
     * Format volume with appropriate units
     * Examples: 450 lbs, 12,450 lbs, 123,450 lbs
     */
    private fun formatVolume(volume: Float): String {
        val volumeInt = volume.roundToInt()
        return when {
            volumeInt >= 1_000_000 -> String.format("%,d lbs", volumeInt)
            volumeInt >= 1_000 -> {
                val thousands = volumeInt / 1_000
                val remainder = volumeInt % 1_000
                if (remainder == 0) {
                    "${thousands}K lbs"
                } else {
                    String.format("%,d lbs", volumeInt)
                }
            }
            else -> "$volumeInt lbs"
        }
    }

    /**
     * Calculate workout duration in minutes
     */
    private fun calculateDuration(startTime: Long, endTime: Long): Int {
        val durationMillis = endTime - startTime
        val durationMinutes = (durationMillis / 1000 / 60).toInt()
        return durationMinutes.coerceAtLeast(1) // At least 1 minute
    }

    /**
     * Display name for muscle groups in formatted output
     */
    private val MuscleGroup.displayName: String
        get() = when (this) {
            MuscleGroup.CHEST -> "Chest"
            MuscleGroup.SHOULDER -> "Shoulders"
            MuscleGroup.TRICEP -> "Triceps"
            MuscleGroup.LEGS -> "Legs"
            MuscleGroup.BACK -> "Back"
            MuscleGroup.BICEP -> "Biceps"
            MuscleGroup.CORE -> "Core"
        }

    /**
     * Display order for muscle groups (used for sorting sections)
     */
    private val MuscleGroup.displayOrder: Int
        get() = when (this) {
            MuscleGroup.CHEST -> 1
            MuscleGroup.SHOULDER -> 2
            MuscleGroup.TRICEP -> 3
            MuscleGroup.BACK -> 4
            MuscleGroup.BICEP -> 5
            MuscleGroup.LEGS -> 6
            MuscleGroup.CORE -> 7
        }
}
