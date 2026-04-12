package com.workoutapp.domain.usecase

import android.content.Context
import com.workoutapp.R
import com.workoutapp.data.utils.ExerciseMapper
import com.workoutapp.data.utils.HevyCsvParser
import com.workoutapp.data.utils.HevyWorkoutData
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

class HevyHistorySeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileComputer: ProfileComputerUseCase
) {

    suspend fun seedFromBundledCsv(gymId: Long? = null): Int {
        val csvContent = context.resources.openRawResource(R.raw.hevy_export)
            .bufferedReader()
            .use { it.readText() }

        val hevyWorkouts = HevyCsvParser.parseCsvContent(csvContent)
        if (hevyWorkouts.isEmpty()) return 0

        val existingExercises = exerciseRepository.getAllExercises().firstOrNull() ?: emptyList()
        val exerciseCache = mutableMapOf<String, Exercise>()

        // Map all unique exercise names
        val uniqueNames = hevyWorkouts.flatMap { it.exercises }.map { it.name }.distinct()

        for (hevyName in uniqueNames) {
            val mapping = ExerciseMapper.getMappingForExercise(hevyName)

            val exercise = if (mapping.ourName != null) {
                existingExercises.find { it.name.equals(mapping.ourName, ignoreCase = true) }
                    ?: createAndInsertCustomExercise(hevyName, mapping)
            } else {
                exerciseRepository.getCustomExerciseByName(hevyName)
                    ?: createAndInsertCustomExercise(hevyName, mapping)
            }

            exerciseCache[hevyName] = exercise
            exerciseRepository.updateUserExerciseStatus(exercise.id, true)
        }

        // Insert all workouts
        var count = 0
        for (hevyWorkout in hevyWorkouts) {
            insertWorkout(hevyWorkout, exerciseCache, gymId)
            count++
        }

        // Recompute full profile from all history (atomic transaction)
        profileComputer.recomputeFullProfile()

        return count
    }

    private suspend fun createAndInsertCustomExercise(
        hevyName: String,
        mapping: com.workoutapp.data.utils.ExerciseMapping
    ): Exercise {
        val exerciseCategory = when {
            mapping.muscleGroups.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER, MuscleGroup.TRICEP) } &&
                mapping.category == WorkoutType.PUSH -> ExerciseCategory.STRENGTH_PUSH
            mapping.muscleGroups.contains(MuscleGroup.LEGS) -> ExerciseCategory.STRENGTH_LEGS
            else -> ExerciseCategory.STRENGTH_PULL
        }

        val exercise = Exercise(
            id = "hevy_${hevyName.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
            name = hevyName,
            muscleGroups = mapping.muscleGroups,
            equipment = mapping.equipment,
            category = mapping.category,
            exerciseCategory = exerciseCategory,
            isUserCreated = true
        )
        exerciseRepository.createCustomExercise(exercise)
        return exercise
    }

    private suspend fun insertWorkout(
        hevyWorkout: HevyWorkoutData,
        exerciseCache: Map<String, Exercise>,
        gymId: Long?
    ) {
        val exerciseNames = hevyWorkout.exercises.map { it.name }
        val workoutType = ExerciseMapper.determineWorkoutType(exerciseNames)

        val workoutId = UUID.randomUUID().toString()
        val workout = Workout(
            id = workoutId,
            date = hevyWorkout.startTime,
            type = workoutType,
            status = WorkoutStatus.COMPLETED,
            gymId = gymId,
            format = WorkoutFormat.STRENGTH,
            durationMinutes = hevyWorkout.durationMinutes
        )
        workoutRepository.createWorkout(workout)

        for (hevyExercise in hevyWorkout.exercises) {
            val exercise = exerciseCache[hevyExercise.name] ?: continue

            val sets = hevyExercise.sets.map { hevySet ->
                Set(
                    reps = hevySet.reps,
                    weight = hevySet.weightLbs,
                    completed = true
                )
            }

            val workoutExercise = WorkoutExercise(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exercise = exercise,
                sets = sets
            )
            workoutRepository.addExerciseToWorkout(workoutId, workoutExercise)
        }
    }
}
