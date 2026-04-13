package com.workoutapp.domain.usecase

import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Computes selection weights for exercises based on days since last performed.
 * Used by both LMU strength and Home Gym conditioning generators.
 *
 * Weight table (from 10x trainer analysis):
 *   0-2 days:   0.05 (nearly excluded — still recovering)
 *   3-4 days:   0.30 (short window, deprioritize)
 *   5-7 days:   1.00 (optimal rotation — full priority)
 *   8-14 days:  1.10 (slightly overdue, mild boost)
 *   14+ days:   1.00 (normal weight)
 *   never done: 1.00 (eligible, no boost)
 */
object ExerciseFreshness {

    fun weight(exerciseId: String, lastPerformedDates: Map<String, Date>, now: Date = Date()): Double {
        val lastDate = lastPerformedDates[exerciseId] ?: return 1.0 // never done
        val daysSince = TimeUnit.MILLISECONDS.toDays(now.time - lastDate.time).toInt()
        return when {
            daysSince <= 2 -> 0.05
            daysSince <= 4 -> 0.30
            daysSince <= 7 -> 1.00
            daysSince <= 14 -> 1.10
            else -> 1.00
        }
    }

    fun daysSinceLastPerformed(exerciseId: String, lastPerformedDates: Map<String, Date>, now: Date = Date()): Int? {
        val lastDate = lastPerformedDates[exerciseId] ?: return null
        return TimeUnit.MILLISECONDS.toDays(now.time - lastDate.time).toInt()
    }

    /**
     * Weighted random selection from a list of candidates.
     * Each candidate's probability is proportional to its weight.
     * Returns null if candidates is empty or all weights are zero.
     */
    fun <T> weightedRandom(
        candidates: List<T>,
        weightFn: (T) -> Double,
        random: Random = Random.Default
    ): T? {
        if (candidates.isEmpty()) return null
        val weights = candidates.map { weightFn(it).coerceAtLeast(0.0) }
        val totalWeight = weights.sum()
        if (totalWeight <= 0.0) return null

        var roll = random.nextDouble() * totalWeight
        for (i in candidates.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return candidates[i]
        }
        return candidates.last() // floating point safety
    }
}
