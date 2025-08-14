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
    
    // Mapping of Strong exercise names to our exercise names (if they exist in our database)
    // null ourName means we'll create a custom exercise
    private val exerciseMappings = listOf(
        // Direct matches
        ExerciseMapping("Bench Press (Barbell)", "Barbell Bench Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Bench Press (Dumbbell)", "Dumbbell Bench Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Deadlift (Barbell)", "Deadlift", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Squat (Barbell)", "Barbell Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Dumbbell)", "Dumbbell Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Barbell)", "Barbell Curl", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Lat Pulldown (Cable)", "Lat Pulldown", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Shoulder Press (Dumbbell)", "Dumbbell Shoulder Press", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Triceps Dip", "Tricep Dips", listOf(MuscleGroup.TRICEP), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Chest Dip", "Chest Dips", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Pull Up", "Pull-ups", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Push Up", "Push-ups", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseMapping("Face Pull", "Face Pulls", listOf(MuscleGroup.SHOULDER), "Cable", WorkoutType.PUSH),
        
        // Close matches with slight name differences
        ExerciseMapping("Bent Over Row (Barbell)", "Bent-Over Barbell Row", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Bent Over One Arm Row (Dumbbell)", "Single-Arm Dumbbell Row", listOf(MuscleGroup.BACK), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Romanian Deadlift (Barbell)", "Romanian Deadlift", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Overhead Press (Barbell)", "Overhead Press", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Bench Press - Close Grip (Barbell)", "Close-Grip Bench Press", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Lateral Raise (Dumbbell)", "Lateral Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Front Raise (Dumbbell)", "Front Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Skullcrusher (Barbell)", "EZ-Bar Skullcrushers", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Triceps Extension (Dumbbell)", "Overhead Tricep Extension", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Fly (Dumbbell)", "Dumbbell Flyes", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Bench Press (Barbell)", "Incline Barbell Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Incline Bench Press (Dumbbell)", "Incline Dumbbell Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Decline Bench Press (Barbell)", "Decline Bench Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Hammer Curl (Dumbbell)", "Hammer Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Preacher Curl (Barbell)", "Preacher Curl", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Cable Fly", "Cable Crossover", listOf(MuscleGroup.CHEST), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Seated Row (Cable)", "Seated Cable Row", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseMapping("Leg Press", "Leg Press", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Leg Curl (Machine)", "Leg Curl", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Leg Extension (Machine)", "Leg Extension", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Calf Raise (Machine)", "Calf Raises", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseMapping("Bulgarian Split Squat", "Bulgarian Split Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        
        // Custom exercises not in our database - will be created as custom
        ExerciseMapping("Deadlift (Dumbbell)", null, listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Deadlift Clean Combo", null, listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Bench / Shrug", null, listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Back Extension", null, listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Back Extension (Machine)", null, listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseMapping("Back Raises", null, listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Chest Dip (Assisted)", null, listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Chest Press (Machine)", null, listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseMapping("Bicep Curl (Cable)", null, listOf(MuscleGroup.BICEP), "Cable", WorkoutType.PULL),
        ExerciseMapping("Bicep Curl (Machine)", null, listOf(MuscleGroup.BICEP), "Machine", WorkoutType.PULL),
        ExerciseMapping("Tricep Extension (Cable)", null, listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseMapping("Shrug (Barbell)", null, listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Shrug (Dumbbell)", null, listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Good Morning (Barbell)", null, listOf(MuscleGroup.BACK, MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Hip Thrust (Barbell)", null, listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Goblet Squat", null, listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Front Squat (Barbell)", null, listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseMapping("Lunge (Dumbbell)", null, listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseMapping("Step Up", null, listOf(MuscleGroup.LEGS), "Bodyweight", WorkoutType.PULL),
        ExerciseMapping("Reverse Fly (Dumbbell)", null, listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseMapping("Upright Row (Barbell)", null, listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseMapping("Pullover (Dumbbell)", null, listOf(MuscleGroup.CHEST, MuscleGroup.BACK), "Dumbbell", WorkoutType.PUSH)
    )
    
    fun getMappingForExercise(strongExerciseName: String): ExerciseMapping {
        // Try to find exact match
        exerciseMappings.find { it.strongName.equals(strongExerciseName, ignoreCase = true) }?.let {
            return it
        }
        
        // Try to find partial match (for variations we haven't mapped)
        val simplifiedName = strongExerciseName.replace(Regex("\\(.*?\\)"), "").trim()
        exerciseMappings.find { 
            it.strongName.contains(simplifiedName, ignoreCase = true) || 
            simplifiedName.contains(it.strongName.replace(Regex("\\(.*?\\)"), "").trim(), ignoreCase = true)
        }?.let {
            return it.copy(strongName = strongExerciseName)
        }
        
        // Default mapping for unknown exercises
        return createDefaultMapping(strongExerciseName)
    }
    
    private fun createDefaultMapping(exerciseName: String): ExerciseMapping {
        // Try to determine muscle group and equipment from name
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
            else -> listOf(MuscleGroup.CHEST) // Default to chest
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
            ourName = null, // Will create as custom exercise
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