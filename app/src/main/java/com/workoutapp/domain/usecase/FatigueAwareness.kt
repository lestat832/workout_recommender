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

    /**
     * Returns a warning if any planned muscle group was trained in a completed
     * workout within OVERLAP_WINDOW_HOURS of [now]. Else null.
     */
    fun checkMuscleOverlap(
        plannedMuscleGroups: Set<MuscleGroup>,
        recentWorkouts: List<CompletedWorkoutSummary>,
        now: Date
    ): String? {
        val planned = plannedMuscleGroups - OVERLAP_EXCLUDED_MUSCLES
        if (planned.isEmpty()) return null

        val windowStart = now.time - TimeUnit.HOURS.toMillis(OVERLAP_WINDOW_HOURS)

        val overlaps = mutableMapOf<MuscleGroup, Long>() // muscle -> hours since most-recent hit
        for (w in recentWorkouts) {
            if (w.date.time < windowStart) continue
            if (w.date.time > now.time) continue // ignore workouts dated in the future of `now`
            val recentMuscles = w.muscleGroups - OVERLAP_EXCLUDED_MUSCLES
            val hoursAgo = TimeUnit.MILLISECONDS.toHours(now.time - w.date.time).coerceAtLeast(0)
            for (m in planned.intersect(recentMuscles)) {
                val existing = overlaps[m]
                if (existing == null || hoursAgo < existing) overlaps[m] = hoursAgo
            }
        }

        if (overlaps.isEmpty()) return null

        val listed = overlaps.entries.sortedBy { it.value }.take(2)
        val muscleLabels = listed.joinToString(", ") { it.key.toFriendlyName() }
        val minHours = listed.first().value
        return "$muscleLabels trained ${minHours}h ago — recovery may be incomplete"
    }

    /**
     * Returns a warning if today's plan is HARD and the last
     * INTENSITY_STACK_THRESHOLD completed sessions were also HARD. Else null.
     *
     * Caller passes `recentCompleted` pre-filtered to the last INTENSITY_LOOKBACK_DAYS
     * (from whatever "now" they're using) and sorted date desc. Workouts dated
     * after [now] are ignored — the debug date-offset testing tool can leave
     * future-dated completed workouts in the DB, and those must not count as
     * "prior" sessions for the stacking check.
     */
    fun checkIntensityStacking(
        recentCompleted: List<CompletedWorkoutSummary>,
        plannedIntensity: Intensity,
        now: Date
    ): String? {
        if (plannedIntensity != Intensity.HARD) return null

        val past = recentCompleted.filter { it.date.time <= now.time }
        if (past.size < INTENSITY_STACK_THRESHOLD) return null

        val lastN = past.take(INTENSITY_STACK_THRESHOLD)
        val allHard = lastN.all { classify(it.format, it.durationMinutes) == Intensity.HARD }
        if (!allHard) return null

        return "Last $INTENSITY_STACK_THRESHOLD sessions were hard — consider easing up today"
    }

    private fun MuscleGroup.toFriendlyName(): String = when (this) {
        MuscleGroup.CHEST -> "Chest"
        MuscleGroup.SHOULDER -> "Shoulders"
        MuscleGroup.TRICEP -> "Triceps"
        MuscleGroup.LEGS -> "Legs"
        MuscleGroup.BACK -> "Back"
        MuscleGroup.BICEP -> "Biceps"
        MuscleGroup.CORE -> "Core"
    }
}
