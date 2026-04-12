package com.workoutapp.data.utils

import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

data class ExerciseMapping(
    val strongName: String,
    val ourName: String?,
    val muscleGroups: List<MuscleGroup>,
    val equipment: String,
    val category: WorkoutType
)

object ExerciseMapper {

    // Complete mapping of Hevy exercise names to catalog exercise names.
    // Every Hevy exercise gets an explicit entry — no partial matching needed.
    private val exerciseMappings = listOf(
        // ── CHEST ────────────────────────────────────────────────────
        ExerciseMapping("Bench Press (Barbell)", "Barbell Bench Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Bench Press (Dumbbell)", "Dumbbell Bench Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Bench Press (Dumbbell)", "Incline Dumbbell Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Bench Press (Barbell)", "Incline Bench Press (Barbell)", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Bench Press (Smith Machine)", "Incline Bench Press (Smith Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Decline Bench Press (Barbell)", "Decline Bench Press (Barbell)", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Decline Bench Press (Machine)", "Decline Bench Press (Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Decline Bench Press (Smith Machine)", "Decline Bench Press (Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Cable Fly", "Cable Crossover", listOf(MuscleGroup.CHEST), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Fly (Dumbbell)", "Fly (Dumbbell)", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Chest Fly (Dumbbell)", "Incline Chest Fly (Dumbbell)", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Chest Press (Machine)", "Incline Chest Press (Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Chest Press (Machine)", "Chest Press (Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Iso-Lateral Chest Press (Machine)", "Iso-Lateral Chest Press (Machine)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Incline Chest/ Shrug Combo", "Incline Chest / Shrug Combo", listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Bench / Shrug", "Bench / Shrug", listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Push Up", "Push-Ups", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Pushup Row", "Pushup Row", listOf(MuscleGroup.CHEST, MuscleGroup.BACK), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Pullover (Dumbbell)", "Pullover (Dumbbell)", listOf(MuscleGroup.BACK, MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),

        // ── DIPS (all variants → Dips) ──────────────────────────────
        ExerciseMapping("Triceps Dip", "Dips", listOf(MuscleGroup.TRICEP), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Chest Dip", "Dips", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Chest Dip (Weighted)", "Dips", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Chest Dip (Assisted)", "Chest Dip (Assisted)", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),

        // ── SHOULDER ─────────────────────────────────────────────────
        ExerciseMapping("Shoulder Press (Dumbbell)", "Dumbbell Shoulder Press", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Overhead Press (Dumbbell)", "Dumbbell Shoulder Press", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Overhead Press (Barbell)", "Barbell Shoulder Press", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Overhead Press (Smith Machine)", "Overhead Press (Smith Machine)", listOf(MuscleGroup.SHOULDER), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Shoulder Press (Machine Plates)", "Shoulder Press (Machine Plates)", listOf(MuscleGroup.SHOULDER), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Seated Shoulder Press (Machine)", "Seated Shoulder Press (Machine)", listOf(MuscleGroup.SHOULDER), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Lateral Raise (Dumbbell)", "Lateral Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Lateral Raise (Machine)", "Lateral Raise (Machine)", listOf(MuscleGroup.SHOULDER), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Front Raise (Dumbbell)", "Front Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Face Pull", "Face Pulls", listOf(MuscleGroup.SHOULDER), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Reverse Fly (Dumbbell)", "Reverse Fly (Dumbbell)", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Shrug (Barbell)", "Shrug (Barbell)", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Shrug (Dumbbell)", "Shrug (Dumbbell)", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Upright Row (Barbell)", "Upright Row (Barbell)", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Upright Row (Dumbbell)", "Upright Row (Dumbbell)", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),

        // ── TRICEP ───────────────────────────────────────────────────
        ExerciseMapping("Bench Press - Close Grip (Barbell)", "Close-Grip Bench Press", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Bench Press Close Grip", "Close-Grip Bench Press", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Skullcrusher (Barbell)", "Skullcrusher", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Triceps Extension (Dumbbell)", "Overhead Tricep Extension", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Triceps Extension (Cable)", "Cable Tricep Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Triceps Pressdown", "Cable Tricep Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Triceps Pushdown", "Cable Tricep Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Triceps Pushdown Rope", "Triceps Rope Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Triceps Rope Pushdown", "Triceps Rope Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Tricep V", "Triceps V", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Triceps - Horn", "Triceps V", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Tricep Kickback", "Tricep Kickback", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Triceps Kickback (Dumbbell)", "Tricep Kickback", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),

        // ── BACK ─────────────────────────────────────────────────────
        ExerciseMapping("Bent Over Row (Barbell)", "Bent Over Barbell Row", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Bent Over Row (Dumbbell)", "Heavy DB Row", listOf(MuscleGroup.BACK), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Bent Over One Arm Row (Dumbbell)", "Heavy DB Row", listOf(MuscleGroup.BACK), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Dumbbell Row", "Heavy DB Row", listOf(MuscleGroup.BACK), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Lat Pulldown (Cable)", "Lat Pulldown", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Lat Pulldown - Close Grip (Cable)", "Lat Pulldown - Close Grip (Cable)", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Lat Pulldown (Machine)", "Lat Pulldown (Machine)", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Seated Row (Cable)", "Seated Cable Row", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Seated Row (Machine)", "Seated Row (Machine)", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Iso-Lateral High Row (Machine)", "Iso-Lateral High Row (Machine)", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Iso-Lateral Row (Machine)", "Iso-Lateral Row (Machine)", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Standing Back Row", "Standing Back Row", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Pull Up", "Pull-ups", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Pull Up (Weighted)", "Pull-ups", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Pull Up (Assisted)", "Pull Up (Assisted)", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Deadlift (Barbell)", "Deadlifts", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Back Extension (Hyperextension)", "Back Raises", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Back Extension (Machine)", "Back Raises", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Back Extension (Weighted Hyperextension)", "Back Raises", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Back Raises", "Back Raises", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),

        // ── BICEP ────────────────────────────────────────────────────
        ExerciseMapping("Bicep Curl (Dumbbell)", "Dumbbell Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Barbell)", "Barbell Curl", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Cable)", "Cable Curl", listOf(MuscleGroup.BICEP), "Cable", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Machine)", "Bicep Curl (Machine)", listOf(MuscleGroup.BICEP), "Machine", WorkoutType.PULL),
        ExerciseMapping("Hammer Curl (Dumbbell)", "Hammer Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Cross Body Hammer Curl", "Cross Body Hammer Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Preacher Curl (Barbell)", "Preacher Curl", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Seated Incline Curl (Dumbbell)", "Seated Incline Curl (Dumbbell)", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),

        // ── LEGS ─────────────────────────────────────────────────────
        ExerciseMapping("Squat (Barbell)", "Barbell Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Squat (Dumbbell)", "Dumbbell Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Front Squat", "Barbell Front Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Hack Squat (Machine)", "Hack Squat", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Hex Squat", "Hex Squat", listOf(MuscleGroup.LEGS), "Hex Bar", WorkoutType.PULL),
        ExerciseMapping("Goblet Squat", "Goblet Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Kettlebell Goblet Squat", "Goblet Squat", listOf(MuscleGroup.LEGS), "Kettlebell", WorkoutType.PULL),
        ExerciseMapping("Bulgarian Split Squat", "Bulgarian Split Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Leg Press", "Leg Press", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Leg Press (Machine)", "Leg Press", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Leg Curl (Machine)", "Leg Curls", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Lying Leg Curl (Machine)", "Leg Curls", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Leg Extension (Machine)", "Leg Extension", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Romanian Deadlift (Barbell)", "Romanian Deadlift", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Walking Lunge", "Walking Lunges", listOf(MuscleGroup.LEGS), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Lunge (Dumbbell)", "Walking Lunges (DB)", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Calf Raise (Machine)", "Calf Raise", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Standing Calf Raise (Dumbbell)", "Calf Raise", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Good Morning (Barbell)", "Good Morning (Barbell)", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Hip Thrust (Barbell)", "Hip Thrust (Barbell)", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Deadlift (Dumbbell)", "Deadlift (Dumbbell)", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Single Leg RDLs", "Single Leg RDLs", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Inner / Outer Thigh", "Inner / Outer Thigh", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Dumbbell Step Up", "Dumbbell Step Up", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Jump Squat", "Jump Squats", listOf(MuscleGroup.LEGS), "Bodyweight", WorkoutType.PULL),

        // ── COMPOUND / COMBOS ────────────────────────────────────────
        ExerciseMapping("Deadlift/Clean/Upright Row", "Deadlift \u2192 Clean \u2192 Upright Row", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Deadlift Clean Combo", "Deadlift Clean Combo", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Squat Curl", "Squat Curl (DB)", listOf(MuscleGroup.LEGS, MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Squat Row", "TRX Squat Row", listOf(MuscleGroup.LEGS, MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Clean", "Clean", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Dumbbell Clean", "Dumbbell Clean", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("KB Complex (Row Clean Squat Press)", "KB Complex (Row Clean Squat Press)", listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Kettlebell", WorkoutType.PULL),
        ExerciseMapping("Walking Lunge / Deadlift / Farmers Carry / Squat", "Walking Lunge / Deadlift / Farmers Carry / Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Thruster (Barbell)", "Thruster (Barbell)", listOf(MuscleGroup.LEGS, MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Thruster (Dumbell)", "Thruster (Dumbell)", listOf(MuscleGroup.LEGS, MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Thruster (Kettlebell)", "Thruster (Kettlebell)", listOf(MuscleGroup.LEGS, MuscleGroup.SHOULDER), "Kettlebell", WorkoutType.PUSH),

        // ── CORE ─────────────────────────────────────────────────────
        ExerciseMapping("Hanging Leg Raise", "Hanging Leg Raise", listOf(MuscleGroup.CORE), "Bodyweight", WorkoutType.PULL)
    )

    fun getMappingForExercise(strongExerciseName: String): ExerciseMapping {
        // Explicit match only — every Hevy exercise has an entry
        exerciseMappings.find { it.strongName.equals(strongExerciseName, ignoreCase = true) }?.let {
            return it
        }

        // Fallback for any unmapped exercise (future Hevy exports with new exercises)
        return createDefaultMapping(strongExerciseName)
    }

    private fun createDefaultMapping(exerciseName: String): ExerciseMapping {
        val nameLower = exerciseName.lowercase()

        val muscleGroups = when {
            nameLower.contains("chest") || nameLower.contains("bench") || nameLower.contains("fly") -> listOf(MuscleGroup.CHEST)
            nameLower.contains("shoulder") || nameLower.contains("press") && !nameLower.contains("bench") -> listOf(MuscleGroup.SHOULDER)
            nameLower.contains("back") || nameLower.contains("row") || nameLower.contains("pull") -> listOf(MuscleGroup.BACK)
            nameLower.contains("bicep") || nameLower.contains("curl") && !nameLower.contains("leg") -> listOf(MuscleGroup.BICEP)
            nameLower.contains("tricep") || nameLower.contains("extension") || nameLower.contains("dip") -> listOf(MuscleGroup.TRICEP)
            nameLower.contains("leg") || nameLower.contains("squat") || nameLower.contains("lunge") || nameLower.contains("calf") -> listOf(MuscleGroup.LEGS)
            nameLower.contains("deadlift") -> listOf(MuscleGroup.BACK, MuscleGroup.LEGS)
            nameLower.contains("core") || nameLower.contains("ab") || nameLower.contains("plank") -> listOf(MuscleGroup.CORE)
            else -> listOf(MuscleGroup.CHEST)
        }

        val equipment = when {
            nameLower.contains("(barbell)") || nameLower.contains("barbell") -> "Barbell"
            nameLower.contains("(dumbbell)") || nameLower.contains("dumbbell") -> "Dumbbell"
            nameLower.contains("(cable)") || nameLower.contains("cable") -> "Cable"
            nameLower.contains("(machine)") || nameLower.contains("machine") -> "Machine"
            nameLower.contains("(band)") || nameLower.contains("band") -> "Resistance Band"
            nameLower.contains("bodyweight") || nameLower.contains("push up") || nameLower.contains("pull up") || nameLower.contains("dip") -> "Bodyweight"
            else -> "Other"
        }

        val category = when {
            muscleGroups.contains(MuscleGroup.CHEST) || muscleGroups.contains(MuscleGroup.SHOULDER) || muscleGroups.contains(MuscleGroup.TRICEP) -> WorkoutType.PUSH
            else -> WorkoutType.PULL
        }

        return ExerciseMapping(
            strongName = exerciseName,
            ourName = exerciseName,
            muscleGroups = muscleGroups,
            equipment = equipment,
            category = category
        )
    }

    fun determineWorkoutType(exercises: List<String>): WorkoutType {
        var pushCount = 0
        var pullCount = 0

        exercises.forEach { exerciseName ->
            val mapping = getMappingForExercise(exerciseName)
            when (mapping.category) {
                WorkoutType.PUSH -> pushCount++
                WorkoutType.PULL -> pullCount++
            }
        }

        return if (pushCount >= pullCount) WorkoutType.PUSH else WorkoutType.PULL
    }
}
