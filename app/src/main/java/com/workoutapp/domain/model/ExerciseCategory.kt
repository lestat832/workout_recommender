package com.workoutapp.domain.model

enum class ExerciseCategory {
    STRENGTH_PUSH,
    STRENGTH_PULL,
    STRENGTH_LEGS,
    CORE,
    CARDIO_CONDITIONING;

    companion object {
        fun deriveFrom(
            muscleGroups: List<MuscleGroup>,
            legacyCategory: WorkoutType
        ): ExerciseCategory = when {
            MuscleGroup.LEGS in muscleGroups -> STRENGTH_LEGS
            MuscleGroup.CORE in muscleGroups -> CORE
            legacyCategory == WorkoutType.PUSH -> STRENGTH_PUSH
            else -> STRENGTH_PULL
        }
    }
}
