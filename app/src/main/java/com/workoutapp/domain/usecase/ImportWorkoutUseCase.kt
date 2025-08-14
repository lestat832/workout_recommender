package com.workoutapp.domain.usecase

import com.workoutapp.data.utils.ExerciseMapper
import com.workoutapp.data.utils.ExerciseMapping
import com.workoutapp.data.utils.StrongCsvParser
import com.workoutapp.data.utils.StrongWorkoutData
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutStatus
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

data class ImportResult(
    val totalWorkouts: Int,
    val importedWorkouts: Int,
    val newExercises: Int,
    val mappedExercises: Int,
    val errors: List<String>
)

class ImportWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    
    suspend fun importFromCsvContent(csvContent: String): ImportResult {
        val errors = mutableListOf<String>()
        var newExercisesCount = 0
        var mappedExercisesCount = 0
        
        try {
            // Parse CSV content
            val strongWorkouts = StrongCsvParser.parseCsvContent(csvContent)
            
            if (strongWorkouts.isEmpty()) {
                return ImportResult(0, 0, 0, 0, listOf("No workouts found in CSV file"))
            }
            
            // Get all existing exercises
            val existingExercises = exerciseRepository.getAllExercises().firstOrNull() ?: emptyList()
            val exerciseMap = mutableMapOf<String, Exercise>()
            
            // Process all unique exercises from the import
            val uniqueExerciseNames = strongWorkouts
                .flatMap { it.exercises }
                .map { it.name }
                .distinct()
            
            for (strongExerciseName in uniqueExerciseNames) {
                val mapping = ExerciseMapper.getMappingForExercise(strongExerciseName)
                
                if (mapping.ourName != null) {
                    // Try to find existing exercise
                    val existingExercise = existingExercises.find { 
                        it.name.equals(mapping.ourName, ignoreCase = true) 
                    }
                    
                    if (existingExercise != null) {
                        exerciseMap[strongExerciseName] = existingExercise
                        mappedExercisesCount++
                    } else {
                        // Create new exercise based on mapping
                        val newExercise = createCustomExercise(strongExerciseName, mapping)
                        exerciseRepository.createCustomExercise(newExercise)
                        exerciseMap[strongExerciseName] = newExercise
                        newExercisesCount++
                    }
                } else {
                    // Check if custom exercise already exists
                    val existingCustom = exerciseRepository.getCustomExerciseByName(strongExerciseName)
                    
                    if (existingCustom != null) {
                        exerciseMap[strongExerciseName] = existingCustom
                        mappedExercisesCount++
                    } else {
                        // Create new custom exercise
                        val newExercise = createCustomExercise(strongExerciseName, mapping)
                        exerciseRepository.createCustomExercise(newExercise)
                        exerciseMap[strongExerciseName] = newExercise
                        newExercisesCount++
                    }
                }
                
                // Add to user's active exercises
                exerciseMap[strongExerciseName]?.let { exercise ->
                    exerciseRepository.updateUserExerciseStatus(exercise.id, true)
                }
            }
            
            // Convert Strong workouts to our format
            val workoutsToImport = mutableListOf<Workout>()
            val workoutExercisesToImport = mutableListOf<WorkoutExercise>()
            
            for (strongWorkout in strongWorkouts) {
                try {
                    val workoutId = UUID.randomUUID().toString()
                    
                    // Determine workout type based on exercises
                    val workoutType = ExerciseMapper.determineWorkoutType(
                        strongWorkout.exercises.map { it.name }
                    )
                    
                    // Create workout
                    val workout = Workout(
                        id = workoutId,
                        date = strongWorkout.date,
                        type = workoutType,
                        status = WorkoutStatus.COMPLETED,
                        exercises = emptyList() // Will be added separately
                    )
                    workoutsToImport.add(workout)
                    
                    // Create workout exercises
                    for (strongExercise in strongWorkout.exercises) {
                        val exercise = exerciseMap[strongExercise.name]
                        if (exercise == null) {
                            errors.add("Could not map exercise: ${strongExercise.name}")
                            continue
                        }
                        
                        // Convert sets
                        val sets = strongExercise.sets.map { strongSet ->
                            Set(
                                reps = strongSet.reps,
                                weight = strongSet.weight,
                                completed = true // All imported sets are marked as completed
                            )
                        }
                        
                        if (sets.isNotEmpty()) {
                            val workoutExercise = WorkoutExercise(
                                id = UUID.randomUUID().toString(),
                                workoutId = workoutId,
                                exercise = exercise,
                                sets = sets
                            )
                            workoutExercisesToImport.add(workoutExercise)
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Error importing workout #${strongWorkout.workoutNumber}: ${e.message}")
                }
            }
            
            // Batch import workouts
            if (workoutsToImport.isNotEmpty()) {
                for (workout in workoutsToImport) {
                    // Create the workout first
                    workoutRepository.createWorkout(workout)
                    
                    // Add each exercise to the workout
                    val exercises = workoutExercisesToImport.filter { it.workoutId == workout.id }
                    for (exercise in exercises) {
                        workoutRepository.addExerciseToWorkout(workout.id, exercise)
                    }
                }
            }
            
            return ImportResult(
                totalWorkouts = strongWorkouts.size,
                importedWorkouts = workoutsToImport.size,
                newExercises = newExercisesCount,
                mappedExercises = mappedExercisesCount,
                errors = errors
            )
            
        } catch (e: Exception) {
            return ImportResult(
                totalWorkouts = 0,
                importedWorkouts = 0,
                newExercises = 0,
                mappedExercises = 0,
                errors = listOf("Import failed: ${e.message}")
            )
        }
    }
    
    private fun createCustomExercise(name: String, mapping: ExerciseMapping): Exercise {
        return Exercise(
            id = UUID.randomUUID().toString(),
            name = name,
            muscleGroups = mapping.muscleGroups,
            equipment = mapping.equipment,
            category = mapping.category,
            imageUrl = null,
            instructions = emptyList(),
            isUserCreated = true
        )
    }
}