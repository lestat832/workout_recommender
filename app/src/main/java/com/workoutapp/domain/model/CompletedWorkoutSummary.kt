package com.workoutapp.domain.model

import java.util.Date
import kotlin.collections.Set

/**
 * Lightweight summary of a completed workout, used by FatigueAwareness.
 * Carries only what's needed for muscle-overlap and intensity-stacking checks.
 */
data class CompletedWorkoutSummary(
    val id: String,
    val date: Date,
    val format: WorkoutFormat,
    val durationMinutes: Int?,
    val muscleGroups: Set<MuscleGroup>
)
