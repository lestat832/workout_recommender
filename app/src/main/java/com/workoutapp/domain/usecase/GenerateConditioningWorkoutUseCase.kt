package com.workoutapp.domain.usecase

import com.workoutapp.data.database.HomeGymMovementCatalog
import com.workoutapp.data.database.HomeGymMovementCatalog.Bucket
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.GeneratedConditioningWorkout
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Generates a Home Gym conditioning workout from the hand-curated
 * [HomeGymMovementCatalog]. Picks an EMOM or AMRAP format and composes
 * the station list according to the sequencing rule
 * **legs → pull → push → core**.
 *
 *  - EMOM (20 min, always 4-station): lower + upper-pull + upper-push +
 *    (core ∪ conditioning). Deterministic order — this is the "structured
 *    strength" format. No cardio by design; if the user wants cardio-driven
 *    work they skip to AMRAP.
 *  - AMRAP (20 min, always 3-station): cardio + strength
 *    (lower ∪ upper-pull ∪ upper-push) + (core ∪ conditioning). The "loose
 *    metcon" format — cardio-first by design for metabolic conditioning.
 *
 * Monthly de-duplication fingerprints the sorted movement ids and compares
 * against any conditioning workout completed at this gym this calendar month.
 * Up to [MAX_RETRIES] resamples on match; falls back to the last candidate
 * after exhaustion.
 */
class GenerateConditioningWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) {
    companion object {
        // EMOM runs 4 stations × 5 rounds = full 20 min. Each 60s station
        // has built-in rest, so the long duration is sustainable.
        const val EMOM_DURATION_MINUTES = 20
        // AMRAP is continuous effort with no built-in rest. 15 min hits the
        // sweet spot for a 3-station metcon — long enough to feel the
        // grind, short enough to keep pace honest instead of turning into
        // a pace-yourself slog.
        const val AMRAP_DURATION_MINUTES = 15
        private const val MAX_RETRIES = 5
        private const val MAX_TRX_PER_SESSION = 2
    }

    /**
     * Predicts the next conditioning format by alternating from the last
     * completed conditioning workout at this gym within the current week.
     * Falls back to EMOM when no history exists or the last workout was
     * in a prior week.
     */
    suspend fun predictNextFormat(gymId: Long): WorkoutFormat {
        val lastWorkout = workoutRepository.getLastCompletedWorkoutByGym(gymId)
        if (lastWorkout == null) return WorkoutFormat.EMOM

        val weekStart = currentWeekStartMonday()
        if (lastWorkout.date.before(weekStart)) return WorkoutFormat.EMOM

        return when (lastWorkout.format) {
            WorkoutFormat.EMOM -> WorkoutFormat.AMRAP
            WorkoutFormat.AMRAP -> WorkoutFormat.EMOM
            else -> WorkoutFormat.EMOM
        }
    }

