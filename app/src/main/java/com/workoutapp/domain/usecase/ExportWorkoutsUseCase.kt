package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.repository.WorkoutRepository
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class ExportWorkoutsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(): String {
        val workouts = workoutRepository.getAllCompletedWorkoutsWithExercises()
            .sortedBy { it.date }

        return buildString {
            appendLine("Date,Exercise,Set,Reps,Weight (lbs),Format,Rounds,Prescription")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (workout in workouts) {
                val date = dateFormat.format(workout.date)
                val format = workout.format.name
                val rounds = workout.completedRounds?.toString() ?: ""
                for (exercise in workout.exercises) {
                    if (exercise.prescription != null) {
                        appendLine("${csvEscape(date)},${csvEscape(exercise.exercise.name)},,,,${csvEscape(format)},${csvEscape(rounds)},${csvEscape(exercise.prescription)}")
                    } else {
                        exercise.sets.forEachIndexed { index, set ->
                            if (set.completed) {
                                appendLine("${csvEscape(date)},${csvEscape(exercise.exercise.name)},${index + 1},${set.reps},${set.weight.toInt()},${csvEscape(format)},,")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun csvEscape(s: String?): String {
        if (s == null) return ""
        return if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            "\"${s.replace("\"", "\"\"")}\""
        } else s
    }
}
