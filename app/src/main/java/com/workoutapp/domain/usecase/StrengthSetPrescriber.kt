package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.WorkoutExercise

/**
 * Stateless set/rep/weight prescriber for LMU strength workouts. Given an
 * exercise's position in the generated workout and the user's recent
 * completed history for that same exercise id, returns a [StrengthPrescription]
 * describing the target sets, rep range, and (if history supports it)
 * recommended working weight with a progression rationale.
 *
 * Prescription logic from a 10x-trainer lens:
 *
 *  1. **Position-based default set scheme.** First exercise in the workout
 *     is the anchor compound lift (heavier, lower reps). Second is
 *     hypertrophy focus (moderate reps). Third+ is accessory volume work
 *     (higher reps). Matches the conventional heavy-first programming of
 *     most strength templates.
 *
 *  2. **History-based weight progression.** If the user has at least two
 *     completed sessions of this exact exercise and cleared the top of the
 *     rep range cleanly at the same working weight both times, prescribe
 *     +5 lb. If history is incomplete or inconsistent, repeat the last
 *     working weight and keep the rep target. First-time exercises get the
 *     default scheme with no weight recommendation.
 *
 *  3. **Non-binding display.** The prescription is informational — the UI
 *     still lets the user freely enter their own reps and weight per set.
 *     This matches the "recommendation that can be overruled" contract.
 *
 * This is a pure function — no repository access. The caller (viewmodel)
 * fetches history and passes it in, which keeps the prescriber free of
 * Android/data-layer dependencies.
 */
object StrengthSetPrescriber {

    /**
     * Prescribe a set scheme + optional weight for [positionInWorkout] given
     * the user's recent completed sessions of this exercise. [history] should
     * be the last N `WorkoutExercise` rows for the same exercise id, newest
     * first. A list of fewer than 2 entries triggers the "first time" path.
     */
    fun prescribe(
        positionInWorkout: Int,
        history: List<WorkoutExercise>
    ): StrengthPrescription {
        val template = templateForPosition(positionInWorkout)

        if (history.size < 2) {
            return StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = null,
                rationale = if (history.isEmpty()) {
                    "first time — find your working weight"
                } else {
                    "returning — match last session and build history"
                }
            )
        }

        // Reduce each session to its completed working sets (weight > 0,
        // reps > 0, completed). Sessions with no completed sets are treated
        // as missing history for progression purposes.
        val lastTwo = history.take(2).map { wex ->
            wex.sets.filter { it.completed && it.weight > 0f && it.reps > 0 }
        }

        if (lastTwo.any { it.isEmpty() }) {
            return StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = null,
                rationale = "incomplete history — hit target reps to unlock progression"
            )
        }

        val mostRecentWorkingWeight = lastTwo[0].maxOf { it.weight }
        val previousWorkingWeight = lastTwo[1].maxOf { it.weight }

        // "Cleared the top of the rep range" means every working set at the
        // heaviest weight hit at least the top end of the target range.
        // Strict check: catches the case where the user did 6 reps instead
        // of 8 on the last set and rounds down — you haven't earned the
        // progression yet.
        val mostRecentClearedTop = lastTwo[0]
            .filter { it.weight >= mostRecentWorkingWeight }
            .all { it.reps >= template.repsMax }
        val previousClearedTop = lastTwo[1]
            .filter { it.weight >= previousWorkingWeight }
            .all { it.reps >= template.repsMax }

        return if (
            mostRecentClearedTop &&
            previousClearedTop &&
            mostRecentWorkingWeight == previousWorkingWeight
        ) {
            StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = mostRecentWorkingWeight + PROGRESSION_DELTA_LB,
                rationale = "+${PROGRESSION_DELTA_LB.toInt()} lb — cleared ${template.repsMax} reps last 2 sessions"
            )
        } else {
            StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = mostRecentWorkingWeight,
                rationale = "repeat weight — aim for ${template.repsMax} reps to unlock progression"
            )
        }
    }

    private data class Template(val sets: Int, val repsMin: Int, val repsMax: Int)

    private fun templateForPosition(position: Int): Template = when (position) {
        0 -> Template(sets = 4, repsMin = 6, repsMax = 8)   // anchor compound
        1 -> Template(sets = 3, repsMin = 8, repsMax = 10)  // hypertrophy
        else -> Template(sets = 3, repsMin = 10, repsMax = 12) // accessory volume
    }

    private const val PROGRESSION_DELTA_LB = 5f
}

/**
 * Prescribed set scheme for an LMU strength exercise. Generated by
 * [StrengthSetPrescriber.prescribe] and displayed read-only in
 * `WorkoutScreen`'s ExerciseCard. The user is free to override per-set
 * by typing their own reps/weight.
 */
data class StrengthPrescription(
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val recommendedWeight: Float?,
    val rationale: String
) {
    /**
     * One-line display string like "Target: 4 × 6-8 @ 190 lb" or
     * "Target: 4 × 6-8" when no weight is recommended.
     */
    fun displayLine(): String {
        val repRange = if (targetRepsMin == targetRepsMax) "${targetRepsMin}"
                       else "${targetRepsMin}-${targetRepsMax}"
        val base = "Target: $targetSets × $repRange"
        val weight = recommendedWeight?.let { w ->
            if (w % 1f == 0f) " @ ${w.toInt()} lb" else " @ $w lb"
        } ?: ""
        return base + weight
    }
}
