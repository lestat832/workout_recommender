package com.workoutapp.domain.usecase

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Computes 4-week block periodization state for LMU strength workouts.
 *
 * Block cycle:
 *   Week 1: Volume phase · Baseline
 *   Week 2: Volume phase · Add reps (targetRepsMax + 1)
 *   Week 3: Intensity phase · Add weight (+5 lb if cleared reps last session)
 *   Week 4: Deload · Recovery (weight × 0.80, sets - 1)
 *
 * Deload triggers:
 *   - SCHEDULED: weekInBlock == 4 via calendar math
 *   - PLATEAU: 2+ exercises in upcoming workout have plateauFlag == true
 *   - EXTENDED_ABSENCE: last workout was 14+ days ago → reset to week 1 (labeled as deload)
 */
object BlockPeriodization {

    const val BLOCK_WEEKS = 4
    const val EXTENDED_ABSENCE_DAYS = 14
    const val PLATEAU_TRIGGER_COUNT = 2
    const val DELOAD_WEIGHT_PCT = 0.80f

    enum class DeloadReason { SCHEDULED, PLATEAU, EXTENDED_ABSENCE }

    data class State(
        val blockNumber: Int,
        val weekInBlock: Int,
        val isDeloadWeek: Boolean,
        val deloadReason: DeloadReason?,
        val phaseLabel: String
    )

    fun computeState(
        blockStartDate: Date,
        blockNumber: Int,
        lastWorkoutDate: Date?,
        plateauedExerciseCount: Int,
        now: Date = Date()
    ): State {
        if (lastWorkoutDate != null) {
            val daysSinceLast = daysBetween(lastWorkoutDate, now)
            if (daysSinceLast >= EXTENDED_ABSENCE_DAYS) {
                return State(
                    blockNumber = blockNumber,
                    weekInBlock = 1,
                    isDeloadWeek = true,
                    deloadReason = DeloadReason.EXTENDED_ABSENCE,
                    phaseLabel = "Week 1 · Returning to training"
                )
            }
        }

        val daysSinceStart = daysBetween(blockStartDate, now).coerceAtLeast(0)
        val weeksSinceStart = (daysSinceStart / 7).toInt()
        val rawWeek = (weeksSinceStart % BLOCK_WEEKS) + 1

        if (rawWeek == BLOCK_WEEKS) {
            return State(
                blockNumber = blockNumber,
                weekInBlock = 4,
                isDeloadWeek = true,
                deloadReason = DeloadReason.SCHEDULED,
                phaseLabel = "Deload week · Recovery"
            )
        }

        if (plateauedExerciseCount >= PLATEAU_TRIGGER_COUNT) {
            return State(
                blockNumber = blockNumber,
                weekInBlock = 4,
                isDeloadWeek = true,
                deloadReason = DeloadReason.PLATEAU,
                phaseLabel = "Deload week · Plateau recovery"
            )
        }

        val label = when (rawWeek) {
            1 -> "Week 1 of 4 · Volume phase · Baseline"
            2 -> "Week 2 of 4 · Volume phase · Add reps"
            3 -> "Week 3 of 4 · Intensity phase · Add weight"
            else -> "Week $rawWeek of 4"
        }
        return State(
            blockNumber = blockNumber,
            weekInBlock = rawWeek,
            isDeloadWeek = false,
            deloadReason = null,
            phaseLabel = label
        )
    }

    fun nextMondayAfter(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val advance = if (daysFromMonday == 0) 7 else 7 - daysFromMonday
        cal.add(Calendar.DAY_OF_YEAR, advance)
        return cal.time
    }

    private fun daysBetween(start: Date, end: Date): Long {
        val diffMillis = end.time - start.time
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }
}