    private fun currentWeekStartMonday(): java.util.Date {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val daysFromMonday = (cal.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.MONDAY + 7) % 7
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysFromMonday)
        return cal.time
    }

    suspend operator fun invoke(
        gymId: Long,
        formatOverride: WorkoutFormat? = null
    ): GeneratedConditioningWorkout {
        // Skip-button flow: when the caller forces a format (user tapped skip
        // on the NextWorkoutCard), use it verbatim. Otherwise pick fresh via
        // predictNextFormat so this path stays consistent with the preview.
        val format = formatOverride ?: predictNextFormat(gymId)
        val expectedCount = expectedStationCount(format)

        val existingFingerprints = workoutRepository
            .getConditioningWorkoutsInMonth(gymId)
            .map { fingerprint(it.exercises.map { e -> e.exercise.id }) }
            .toSet()

        val lastPerformedDates = workoutRepository.getExerciseLastPerformedDates()

        var candidateIds = pickMovementIds(format, lastPerformedDates)
        var attempts = 0
        while (fingerprint(candidateIds) in existingFingerprints && attempts < MAX_RETRIES) {
            candidateIds = pickMovementIds(format, lastPerformedDates)
            attempts++
        }

        val exercises = candidateIds.mapNotNull { exerciseRepository.getExerciseById(it) }
        if (exercises.size < expectedCount) {
            throw IllegalStateException(
                "Home gym catalog incomplete: $format expected $expectedCount exercises, " +
                    "found ${exercises.size}. Check HomeGymCatalogSeeder ran on install " +
                    "and that every bucket the $format shape requires is populated."
            )
        }

        return GeneratedConditioningWorkout(
            format = format,
            exercises = exercises,
            durationMinutes = when (format) {
                WorkoutFormat.EMOM -> EMOM_DURATION_MINUTES
                WorkoutFormat.AMRAP -> AMRAP_DURATION_MINUTES
                else -> EMOM_DURATION_MINUTES
            }
        )
    }

    private fun expectedStationCount(format: WorkoutFormat): Int = when (format) {
        WorkoutFormat.EMOM -> 4
        WorkoutFormat.AMRAP -> 3
        else -> error("Unsupported conditioning format: $format")
    }

    /**
     * Picks movement ids according to the format-specific builder rules.
     *
     * EMOM enforces **legs → pull → push → core** strictly. The order here is
     * load-bearing: ConditioningWorkoutScreen renders the returned list
     * top-to-bottom as the visible station sequence during the session.
     *
     * AMRAP stays as cardio + strength + closer — a deliberate break from the
     * strict sequencing rule so the two formats feel different in the hand.
     *
     * EMOM also enforces a "one Heavy DB per session" cap: picking a heavy
     * movement in an earlier strength slot excludes heavies from subsequent
     * strength slots. Prevents brutal all-heavy metcons where a 60s clock
     * across three compound lifts degrades form by round 3-4.
     */
    private fun pickMovementIds(format: WorkoutFormat, lastPerformedDates: Map<String, java.util.Date>): List<String> {
        val byBucket = HomeGymMovementCatalog.byBucket()
        val cardio = byBucket[Bucket.CARDIO].orEmpty()
        val lower = byBucket[Bucket.LOWER_BODY].orEmpty()
        val pull = byBucket[Bucket.UPPER_PULL].orEmpty()
        val push = byBucket[Bucket.UPPER_PUSH].orEmpty()
        val core = byBucket[Bucket.CORE].orEmpty()
        val conditioning = byBucket[Bucket.CONDITIONING_BODYWEIGHT].orEmpty()

        return when (format) {
            WorkoutFormat.EMOM -> {
                val legsPick = ExerciseFreshness.weightedRandom(lower, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: lower.random()
                // After each strength pick, if a heavy has already been used
                // filter the next pool to exclude heavies. `.ifEmpty { pool }`
                // is a safety net for hypothetical futures where every pull
                // or push is heavy — today only 1 of 5 is, so the filter
                // never empties, but the fallback keeps the generator safe.
                val pullPool = if (legsPick.isHeavy()) pull.filterNot { it.isHeavy() } else pull
                val pullCandidates = pullPool.ifEmpty { pull }
                val pullPick = ExerciseFreshness.weightedRandom(pullCandidates, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: pullCandidates.random()
                val heavyUsed = legsPick.isHeavy() || pullPick.isHeavy()
                val pushPool = if (heavyUsed) push.filterNot { it.isHeavy() } else push
                val pushCandidates = pushPool.ifEmpty { push }
                val pushPick = ExerciseFreshness.weightedRandom(pushCandidates, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: pushCandidates.random()
                val closerPool = core + conditioning
                val closerPick = ExerciseFreshness.weightedRandom(closerPool, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: closerPool.random()
                val picks = mutableListOf(legsPick, pullPick, pushPick, closerPick)
                capTrx(picks, listOf(lower, pull, push, core + conditioning))
                picks.map { it.id }
            }
            WorkoutFormat.AMRAP -> {
                val cardioPick = ExerciseFreshness.weightedRandom(cardio, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: cardio.random()
                val strengthPool = lower + pull + push
                val strengthPick = ExerciseFreshness.weightedRandom(strengthPool, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: strengthPool.random()
                val closerPool = core + conditioning
                val closerPick = ExerciseFreshness.weightedRandom(closerPool, weightFn = {
                    ExerciseFreshness.weight(it.id, lastPerformedDates)
                }) ?: closerPool.random()
                val picks = mutableListOf(cardioPick, strengthPick, closerPick)
                capTrx(picks, listOf(cardio, lower + pull + push, core + conditioning))
                picks.map { it.id }
            }
            else -> error("Unsupported conditioning format: $format")
        }
    }

    /**
     * Identifies "Heavy DB" movements by their stable id prefix. These are
     * the four one-DB heavy compound lifts (goblet squat, lunge, row, push
     * press) that load the session hardest and are capped at one per EMOM.
     */
    private fun HomeGymMovementCatalog.Movement.isHeavy(): Boolean =
        id.startsWith("custom_heavy_")

    private fun HomeGymMovementCatalog.Movement.isTrx(): Boolean =
        equipment == "Suspension Trainer"

    /**
     * Enforces max 2 TRX exercises per session to avoid excessive strap
     * adjustment overhead within 60-second EMOM windows. Re-rolls excess
     * TRX picks from the same bucket pool. If no non-TRX alternative
     * exists in a bucket, keeps the original pick.
     */
    private fun capTrx(
        picks: MutableList<HomeGymMovementCatalog.Movement>,
        bucketPools: List<List<HomeGymMovementCatalog.Movement>>
    ) {
        val trxIndices = picks.indices.filter { picks[it].isTrx() }
        if (trxIndices.size <= MAX_TRX_PER_SESSION) return
        for (i in trxIndices.drop(MAX_TRX_PER_SESSION)) {
            val pool = bucketPools[i].filterNot { it.isTrx() || it.id in picks.map { p -> p.id } }
            if (pool.isNotEmpty()) picks[i] = pool.random()
        }
    }

    private fun fingerprint(ids: List<String>): String = ids.sorted().joinToString(",")
}
