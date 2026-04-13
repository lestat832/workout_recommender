package com.workoutapp.data.database

import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

/**
 * Curated leg-movement catalog for LMU Gym strength workouts. When the
 * strength generator picks a LEGS exercise, it restricts the pool to ids
 * in [allowedIds] (with a soft fallback to the full CDN pool if none are
 * equipment-compatible).
 *
 * Two ids are reused from [HomeGymMovementCatalog] — goblet squat and the
 * pair DB squat — because they describe the same movements the user listed
 * as "goblet squat" and "dumbbell squat" for LMU. Those rows are already
 * seeded by [HomeGymCatalogSeeder]; this catalog just whitelists them.
 *
 * The remaining 9 movements are seeded by [buildNewExercises] via
 * InitializeExercisesUseCase on first launch.
 */
object LmuLegCatalog {

    data class Entry(
        val id: String,
        val name: String,
        val equipment: String,
        val instructions: List<String>,
        val seed: Boolean // false for ids reused from another catalog
    )

    val entries: List<Entry> = listOf(
        Entry(
            id = "custom_lmu_walking_lunges_db",
            name = "Walking Lunges (DB)",
            equipment = "Dumbbell",
            instructions = listOf(
                "Hold a dumbbell in each hand at your sides.",
                "Step forward into a lunge, dropping the back knee toward the floor.",
                "Drive through the front heel to bring the back leg through into the next lunge.",
                "Walk forward for reps or distance."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_hex_squat",
            name = "Hex Squat",
            equipment = "Hex Bar",
            instructions = listOf(
                "Stand inside the hex bar with feet shoulder-width apart.",
                "Hinge and squat down to grip the handles at your sides.",
                "Drive through the heels, keeping the chest up and back flat.",
                "Lock out at the top, then lower under control."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_barbell_front_squat",
            name = "Barbell Front Squat",
            equipment = "Barbell",
            instructions = listOf(
                "Rack the barbell on the front of your shoulders, elbows high.",
                "Brace the core and squat down with the torso upright.",
                "Drive through the heels to stand, keeping the elbows up throughout.",
                "Control the descent on every rep."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_barbell_back_squat",
            name = "Barbell Back Squat",
            equipment = "Barbell",
            instructions = listOf(
                "Rack the barbell across the upper back.",
                "Brace the core, unrack, and step back into position.",
                "Squat down with hips back and chest up, knees tracking over toes.",
                "Drive through the heels to stand."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_lying_hamstring_curl",
            name = "Lying Hamstring Curl",
            equipment = "Machine",
            instructions = listOf(
                "Lie face down on the hamstring curl machine with the pad behind the ankles.",
                "Curl the heels toward the glutes by contracting the hamstrings.",
                "Squeeze at the top of the rep.",
                "Lower under control to the starting position."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_bulgarian_split_squat",
            name = "Bulgarian Split Squat",
            equipment = "Dumbbell",
            instructions = listOf(
                "Hold a dumbbell in each hand, stand about 2 feet from a bench.",
                "Place the top of one foot behind you on the bench.",
                "Lower into a lunge until the back knee almost touches the floor.",
                "Drive through the front heel to stand. Complete reps, then switch legs."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_dl_clean_uprow_superset",
            name = "Deadlift → Clean → Upright Row",
            equipment = "Barbell",
            instructions = listOf(
                "Superset: perform each movement for the set reps before moving on.",
                "1) Deadlift: hinge at the hips, grip the bar, drive through the heels to lockout.",
                "2) Power Clean: from the floor, pull explosively and catch the bar in the front rack.",
                "3) Upright Row: from standing, pull the bar to chest height with elbows leading.",
                "Rest only between full supersets, not between movements."
            ),
            seed = true
        ),
        Entry(
            id = "custom_lmu_power_clean",
            name = "Power Clean",
            equipment = "Barbell",
            instructions = listOf(
                "Set up with the barbell over mid-foot, shoulders slightly ahead of the bar.",
                "Pull the bar explosively by extending hips, knees, and ankles.",
                "Shrug and pull the body under the bar, catching it in the front rack.",
                "Stand fully, then lower under control."
            ),
            seed = true
        ),
        Entry(
            // Reused from HomeGymMovementCatalog — already seeded by HomeGymCatalogSeeder.
            id = "custom_heavy_db_goblet_squat",
            name = "Goblet Squat",
            equipment = "Dumbbell",
            instructions = emptyList(),
            seed = false
        ),
        Entry(
            id = "custom_lmu_squat_curl_db",
            name = "Squat Curl (DB)",
            equipment = "Dumbbell",
            instructions = listOf(
                "Hold a dumbbell in each hand at your sides.",
                "Squat down with control, keeping chest up.",
                "As you drive up from the squat, curl both dumbbells to the shoulders.",
                "Lower the dumbbells as you descend into the next squat."
            ),
            seed = true
        ),
        Entry(
            // Reused from HomeGymMovementCatalog — already seeded by HomeGymCatalogSeeder.
            id = "custom_pair_db_squat",
            name = "Dumbbell Squat",
            equipment = "Dumbbell",
            instructions = emptyList(),
            seed = false
        )
    )

    val allowedIds: Set<String> = entries.map { it.id }.toSet()

    /**
     * Returns [Exercise] entries for the movements that should be inserted by
     * the seeder. Reused-ID entries are omitted — those rows already exist
     * from [HomeGymCatalogSeeder].
     */
    fun buildNewExercises(): List<Exercise> =
        entries.filter { it.seed }.map { entry ->
            Exercise(
                id = entry.id,
                name = entry.name,
                muscleGroups = listOf(MuscleGroup.LEGS),
                equipment = entry.equipment,
                category = WorkoutType.PULL,
                exerciseCategory = ExerciseCategory.STRENGTH_LEGS,
                imageUrl = null,
                instructions = entry.instructions,
                isUserCreated = false
            )
        }
}
