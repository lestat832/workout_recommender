package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.CompletedWorkoutSummary
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Passive fatigue warnings — does not change prescriptions.
 *
 * Two signals:
 *  1. Muscle overlap: today's plan hits a muscle group trained <48h ago.
 *  2. Intensity stacking: last two completed sessions were HARD and today is also HARD.
 *
 * Classification rules (derived from best-practice trainer guidance, not measured):
 *   STRENGTH             -> HARD
 *   EMOM  >= 20 min      -> HARD
 *   EMOM  <  20 min      -> MODERATE
 *   AMRAP <  15 min      -> EASY
 *   AMRAP == 15 min      -> MODERATE
 *   AMRAP >  15 min      -> MODERATE (defensive; long AMRAPs rare)
 */
object FatigueAwareness {

    const val OVERLAP_WINDOW_HOURS = 48L
    const val INTENSITY_STACK_THRESHOLD = 2
    const val INTENSITY_LOOKBACK_DAYS = 7L
    private const val EMOM_HARD_MINUTES = 20

    /** Muscle groups excluded from the overlap check because they're trained too often. */
    val OVERLAP_EXCLUDED_MUSCLES: Set<MuscleGroup> = setOf(MuscleGroup.CORE)

    enum class Intensity { HARD, MODERATE, EASY }

    fun classify(format: WorkoutFormat, durationMinutes: Int?): Intensity {
        return when (format) {
            WorkoutFormat.STRENGTH -> Intensity.HARD
            WorkoutFormat.EMOM -> {
                val mins = durationMinutes ?: EMOM_HARD_MINUTES
                if (mins >= EMOM_HARD_MINUTES) Intensity.HARD else Intensity.MODERATE
            }
            WorkoutFormat.AMRAP -> {
                val mins = durationMinutes ?: 15
                when {
                    mins < 15 -> Intensity.EASY
                    else -> Intensity.MODERATE
                }
            }
        }
    }
}
