package com.workoutapp.data.mapper

import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.data.remote.ExerciseDbJson
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

/**
 * Mapper functions to convert ExerciseDB API responses to domain models
 */
object ExerciseMapper {

    private const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    /**
     * Converts ExerciseDB JSON to domain Exercise model
     */
    fun ExerciseDbJson.toDomain(): Exercise {
        return Exercise(
            id = this.id,
            name = this.name,
            muscleGroups = mapMuscleGroups(this.primaryMuscles, this.secondaryMuscles),
            equipment = mapEquipment(this.equipment),
            category = mapWorkoutType(this.force, this.primaryMuscles),
            imageUrl = mapImageUrl(this.images),
            instructions = this.instructions
        )
    }

    /**
     * Converts ExerciseDB JSON to database entity
     */
    fun ExerciseDbJson.toEntity(): ExerciseEntity {
        return ExerciseEntity(
            id = this.id,
            name = this.name,
            muscleGroups = mapMuscleGroups(this.primaryMuscles, this.secondaryMuscles),
            equipment = mapEquipment(this.equipment),
            category = mapWorkoutType(this.force, this.primaryMuscles),
            imageUrl = mapImageUrl(this.images),
            instructions = this.instructions,
            isUserCreated = false,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Maps ExerciseDB muscle names to MuscleGroup enum
     * Combines both primary and secondary muscles
     */
    private fun mapMuscleGroups(
        primaryMuscles: List<String>,
        secondaryMuscles: List<String>
    ): List<MuscleGroup> {
        val allMuscles = (primaryMuscles + secondaryMuscles).distinct()
        val muscleGroups = mutableSetOf<MuscleGroup>()

        allMuscles.forEach { muscle ->
            when (muscle.lowercase()) {
                "chest" -> muscleGroups.add(MuscleGroup.CHEST)
                "shoulders" -> muscleGroups.add(MuscleGroup.SHOULDER)
                "triceps" -> muscleGroups.add(MuscleGroup.TRICEP)
                "biceps" -> muscleGroups.add(MuscleGroup.BICEP)
                "lats", "middle back", "lower back", "traps" ->
                    muscleGroups.add(MuscleGroup.BACK)
                "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors" ->
                    muscleGroups.add(MuscleGroup.LEGS)
                "abdominals" -> muscleGroups.add(MuscleGroup.CORE)
                // forearms, neck are ignored (minor muscles)
            }
        }

        return muscleGroups.toList()
    }

    /**
     * Maps ExerciseDB equipment names to user-friendly strings
     */
    private fun mapEquipment(equipment: String?): String {
        return when (equipment?.lowercase()) {
            null -> "Bodyweight"
            "body only" -> "Bodyweight"
            "barbell" -> "Barbell"
            "dumbbell" -> "Dumbbell"
            "cable" -> "Cable"
            "machine" -> "Machine"
            "kettlebells" -> "Kettlebell"
            "bands" -> "Resistance Band"
            "exercise ball" -> "Exercise Ball"
            "e-z curl bar" -> "EZ Bar"
            "foam roll" -> "Foam Roller"
            "medicine ball" -> "Medicine Ball"
            "other" -> "Other"
            else -> equipment.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Determines if exercise is PUSH or PULL
     * Uses force field first, falls back to muscle-based classification
     */
    private fun mapWorkoutType(
        force: String?,
        primaryMuscles: List<String>
    ): WorkoutType {
        // First: Use force field if available (86% coverage)
        when (force?.lowercase()) {
            "push" -> return WorkoutType.PUSH
            "pull" -> return WorkoutType.PULL
            "static" -> return WorkoutType.PULL // Treat static as PULL (core/stretching)
        }

        // Fallback: Classify by primary muscle
        val primaryMuscle = primaryMuscles.firstOrNull()?.lowercase()

        return when (primaryMuscle) {
            // PUSH muscles: pressing/extending movements
            "chest", "triceps", "shoulders", "quadriceps", "calves", "glutes" ->
                WorkoutType.PUSH

            // PULL muscles: pulling/flexing movements
            "biceps", "lats", "middle back", "lower back", "traps",
            "hamstrings", "abdominals", "forearms" ->
                WorkoutType.PULL

            // Default to PULL for unknown
            else -> WorkoutType.PULL
        }
    }

    /**
     * Constructs full image URL from relative path
     */
    private fun mapImageUrl(images: List<String>?): String? {
        return images?.firstOrNull()?.let { IMAGE_BASE_URL + it }
    }
}
