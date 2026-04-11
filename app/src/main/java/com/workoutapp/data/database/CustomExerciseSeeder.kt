package com.workoutapp.data.database

import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.ExerciseCategory
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

/**
 * Custom exercises seeded on first install (Phase 3). Covers home-gym
 * equipment that is not in the Free Exercise DB CDN: TRX suspension trainer,
 * medicine ball, ab wheel, and the cardio conditioning stations (rower, bike,
 * jump rope).
 *
 * Strength-style entries use WorkoutType.PUSH/PULL on the legacy `category`
 * field for backward compatibility, and set `exerciseCategory` explicitly to
 * the correct taxonomy value. Cardio stations are tagged CARDIO_CONDITIONING
 * with empty muscleGroups — the generator filters them via exerciseCategory
 * directly, not via muscle group targeting.
 */
object CustomExerciseSeeder {

    val exercises: List<Exercise> = listOf(
        // TRX — strength
        Exercise(
            id = "custom_trx_chest_press",
            name = "TRX Chest Press",
            muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEP),
            equipment = "Suspension Trainer",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
            instructions = listOf(
                "Face away from the anchor, grip the handles at chest height.",
                "Lean into the straps with arms extended, body in a plank line.",
                "Lower the chest between the handles, elbows flaring slightly.",
                "Press back to the start by contracting the chest and triceps."
            )
        ),
        Exercise(
            id = "custom_trx_row",
            name = "TRX Row",
            muscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.BICEP),
            equipment = "Suspension Trainer",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.STRENGTH_PULL,
            instructions = listOf(
                "Face the anchor, grip the handles, lean back with arms extended.",
                "Pull the chest toward the handles by driving the elbows back.",
                "Squeeze the shoulder blades together at the top.",
                "Lower under control to the starting position."
            )
        ),
        Exercise(
            id = "custom_trx_atomic_pushup",
            name = "TRX Atomic Push-Up",
            muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.CORE),
            equipment = "Suspension Trainer",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
            instructions = listOf(
                "Place both feet in the foot cradles in a push-up position.",
                "Lower the chest toward the floor, maintaining a tight core.",
                "Press back up while tucking the knees toward the chest.",
                "Extend the legs back and repeat."
            )
        ),
        Exercise(
            id = "custom_trx_tricep_extension",
            name = "TRX Tricep Extension",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Suspension Trainer",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
            instructions = listOf(
                "Face away from the anchor, grip the handles overhead.",
                "Lean forward with elbows fixed and forearms behind the head.",
                "Extend the arms fully by contracting the triceps.",
                "Return to the bent position under control."
            )
        ),
        Exercise(
            id = "custom_trx_bicep_curl",
            name = "TRX Bicep Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Suspension Trainer",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.STRENGTH_PULL,
            instructions = listOf(
                "Face the anchor, grip the handles palms up, lean back.",
                "Keep elbows high and fixed as you curl toward the forehead.",
                "Squeeze the biceps at the top of the curl.",
                "Lower under control and repeat."
            )
        ),
        Exercise(
            id = "custom_trx_y_fly",
            name = "TRX Y-Fly",
            muscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.SHOULDER),
            equipment = "Suspension Trainer",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.STRENGTH_PULL,
            instructions = listOf(
                "Face the anchor, arms extended overhead in a Y shape.",
                "Pull the body toward the anchor keeping the Y shape.",
                "Squeeze the rear delts and mid-back at the top.",
                "Lower to start under control."
            )
        ),
        Exercise(
            id = "custom_trx_pike_pushup",
            name = "TRX Pike Push-Up",
            muscleGroups = listOf(MuscleGroup.SHOULDER, MuscleGroup.TRICEP),
            equipment = "Suspension Trainer",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
            instructions = listOf(
                "Place feet in the foot cradles, hands on the floor in a pike.",
                "Lower the top of the head toward the floor between the hands.",
                "Press back up to the pike position by contracting the shoulders.",
                "Maintain the pike throughout the movement."
            )
        ),
        Exercise(
            id = "custom_trx_pistol_squat",
            name = "TRX Pistol Squat",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Suspension Trainer",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.STRENGTH_LEGS,
            instructions = listOf(
                "Grip the handles for balance, extend one leg forward.",
                "Lower into a single-leg squat on the standing leg.",
                "Use the straps to assist balance, not to pull yourself up.",
                "Drive through the heel to return to standing."
            )
        ),

        // Medicine ball
        Exercise(
            id = "custom_med_ball_slam",
            name = "Medicine Ball Slam",
            muscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.CORE),
            equipment = "Medicine Ball",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.STRENGTH_PULL,
            instructions = listOf(
                "Hold the medicine ball overhead with arms extended.",
                "Engage the lats and core and slam the ball to the floor.",
                "Squat to pick up the ball and return to the overhead position.",
                "Keep the movement explosive on every rep."
            )
        ),
        Exercise(
            id = "custom_med_ball_chest_pass",
            name = "Medicine Ball Chest Pass",
            muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEP),
            equipment = "Medicine Ball",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.STRENGTH_PUSH,
            instructions = listOf(
                "Hold the ball at chest height with both hands.",
                "Explosively push the ball forward at a wall or target.",
                "Catch the rebound or pick it up and reset.",
                "Focus on chest and triceps driving the pass."
            )
        ),
        Exercise(
            id = "custom_med_ball_russian_twist",
            name = "Medicine Ball Russian Twist",
            muscleGroups = listOf(MuscleGroup.CORE),
            equipment = "Medicine Ball",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.CORE,
            instructions = listOf(
                "Sit on the floor with knees bent and feet lifted.",
                "Hold the medicine ball at chest height with both hands.",
                "Rotate the torso to tap the ball to one side of the hips.",
                "Alternate sides with control, keeping the core engaged."
            )
        ),

        // Ab wheel
        Exercise(
            id = "custom_ab_wheel_rollout",
            name = "Ab Wheel Rollout",
            muscleGroups = listOf(MuscleGroup.CORE),
            equipment = "Ab Wheel",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.CORE,
            instructions = listOf(
                "Kneel on a mat gripping the ab wheel with both hands.",
                "Roll the wheel forward, extending the arms and body.",
                "Stop before the hips sag or the lower back arches.",
                "Contract the core to pull the wheel back to the start."
            )
        ),

        // Cardio conditioning stations
        Exercise(
            id = "custom_rower_interval",
            name = "Rower Interval",
            muscleGroups = emptyList(),
            equipment = "Indoor Rower",
            category = WorkoutType.PULL,
            exerciseCategory = ExerciseCategory.CARDIO_CONDITIONING,
            instructions = listOf(
                "Row at a hard pace for the minute.",
                "Drive through the legs, then hips, then pull with the arms.",
                "Control the return to the catch position.",
                "Aim for steady power throughout the interval."
            )
        ),
        Exercise(
            id = "custom_bike_interval",
            name = "Bike Interval",
            muscleGroups = emptyList(),
            equipment = "Indoor Bike",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.CARDIO_CONDITIONING,
            instructions = listOf(
                "Ride at a hard pace for the minute.",
                "Maintain a high cadence and engage the core.",
                "Drive through both legs evenly.",
                "Aim for steady power throughout the interval."
            )
        ),
        Exercise(
            id = "custom_jump_rope_interval",
            name = "Jump Rope Interval",
            muscleGroups = emptyList(),
            equipment = "Jump Rope",
            category = WorkoutType.PUSH,
            exerciseCategory = ExerciseCategory.CARDIO_CONDITIONING,
            instructions = listOf(
                "Skip at a steady pace for the minute.",
                "Keep the elbows close to the body and wrists relaxed.",
                "Land softly on the balls of the feet.",
                "Mix in double-unders if comfortable."
            )
        )
    )
}
