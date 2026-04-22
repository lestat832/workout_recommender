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
        // Use conditioning-only lookup so mixed-format history at this gym
        // (hypothetical today; real if future versions allow) can't let a
        // strength session misdirect EMOM/AMRAP alternation.
        val lastWorkout = workoutRepository.getLastCompletedConditioningWorkoutByGym(gymId)
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
        formatOverride: WorkoutFormat? = null,
        excludedIds: Set<String> = emptySet()
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

        // N=1 cooldown: exclude movements from the single most recent completed
        // conditioning workout at this gym, merged with any caller-supplied
        // exclusions (e.g., Shuffle All passes the current preview's movements
        // so regeneration actually produces fresh picks). Replaces the prior
        // 0-14 day freshness gradient (user preference — binary recency, not
        // graded).
        val cooldownIds =
            workoutRepository.getExerciseIdsFromLastConditioningWorkoutAtGym(gymId) + excludedIds

        var candidateIds = pickMovementIds(format, cooldownIds)
        var attempts = 0
        while (fingerprint(candidateIds) in existingFingerprints && attempts < MAX_RETRIES) {
            candidateIds = pickMovementIds(format, cooldownIds)
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
    private fun pickMovementIds(format: WorkoutFormat, cooldownIds: Set<String>): List<String> {
        val byBucket = HomeGymMovementCatalog.byBucket()
        val cardio = byBucket[Bucket.CARDIO].orEmpty()
        val lower = byBucket[Bucket.LOWER_BODY].orEmpty()
        val pull = byBucket[Bucket.UPPER_PULL].orEmpty()
        val push = byBucket[Bucket.UPPER_PUSH].orEmpty()
        val core = byBucket[Bucket.CORE].orEmpty()
        val conditioning = byBucket[Bucket.CONDITIONING_BODYWEIGHT].orEmpty()

        // Apply N=1 cooldown with safe fallback. Every bucket pool passes
        // through this before selection.
        fun cooled(list: List<HomeGymMovementCatalog.Movement>) =
            list.filterNot { it.id in cooldownIds }.ifEmpty { list }

        val usedFamilies = mutableSetOf<String>()

        // Three-tier fallback: prefer (family-unique ∧ cooldown-fresh); fall
        // back to cooldown-fresh alone if the family filter empties the pool;
        // fall back to the raw bucket if cooldown empties it too. Priority
        // order: always-pick-something > cooldown > family dedup. Family is
        // the softest constraint because monthly fingerprint + MAX_RETRIES
        // already handle "don't repeat the identical workout" downstream.
        fun pickFrom(pool: List<HomeGymMovementCatalog.Movement>): HomeGymMovementCatalog.Movement {
            val familyAndCooled = pool
                .filterNot { it.id in cooldownIds }
                .filter { familyOf(it) == null || familyOf(it) !in usedFamilies }
            val chosen = when {
                familyAndCooled.isNotEmpty() -> familyAndCooled.random()
                else -> cooled(pool).random()
            }
            familyOf(chosen)?.let { usedFamilies.add(it) }
            return chosen
        }

        return when (format) {
            WorkoutFormat.EMOM -> {
                val legsPick = pickFrom(lower)
                val pullPool = if (legsPick.isHeavy()) pull.filterNot { it.isHeavy() } else pull
                val pullPick = pickFrom(pullPool.ifEmpty { pull })
                val heavyUsed = legsPick.isHeavy() || pullPick.isHeavy()
                val pushPool = if (heavyUsed) push.filterNot { it.isHeavy() } else push
                val pushPick = pickFrom(pushPool.ifEmpty { push })
                val closerPick = pickFrom(core + conditioning)
                val picks = mutableListOf(legsPick, pullPick, pushPick, closerPick)
                // Pass cooldown-filtered bucket pools to the post-processing
                // helpers so TRX rerolls cannot reintroduce an excluded movement.
                val trxBuckets = listOf(cooled(lower), cooled(pull), cooled(push), cooled(core + conditioning))
                capTrx(picks, trxBuckets)
                enforceTrxAdjacency(picks, trxBuckets)
                picks.map { it.id }
            }
            WorkoutFormat.AMRAP -> {
                val cardioPick = pickFrom(cardio)
                val strengthPick = pickFrom(lower + pull + push)
                val closerPick = pickFrom(core + conditioning)
                val picks = mutableListOf(cardioPick, strengthPick, closerPick)
                val trxBuckets = listOf(cooled(cardio), cooled(lower + pull + push), cooled(core + conditioning))
                capTrx(picks, trxBuckets)
                enforceTrxAdjacency(picks, trxBuckets)
                picks.map { it.id }
            }
            else -> error("Unsupported conditioning format: $format")
        }
    }

    /**
     * Identifies movement "family" for dedup within a single generated
     * session. Returns null if no pattern matches — unrecognized movements
     * are treated as their own unique family and never conflict.
     *
     * Scope intentionally narrow: push-up variants and burpee variants only.
     * Expanding to other roots (squat/row/twist) is easy to get wrong because
     * e.g. Goblet Squat and Wall Ball Squat are legitimately different
     * movements. Add new entries only when a real duplication is observed.
     */
    private fun familyOf(m: HomeGymMovementCatalog.Movement): String? {
        return when {
            "Push-Up" in m.name -> "pushup"
            "Burpee" in m.name -> "burpee"
            else -> null
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

    /**
     * Enforces that no two TRX exercises land in adjacent stations, including
     * the wraparound edge (last station → first station). Wraparound matters
     * in EMOM because stations loop continuously; in AMRAP because the block
     * cycles while the clock runs. After [capTrx] guarantees ≤2 TRX per
     * session, violations reduce to: "one pair of TRX picks at neighboring
     * positions mod N."
     *
     * Re-roll strategy: when position `i` and position `(i+1) mod N` are both
     * TRX, replace the latter with a non-TRX movement from its bucket,
     * excluding any movement already in [picks]. Pool-exhaustion is tolerated
     * — we accept the adjacency rather than fail generation.
     */
    private fun enforceTrxAdjacency(
        picks: MutableList<HomeGymMovementCatalog.Movement>,
        bucketPools: List<List<HomeGymMovementCatalog.Movement>>
    ) {
        val n = picks.size
        if (n < 2) return
        repeat(n) {
            val violation = (0 until n).firstOrNull { i ->
                val next = (i + 1) % n
                picks[i].isTrx() && picks[next].isTrx()
            } ?: return
            val replaceAt = (violation + 1) % n
            val usedIds = picks.map { it.id }.toSet()
            val pool = bucketPools[replaceAt]
                .filterNot { it.isTrx() || it.id in usedIds }
            if (pool.isEmpty()) return
            picks[replaceAt] = pool.random()
        }
    }

    private fun fingerprint(ids: List<String>): String = ids.sorted().joinToString(",")
}
