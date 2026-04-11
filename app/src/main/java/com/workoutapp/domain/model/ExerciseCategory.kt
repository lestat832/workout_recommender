package com.workoutapp.domain.model

enum class ExerciseCategory {
    STRENGTH_PUSH,
    STRENGTH_PULL,
    STRENGTH_LEGS,
    CORE,
    CARDIO_CONDITIONING;

    companion object {
        /**
         * Name fragments that indicate a movement is cardio/plyometric rather than
         * strength, regardless of its muscle group tag. Anything matching this list
         * gets categorized as CARDIO_CONDITIONING so it doesn't leak into LMU
         * strength workouts (e.g., "Fast Skipping" was showing up on pull day
         * because it has LEGS in its muscle groups).
         */
        private val CARDIO_NAME_PATTERNS = listOf(
            "skipping",
            "jump rope",
            "jumping jack",
            "mountain climber",
            "high knee",
            "burpee",
            "sprint",
            "running",
            "skater"
        )

        fun deriveFrom(
            muscleGroups: List<MuscleGroup>,
            legacyCategory: WorkoutType,
            name: String = ""
        ): ExerciseCategory {
            if (isCardioPatternName(name)) return CARDIO_CONDITIONING
            return when {
                MuscleGroup.LEGS in muscleGroups -> STRENGTH_LEGS
                MuscleGroup.CORE in muscleGroups -> CORE
                legacyCategory == WorkoutType.PUSH -> STRENGTH_PUSH
                else -> STRENGTH_PULL
            }
        }

        fun isCardioPatternName(name: String): Boolean {
            val lower = name.lowercase()
            return CARDIO_NAME_PATTERNS.any { it in lower }
        }
    }
}
