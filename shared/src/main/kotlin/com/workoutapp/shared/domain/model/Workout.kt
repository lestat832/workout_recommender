package com.workoutapp.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Workout(
    val id: String,
    val date: Instant,
    val type: WorkoutType,
    val status: WorkoutStatus,
    val exercises: List<WorkoutExercise> = emptyList()
) {
    // Helper function to convert to Java Date for Android compatibility
    fun toDate(): Date = Date(date.toEpochMilliseconds())
}

@Serializable
enum class WorkoutStatus {
    IN_PROGRESS,
    COMPLETED,
    INCOMPLETE
}

@Serializable
data class WorkoutExercise(
    val id: String,
    val workoutId: String,
    val exercise: Exercise,
    val sets: List<Set>
)

@Serializable
data class Set(
    val reps: Int,
    val weight: Float,
    val completed: Boolean = false
)