package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.ExerciseProfile
import com.workoutapp.domain.model.GlobalProfile
import com.workoutapp.domain.model.LoadingPattern
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.MuscleGroupProfile
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.TrainingProfileRepository
import com.workoutapp.domain.repository.WorkoutRepository
import java.util.Calendar
import javax.inject.Inject

class ProfileComputerUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: TrainingProfileRepository
) {

    suspend fun recomputeFullProfile() {
        val allWorkouts = workoutRepository.getAllCompletedWorkoutsWithExercises()
        if (allWorkouts.isEmpty()) return

        val now = System.currentTimeMillis()

        val exerciseProfiles = buildExerciseProfiles(allWorkouts)
        val muscleGroupProfiles = buildMuscleGroupProfiles(allWorkouts, now)
        val globalProfile = buildGlobalProfile(allWorkouts, now)

        // Atomic: delete all old profiles and write new ones in one transaction
        profileRepository.deleteAndRebuildProfiles(exerciseProfiles, muscleGroupProfiles, globalProfile)
    }

    suspend fun updateAfterWorkout(workoutId: String) {
        val workout = workoutRepository.getWorkoutById(workoutId) ?: return
        if (workout.status != WorkoutStatus.COMPLETED) return

        val allWorkouts = workoutRepository.getAllCompletedWorkoutsWithExercises()
        val now = System.currentTimeMillis()

        // Update exercise profiles for exercises in this workout
        val touchedExerciseIds = workout.exercises.map { it.exercise.id }
        val updatedExerciseProfiles = buildExerciseProfiles(allWorkouts)
            .filter { it.exerciseId in touchedExerciseIds }
        profileRepository.saveExerciseProfiles(updatedExerciseProfiles)

        // Update muscle group profiles for groups in this workout
        val touchedGroups = workout.exercises
            .flatMap { it.exercise.muscleGroups }
            .toSet()
        val allMuscleProfiles = buildMuscleGroupProfiles(allWorkouts, now)
        val updatedMuscleProfiles = allMuscleProfiles.filter { it.muscleGroup in touchedGroups }
        profileRepository.saveMuscleGroupProfiles(updatedMuscleProfiles)

        // Update global profile
        profileRepository.saveGlobalProfile(buildGlobalProfile(allWorkouts, now))
    }

    // ── Exercise profiles ───────────────────────────────────────────────

    private fun buildExerciseProfiles(
        allWorkouts: List<Workout>
    ): List<ExerciseProfile> {
        val exerciseSessions = mutableMapOf<String, MutableList<ExerciseSession>>()

        for (workout in allWorkouts) {
            for (wex in workout.exercises) {
                exerciseSessions
                    .getOrPut(wex.exercise.id) { mutableListOf() }
                    .add(ExerciseSession(workout, wex.sets))
            }
        }

        return exerciseSessions.map { (exerciseId, sessions) ->
            // Sessions are already sorted newest-first (from allWorkouts order)
            val strengthSessions = sessions.filter { it.workout.format == WorkoutFormat.STRENGTH }
            val allSessionCount = sessions.size
            val strengthCount = strengthSessions.size

            // Loading pattern from last 5 STRENGTH sessions
            val loadingPattern = detectLoadingPattern(strengthSessions.take(5))

            // Working weight from last 3 STRENGTH sessions (recency-weighted)
            val workingWeight = computeWorkingWeight(strengthSessions.take(3))

            // Warmup weight and ramp steps
            val (warmupWeight, rampSteps) = computeWarmupInfo(strengthSessions.take(3))

            // Detect if bodyweight-only
            val bodyweightOnly = strengthSessions.take(3).all { session ->
                session.sets.all { it.weight == 0f }
            } && strengthSessions.isNotEmpty()

            // Preferred set/rep scheme from last 3 STRENGTH sessions
            val (prefSets, prefRepsMin, prefRepsMax) = computePreferredScheme(strengthSessions.take(3))

            // Plateau detection
            val (plateauFlag, plateauCount) = detectPlateau(strengthSessions)

            // Estimated 1RM (Epley)
            val e1rm = computeEstimated1RM(strengthSessions.take(5))

            // Progression rate
            val progressionRate = computeProgressionRate(strengthSessions)

            // Last progression date
            val lastProgDate = detectLastProgressionDate(strengthSessions)

            ExerciseProfile(
                exerciseId = exerciseId,
                loadingPattern = loadingPattern,
                loadingPatternConfidence = strengthCount.coerceAtMost(10),
                currentWorkingWeight = workingWeight,
                warmupWeight = warmupWeight,
                rampSteps = rampSteps,
                bodyweightOnly = bodyweightOnly,
                preferredWorkingSets = prefSets,
                preferredRepsMin = prefRepsMin,
                preferredRepsMax = prefRepsMax,
                lastProgressionDate = lastProgDate,
                progressionRateLbPerMonth = progressionRate,
                estimatedOneRepMax = e1rm,
                plateauFlag = plateauFlag,
                plateauSessionCount = plateauCount,
                sessionCount = allSessionCount,
                strengthSessionCount = strengthCount,
                lastPerformedDate = sessions.firstOrNull()?.workout?.date?.time
            )
        }
    }

    private data class ExerciseSession(
        val workout: Workout,
        val sets: List<com.workoutapp.domain.model.Set>
    )

    private fun detectLoadingPattern(sessions: List<ExerciseSession>): LoadingPattern {
        if (sessions.size < 5) return LoadingPattern.UNKNOWN

        val patterns = sessions.map { session ->
            val weights = session.sets
                .filter { it.weight > 0f && it.completed }
                .map { it.weight }

            if (weights.size < 2) return@map LoadingPattern.UNKNOWN

            val maxW = weights.max()
            val minW = weights.min()
            val range = maxW - minW

            when {
                // Flat: all weights within 5% of max
                range <= maxW * 0.05f -> LoadingPattern.FLAT

                // Ramp and hold: weights go up, last 2+ sets are at same weight
                weights.last() >= weights.first() &&
                    weights.takeLast(2).distinct().size == 1 &&
                    weights.first() < weights.last() -> LoadingPattern.RAMP_AND_HOLD

                // Ascending ramp: monotonically non-decreasing, no hold
                weights.zipWithNext().all { (a, b) -> b >= a } &&
                    weights.takeLast(2).distinct().size > 1 -> LoadingPattern.ASCENDING_RAMP

                // Ramp and hold also catches: ramp up, do multiple at top, then back down (pyramid)
                weights.indexOf(maxW) < weights.lastIndex &&
                    weights.count { it == maxW } >= 2 -> LoadingPattern.RAMP_AND_HOLD

                else -> LoadingPattern.UNKNOWN
            }
        }

        // Majority vote
        val counts = patterns.groupingBy { it }.eachCount()
        val majority = counts.maxByOrNull { it.value }
        return if (majority != null && majority.value >= 3 && majority.key != LoadingPattern.UNKNOWN) {
            majority.key
        } else {
            LoadingPattern.UNKNOWN
        }
    }

    private fun computeWorkingWeight(sessions: List<ExerciseSession>): Float? {
        val weights = sessions.mapNotNull { session ->
            session.sets
                .filter { it.weight > 0f && it.completed && it.reps >= 5 }
                .maxByOrNull { it.weight }
                ?.weight
        }

        return when (weights.size) {
            0 -> null
            1 -> weights[0]
            2 -> weights[0] * 0.6f + weights[1] * 0.4f
            else -> weights[0] * 0.5f + weights[1] * 0.3f + weights[2] * 0.2f
        }
    }

    private fun computeWarmupInfo(sessions: List<ExerciseSession>): Pair<Float?, Int> {
        val infos = sessions.mapNotNull { session ->
            val weights = session.sets
                .filter { it.weight > 0f && it.completed }
                .map { it.weight }

            if (weights.size < 2) return@mapNotNull null

            val maxW = weights.max()
            val warmupCutoff = maxW * 0.7f

            val rampSets = weights.takeWhile { it < warmupCutoff * 1.1f && it < maxW }
            if (rampSets.isEmpty()) null
            else Pair(rampSets.first(), rampSets.size)
        }

        if (infos.isEmpty()) return Pair(null, 0)

        val avgWarmup = infos.map { it.first }.average().toFloat()
        val avgRamp = infos.map { it.second }.average().toInt().coerceAtLeast(0)
        return Pair(avgWarmup, avgRamp)
    }

    private fun computePreferredScheme(sessions: List<ExerciseSession>): Triple<Int, Int, Int> {
        if (sessions.isEmpty()) return Triple(3, 8, 10)

        val workingSetCounts = sessions.map { session ->
            val weights = session.sets.filter { it.weight > 0f && it.completed }.map { it.weight }
            if (weights.isEmpty()) {
                // Bodyweight: all sets are "working"
                session.sets.count { it.completed }
            } else {
                val maxW = weights.max()
                session.sets.count { it.completed && it.weight >= maxW * 0.9f }
            }
        }

        val repRanges = sessions.flatMap { session ->
            val weights = session.sets.filter { it.weight > 0f && it.completed }.map { it.weight }
            if (weights.isEmpty()) {
                session.sets.filter { it.completed }.map { it.reps }
            } else {
                val maxW = weights.max()
                session.sets.filter { it.completed && it.weight >= maxW * 0.9f }.map { it.reps }
            }
        }

        val prefSets = workingSetCounts.average().toInt().coerceIn(2, 6)
        val prefMin = if (repRanges.isNotEmpty()) {
            repRanges.min().coerceIn(1, 20)
        } else 8
        val prefMax = if (repRanges.isNotEmpty()) {
            repRanges.max().coerceIn(prefMin, 20)
        } else 10

        return Triple(prefSets, prefMin, prefMax)
    }

    private fun detectPlateau(sessions: List<ExerciseSession>): Pair<Boolean, Int> {
        val workingWeights = sessions
            .filter { it.workout.format == WorkoutFormat.STRENGTH }
            .take(6)
            .mapNotNull { session ->
                session.sets
                    .filter { it.weight > 0f && it.completed && it.reps >= 5 }
                    .maxByOrNull { it.weight }
                    ?.weight
            }

        if (workingWeights.size < 2) return Pair(false, 0)

        val currentWeight = workingWeights.first()
        val sameWeightCount = workingWeights.takeWhile {
            kotlin.math.abs(it - currentWeight) <= 2.5f
        }.size

        return Pair(sameWeightCount >= 4, sameWeightCount)
    }

    private fun computeEstimated1RM(sessions: List<ExerciseSession>): Float? {
        val bestSet = sessions.flatMap { it.sets }
            .filter { it.weight > 0f && it.completed && it.reps in 1..12 }
            .maxByOrNull { it.weight * (1f + it.reps / 30f) }
            ?: return null

        return bestSet.weight * (1f + bestSet.reps / 30f)
    }

    private fun computeProgressionRate(sessions: List<ExerciseSession>): Float? {
        val strengthSessions = sessions.filter { it.workout.format == WorkoutFormat.STRENGTH }
        if (strengthSessions.size < 2) return null

        val newest = strengthSessions.first()
        val oldest = strengthSessions.last()

        val newestWeight = newest.sets
            .filter { it.weight > 0f && it.completed && it.reps >= 5 }
            .maxByOrNull { it.weight }?.weight ?: return null
        val oldestWeight = oldest.sets
            .filter { it.weight > 0f && it.completed && it.reps >= 5 }
            .maxByOrNull { it.weight }?.weight ?: return null

        val monthsElapsed = (newest.workout.date.time - oldest.workout.date.time) /
            (1000.0 * 60 * 60 * 24 * 30)

        return if (monthsElapsed > 0.5) {
            ((newestWeight - oldestWeight) / monthsElapsed).toFloat()
        } else null
    }

    private fun detectLastProgressionDate(sessions: List<ExerciseSession>): Long? {
        val strengthSessions = sessions.filter { it.workout.format == WorkoutFormat.STRENGTH }

        for (i in 0 until strengthSessions.size - 1) {
            val currentWeight = strengthSessions[i].sets
                .filter { it.weight > 0f && it.completed && it.reps >= 5 }
                .maxByOrNull { it.weight }?.weight ?: continue
            val previousWeight = strengthSessions[i + 1].sets
                .filter { it.weight > 0f && it.completed && it.reps >= 5 }
                .maxByOrNull { it.weight }?.weight ?: continue

            if (currentWeight > previousWeight + 2.5f) {
                return strengthSessions[i].workout.date.time
            }
        }
        return null
    }

    // ── Muscle group profiles ───────────────────────────────────────────

    private fun buildMuscleGroupProfiles(
        allWorkouts: List<Workout>,
        now: Long
    ): List<MuscleGroupProfile> {
        val fourWeeksAgo = now - 28L * 24 * 60 * 60 * 1000

        return MuscleGroup.entries.map { group ->
            // Find all sessions that hit this group (count ALL muscle groups per exercise)
            val sessionsHittingGroup = allWorkouts.filter { workout ->
                workout.exercises.any { wex ->
                    group in wex.exercise.muscleGroups
                }
            }

            // Sets in last 4 weeks hitting this group
            val recentSets = allWorkouts
                .filter { it.date.time >= fourWeeksAgo }
                .flatMap { w -> w.exercises.filter { group in it.exercise.muscleGroups } }
                .sumOf { it.sets.size }
            val weeklyVolume = recentSets / 4f

            // Coverage: % of last 30 STRENGTH sessions
            val last30Strength = allWorkouts
                .filter { it.format == WorkoutFormat.STRENGTH }
                .take(30)
            val coveragePct = if (last30Strength.isNotEmpty()) {
                last30Strength.count { w ->
                    w.exercises.any { group in it.exercise.muscleGroups }
                } / last30Strength.size.toFloat()
            } else 0f

            // Preferred exercises: top 5 by frequency in last 30 sessions
            val exerciseFreq = allWorkouts.take(30)
                .flatMap { w -> w.exercises.filter { group in it.exercise.muscleGroups } }
                .groupingBy { it.exercise.id }
                .eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { it.key }

            // Avoided: tried ≤2 times total
            val allExerciseFreq = sessionsHittingGroup
                .flatMap { w -> w.exercises.filter { group in it.exercise.muscleGroups } }
                .groupingBy { it.exercise.id }
                .eachCount()
            val avoided = allExerciseFreq
                .filter { it.value <= 2 }
                .keys.toList()

            // Volume tolerance: detect rep falloff in STRENGTH sessions
            val strengthHitting = sessionsHittingGroup.filter { it.format == WorkoutFormat.STRENGTH }
            val tolerance = detectVolumeTolerance(strengthHitting, group)

            MuscleGroupProfile(
                muscleGroup = group,
                weeklySetVolume = weeklyVolume,
                volumeTolerance = tolerance,
                coveragePercentage = coveragePct,
                preferredExerciseIds = exerciseFreq,
                avoidedExerciseIds = avoided,
                lastTrainedDate = sessionsHittingGroup.firstOrNull()?.date?.time,
                sessionCount = sessionsHittingGroup.size
            )
        }
    }

    private fun detectVolumeTolerance(
        sessions: List<Workout>,
        group: MuscleGroup
    ): Int? {
        // Look at sessions where this group had 4+ sets and check for rep falloff
        val candidates = sessions.take(10).mapNotNull { w ->
            val setsForGroup = w.exercises
                .filter { group in it.exercise.muscleGroups }
                .flatMap { it.sets }
                .filter { it.completed && it.reps > 0 }

            if (setsForGroup.size >= 4) setsForGroup else null
        }

        if (candidates.size < 3) return null

        // Average reps in first 3 sets vs sets 4+
        val avgEarly = candidates.flatMap { it.take(3) }.map { it.reps }.average()
        val avgLate = candidates.flatMap { it.drop(3) }.map { it.reps }.average()

        return if (avgLate < avgEarly * 0.8) 3 else null
    }

    // ── Global profile ──────────────────────────────────────────────────

    private fun buildGlobalProfile(
        allWorkouts: List<Workout>,
        now: Long
    ): GlobalProfile {
        val fourWeeksAgo = now - 28L * 24 * 60 * 60 * 1000
        val eightWeeksAgo = now - 56L * 24 * 60 * 60 * 1000

        val strengthWorkouts = allWorkouts.filter { it.format == WorkoutFormat.STRENGTH }
        val conditioningWorkouts = allWorkouts.filter { it.format != WorkoutFormat.STRENGTH }

        // Frequency: sessions in last 4 weeks / 4
        val recentAll = allWorkouts.filter { it.date.time >= fourWeeksAgo }
        val frequency = recentAll.size / 4f

        // Push/pull ratio: STRENGTH only, last 8 weeks
        val recentStrength = strengthWorkouts.filter { it.date.time >= eightWeeksAgo }
        val pushCount = recentStrength.count { it.type == WorkoutType.PUSH }
        val pullCount = recentStrength.count { it.type == WorkoutType.PULL }
        val ratio = if (pullCount > 0) pushCount.toFloat() / pullCount else 1f

        // Avg exercises per session (STRENGTH only)
        val avgExPerSession = if (strengthWorkouts.isNotEmpty()) {
            strengthWorkouts.take(20).map { it.exercises.size }.average().toFloat()
        } else 0f

        // Avg sets per exercise (STRENGTH only)
        val avgSetsPerEx = if (strengthWorkouts.isNotEmpty()) {
            strengthWorkouts.take(20)
                .flatMap { it.exercises }
                .map { it.sets.size }
                .average().toFloat()
        } else 0f

        // Avg session duration (ALL formats, from those that have duration)
        val durationsMin = allWorkouts.take(30).mapNotNull { it.durationMinutes }
        val avgDuration = if (durationsMin.isNotEmpty()) durationsMin.average().toFloat() else 0f

        // Preferred training days
        val dayCounts = allWorkouts.take(60)
            .groupingBy { dayOfWeek(it.date.time) }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }

        // Current streak
        val streak = computeStreak(allWorkouts, now)

        return GlobalProfile(
            avgSessionDurationMin = avgDuration,
            avgExercisesPerSession = avgExPerSession,
            avgSetsPerExercise = avgSetsPerEx,
            trainingFrequencyPerWeek = frequency,
            pushPullRatio = ratio,
            preferredTrainingDays = dayCounts,
            totalCompletedSessions = allWorkouts.size,
            totalStrengthSessions = strengthWorkouts.size,
            totalConditioningSessions = conditioningWorkouts.size,
            currentStreakWeeks = streak,
            lastWorkoutDate = allWorkouts.firstOrNull()?.date?.time,
            lastFullRecompute = now
        )
    }

    private fun dayOfWeek(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }

    private fun computeStreak(workouts: List<Workout>, now: Long): Int {
        if (workouts.isEmpty()) return 0

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Roll back to Monday of current week
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        val workoutDates = workouts.map { it.date.time }.toSet()

        for (weeksBack in 0..52) {
            val weekStart = cal.timeInMillis
            val weekEnd = weekStart + 7L * 24 * 60 * 60 * 1000

            val hasWorkoutThisWeek = workoutDates.any { it in weekStart until weekEnd }
            if (hasWorkoutThisWeek) {
                streak++
            } else {
                break
            }
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }

        return streak
    }
}
