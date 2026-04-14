package com.workoutapp.domain.usecase

import android.util.Log
import com.workoutapp.domain.model.ExerciseProfile
import com.workoutapp.domain.model.LoadingPattern
import com.workoutapp.domain.model.SetPrescription
import com.workoutapp.domain.model.SetType
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutPrescription

private const val TAG = "FortisLupus"

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
        history: List<WorkoutExercise>,
        equipment: String = "",
        progressionRate: Float? = null,
        weekInBlock: Int = 1,
        isDeloadWeek: Boolean = false
    ): StrengthPrescription {
        val template = templateForPosition(positionInWorkout)

        if (history.size < 2) {
            Log.d(TAG, "Prescribe pos=$positionInWorkout: history fallback, sessions=${history.size}")
            val base = StrengthPrescription(
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
            return applyBlockProgression(base, positionInWorkout, weekInBlock, isDeloadWeek)
        }

        // Reduce each session to its completed working sets (weight > 0,
        // reps > 0, completed). Sessions with no completed sets are treated
        // as missing history for progression purposes.
        val lastTwo = history.take(2).map { wex ->
            wex.sets.filter { it.completed && it.weight > 0f && it.reps > 0 }
        }

        if (lastTwo.any { it.isEmpty() }) {
            val base = StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = null,
                rationale = "incomplete history — hit target reps to unlock progression"
            )
            return applyBlockProgression(base, positionInWorkout, weekInBlock, isDeloadWeek)
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

        Log.d(TAG, "Prescribe pos=$positionInWorkout: last weight=${mostRecentWorkingWeight.toInt()}lb, cleared=$mostRecentClearedTop")
        Log.d(TAG, "Prescribe pos=$positionInWorkout: equipment=$equipment, rate=${progressionRate ?: "unknown"}, dbSlow=${isDumbbellSlowProgressor(equipment, progressionRate)}")

        val basePrescription = if (
            mostRecentClearedTop &&
            previousClearedTop &&
            mostRecentWorkingWeight == previousWorkingWeight
        ) {
            if (isDumbbellSlowProgressor(equipment, progressionRate)) {
                StrengthPrescription(
                    targetSets = template.sets,
                    targetRepsMin = template.repsMin,
                    targetRepsMax = template.repsMax + 1,
                    recommendedWeight = mostRecentWorkingWeight,
                    rationale = "hold ${mostRecentWorkingWeight.toInt()} lb — aim for ${template.repsMax + 1} reps to earn +${PROGRESSION_DELTA_LB.toInt()} lb"
                )
            } else {
                StrengthPrescription(
                    targetSets = template.sets,
                    targetRepsMin = template.repsMin,
                    targetRepsMax = template.repsMax,
                    recommendedWeight = mostRecentWorkingWeight + PROGRESSION_DELTA_LB,
                    rationale = "+${PROGRESSION_DELTA_LB.toInt()} lb — cleared ${template.repsMax} reps last 2 sessions"
                )
            }
        } else {
            StrengthPrescription(
                targetSets = template.sets,
                targetRepsMin = template.repsMin,
                targetRepsMax = template.repsMax,
                recommendedWeight = mostRecentWorkingWeight,
                rationale = "repeat weight — aim for ${template.repsMax} reps to unlock progression"
            )
        }

        return applyBlockProgression(basePrescription, positionInWorkout, weekInBlock, isDeloadWeek)
    }

    /**
     * Profile-aware prescriber that generates per-set prescriptions with
     * warmup/ramp/working sets based on the user's detected loading pattern.
     */
    fun prescribeFromProfile(
        positionInWorkout: Int,
        profile: ExerciseProfile,
        weekInBlock: Int = 1,
        isDeloadWeek: Boolean = false
    ): WorkoutPrescription {
        Log.d(TAG, "Profile prescribe: ${profile.exerciseId} pos=$positionInWorkout, working=${profile.currentWorkingWeight?.toInt()}lb, pattern=${profile.loadingPattern}, plateau=${profile.plateauFlag}")
        val template = templateForPosition(positionInWorkout)
        val sets = mutableListOf<SetPrescription>()

        // Use profile's preferred scheme when enough data exists, else position defaults
        val workingSets = if (profile.strengthSessionCount >= 3) profile.preferredWorkingSets else template.sets
        val repsMin = if (profile.strengthSessionCount >= 3) profile.preferredRepsMin else template.repsMin
        val repsMax = if (profile.strengthSessionCount >= 3) profile.preferredRepsMax else template.repsMax

        val workingWeight = profile.currentWorkingWeight

        if (profile.bodyweightOnly || workingWeight == null) {
            // Bodyweight or no weight data: flat sets, rep progression
            repeat(workingSets) { i ->
                sets.add(SetPrescription(
                    setType = SetType.WORKING,
                    targetRepsMin = repsMin,
                    targetRepsMax = repsMax,
                    note = if (i == workingSets - 1) "clear $repsMax all sets → bump to ${repsMax + 3} next cycle" else null
                ))
            }
            val bodyweightBase = WorkoutPrescription(
                sets = sets,
                rationale = if (profile.bodyweightOnly) "bodyweight — progress by adding reps"
                    else "first time — find your working weight",
                loadingPattern = LoadingPattern.FLAT,
                bodyweightOnly = profile.bodyweightOnly
            )
            return applyBlockProgressionToWorkoutPrescription(bodyweightBase, positionInWorkout, weekInBlock, isDeloadWeek)
        }

        val rationale: String
        val targetWeight: Float

        // Progression: if plateau, suggest holding. If enough history, check progression.
        if (profile.plateauFlag) {
            targetWeight = workingWeight
            rationale = "plateau at ${workingWeight.toInt()} lb for ${profile.plateauSessionCount} sessions — consider a deload week or exercise swap"
        } else {
            targetWeight = workingWeight
            rationale = "repeat ${workingWeight.toInt()} lb — clear $repsMax reps to unlock +${PROGRESSION_DELTA_LB.toInt()} lb"
        }

        when (profile.loadingPattern) {
            LoadingPattern.RAMP_AND_HOLD -> {
                // Warmup set
                profile.warmupWeight?.let { warmup ->
                    sets.add(SetPrescription(
                        setType = SetType.WARMUP,
                        targetRepsMin = 10,
                        targetRepsMax = 12,
                        recommendedWeight = warmup
                    ))
                }

                // Ramp sets (between warmup and working weight)
                if (profile.rampSteps > 0 && profile.warmupWeight != null) {
                    val rampIncrement = (targetWeight - profile.warmupWeight) / (profile.rampSteps + 1)
                    for (step in 1..profile.rampSteps.coerceAtMost(2)) {
                        val rampWeight = profile.warmupWeight + rampIncrement * step
                        sets.add(SetPrescription(
                            setType = SetType.RAMP,
                            targetRepsMin = repsMin,
                            targetRepsMax = repsMax,
                            recommendedWeight = roundToNearest5(rampWeight)
                        ))
                    }
                }

                // Working sets
                repeat(workingSets.coerceAtLeast(3)) { i ->
                    sets.add(SetPrescription(
                        setType = SetType.WORKING,
                        targetRepsMin = repsMin,
                        targetRepsMax = repsMax,
                        recommendedWeight = targetWeight,
                        note = if (i == workingSets.coerceAtLeast(3) - 1) "clear $repsMax to unlock ${(targetWeight + PROGRESSION_DELTA_LB).toInt()} lb" else null
                    ))
                }
            }

            LoadingPattern.ASCENDING_RAMP -> {
                // 1 light ramp set
                val startWeight = targetWeight * 0.75f
                sets.add(SetPrescription(
                    setType = SetType.RAMP,
                    targetRepsMin = repsMax,
                    targetRepsMax = repsMax,
                    recommendedWeight = roundToNearest5(startWeight)
                ))

                // Working sets with ascending weight
                val increment = (targetWeight - roundToNearest5(startWeight)) / workingSets.coerceAtLeast(2)
                repeat(workingSets.coerceAtLeast(3)) { i ->
                    val setWeight = roundToNearest5(startWeight + increment * (i + 1))
                    sets.add(SetPrescription(
                        setType = SetType.WORKING,
                        targetRepsMin = repsMin,
                        targetRepsMax = repsMax,
                        recommendedWeight = setWeight.coerceAtMost(targetWeight),
                        note = if (i == workingSets.coerceAtLeast(3) - 1) "clear $repsMax to unlock ${(targetWeight + PROGRESSION_DELTA_LB).toInt()} lb" else null
                    ))
                }
            }

            LoadingPattern.FLAT, LoadingPattern.UNKNOWN -> {
                // All sets at same weight
                repeat(workingSets) { i ->
                    sets.add(SetPrescription(
                        setType = SetType.WORKING,
                        targetRepsMin = repsMin,
                        targetRepsMax = repsMax,
                        recommendedWeight = targetWeight,
                        note = if (i == workingSets - 1) "clear $repsMax to unlock ${(targetWeight + PROGRESSION_DELTA_LB).toInt()} lb" else null
                    ))
                }
            }
        }

        val baseWorkout = WorkoutPrescription(
            sets = sets,
            rationale = rationale,
            loadingPattern = profile.loadingPattern
        )
        return applyBlockProgressionToWorkoutPrescription(baseWorkout, positionInWorkout, weekInBlock, isDeloadWeek)
    }

    /**
     * Applies block-weekly progression on top of base prescription.
     * Only affects positions 0 (anchor) and 1 (hypertrophy). Accessory (position 2+)
     * uses base Phase 1 logic unchanged.
     */
    private fun applyBlockProgression(
        base: StrengthPrescription,
        positionInWorkout: Int,
        weekInBlock: Int,
        isDeloadWeek: Boolean
    ): StrengthPrescription {
        if (positionInWorkout >= 2) return base

        if (isDeloadWeek) {
            val deloadWeight = base.recommendedWeight?.let {
                roundToNearest5(it * BlockPeriodization.DELOAD_WEIGHT_PCT)
            }
            val deloadSets = (base.targetSets - 1).coerceAtLeast(2)
            return base.copy(
                targetSets = deloadSets,
                recommendedWeight = deloadWeight,
                rationale = "Deload · ${deloadWeight?.toInt() ?: "bodyweight"} lb, ${deloadSets} sets — recovery week"
            )
        }

        return when (weekInBlock) {
            1 -> base
            2 -> {
                if (base.targetRepsMax <= 5) base
                else base.copy(
                    targetRepsMax = base.targetRepsMax + 1,
                    rationale = "Week 2 · aim for ${base.targetRepsMax + 1} reps (same weight) to earn +5 lb next week"
                )
            }
            3 -> base
            else -> base
        }
    }

    private fun isDumbbellSlowProgressor(equipment: String, progressionRate: Float?): Boolean {
        if (!equipment.contains("Dumbbell", ignoreCase = true)) return false
        return progressionRate != null && progressionRate < 5f
    }

    /**
     * Applies block-weekly progression to the profile-aware WorkoutPrescription.
     * Only affects positions 0 (anchor) and 1 (hypertrophy).
     *
     * Deload: drops the last working set (min 2 working sets retained) and
     * reduces all recommended weights (working + warmup + ramp) by 20%.
     * Week 2: bumps targetRepsMax by 1 on working sets (skip if max <= 5).
     */
    private fun applyBlockProgressionToWorkoutPrescription(
        base: WorkoutPrescription,
        positionInWorkout: Int,
        weekInBlock: Int,
        isDeloadWeek: Boolean
    ): WorkoutPrescription {
        if (positionInWorkout >= 2) return base

        if (isDeloadWeek) {
            val workingCount = base.sets.count { it.setType == SetType.WORKING }
            val keepWorking = (workingCount - 1).coerceAtLeast(2)
            var seen = 0
            val trimmed = base.sets.filter { set ->
                if (set.setType != SetType.WORKING) true
                else {
                    seen++
                    seen <= keepWorking
                }
            }
            val deloadSets = trimmed.map { set ->
                set.copy(
                    recommendedWeight = set.recommendedWeight?.let { roundToNearest5(it * BlockPeriodization.DELOAD_WEIGHT_PCT) },
                    note = null
                )
            }
            return base.copy(
                sets = deloadSets,
                rationale = "Deload · 80% weight, ${keepWorking} working sets — recovery week"
            )
        }

        return when (weekInBlock) {
            2 -> {
                val bumpedSets = base.sets.map { set ->
                    if (set.setType == SetType.WORKING && set.targetRepsMax > 5) {
                        set.copy(targetRepsMax = set.targetRepsMax + 1)
                    } else set
                }
                base.copy(
                    sets = bumpedSets,
                    rationale = "Week 2 · chase +1 rep at same weight to earn +5 lb next week"
                )
            }
            else -> base
        }
    }

    private fun roundToNearest5(weight: Float): Float {
        return (kotlin.math.round(weight / 5f) * 5f)
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
/**
 * Bridge from legacy aggregate prescription to the new per-set model.
 * Used when no ExerciseProfile exists for an exercise.
 */
fun StrengthPrescription.toWorkoutPrescription(): WorkoutPrescription {
    val sets = (1..targetSets).map { i ->
        SetPrescription(
            setType = SetType.WORKING,
            targetRepsMin = targetRepsMin,
            targetRepsMax = targetRepsMax,
            recommendedWeight = recommendedWeight,
            note = if (i == targetSets) rationale else null
        )
    }
    return WorkoutPrescription(
        sets = sets,
        rationale = rationale,
        loadingPattern = LoadingPattern.UNKNOWN
    )
}

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
