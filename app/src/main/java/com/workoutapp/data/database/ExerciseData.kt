package com.workoutapp.data.database

import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

object ExerciseData {
    val exercises = listOf(
        // PUSH EXERCISES - CHEST
        ExerciseEntity("1", "Barbell Bench Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseEntity("2", "Dumbbell Bench Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("3", "Incline Barbell Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        ExerciseEntity("4", "Incline Dumbbell Press", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("5", "Dumbbell Flyes", listOf(MuscleGroup.CHEST), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("6", "Cable Crossover", listOf(MuscleGroup.CHEST), "Cable", WorkoutType.PUSH),
        ExerciseEntity("7", "Push-ups", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("8", "Chest Dips", listOf(MuscleGroup.CHEST), "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("9", "Machine Chest Press", listOf(MuscleGroup.CHEST), "Machine", WorkoutType.PUSH),
        ExerciseEntity("10", "Decline Bench Press", listOf(MuscleGroup.CHEST), "Barbell", WorkoutType.PUSH),
        
        // PUSH EXERCISES - SHOULDER
        ExerciseEntity("11", "Overhead Press", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseEntity("12", "Dumbbell Shoulder Press", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("13", "Arnold Press", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("14", "Lateral Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("15", "Front Raises", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("16", "Cable Lateral Raises", listOf(MuscleGroup.SHOULDER), "Cable", WorkoutType.PUSH),
        ExerciseEntity("17", "Face Pulls", listOf(MuscleGroup.SHOULDER), "Cable", WorkoutType.PUSH),
        ExerciseEntity("18", "Upright Row", listOf(MuscleGroup.SHOULDER), "Barbell", WorkoutType.PUSH),
        ExerciseEntity("19", "Machine Shoulder Press", listOf(MuscleGroup.SHOULDER), "Machine", WorkoutType.PUSH),
        ExerciseEntity("20", "Rear Delt Flyes", listOf(MuscleGroup.SHOULDER), "Dumbbell", WorkoutType.PUSH),
        
        // PUSH EXERCISES - TRICEP
        ExerciseEntity("21", "Close-Grip Bench Press", listOf(MuscleGroup.TRICEP), "Barbell", WorkoutType.PUSH),
        ExerciseEntity("22", "Tricep Dips", listOf(MuscleGroup.TRICEP), "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("23", "Overhead Tricep Extension", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("24", "Cable Tricep Pushdown", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseEntity("25", "Diamond Push-ups", listOf(MuscleGroup.TRICEP), "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("26", "Tricep Kickbacks", listOf(MuscleGroup.TRICEP), "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("27", "Cable Overhead Extension", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        ExerciseEntity("28", "EZ-Bar Skullcrushers", listOf(MuscleGroup.TRICEP), "EZ-Bar", WorkoutType.PUSH),
        ExerciseEntity("29", "Machine Tricep Press", listOf(MuscleGroup.TRICEP), "Machine", WorkoutType.PUSH),
        ExerciseEntity("30", "Rope Pushdowns", listOf(MuscleGroup.TRICEP), "Cable", WorkoutType.PUSH),
        
        // PULL EXERCISES - BACK
        ExerciseEntity("31", "Deadlift", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseEntity("32", "Pull-ups", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseEntity("33", "Bent-Over Barbell Row", listOf(MuscleGroup.BACK), "Barbell", WorkoutType.PULL),
        ExerciseEntity("34", "T-Bar Row", listOf(MuscleGroup.BACK), "T-Bar", WorkoutType.PULL),
        ExerciseEntity("35", "Lat Pulldown", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseEntity("36", "Seated Cable Row", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        ExerciseEntity("37", "Single-Arm Dumbbell Row", listOf(MuscleGroup.BACK), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("38", "Machine Row", listOf(MuscleGroup.BACK), "Machine", WorkoutType.PULL),
        ExerciseEntity("39", "Chin-ups", listOf(MuscleGroup.BACK), "Bodyweight", WorkoutType.PULL),
        ExerciseEntity("40", "Cable Pullover", listOf(MuscleGroup.BACK), "Cable", WorkoutType.PULL),
        
        // PULL EXERCISES - BICEP
        ExerciseEntity("41", "Barbell Curl", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseEntity("42", "Dumbbell Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("43", "Hammer Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("44", "Preacher Curl", listOf(MuscleGroup.BICEP), "EZ-Bar", WorkoutType.PULL),
        ExerciseEntity("45", "Cable Curl", listOf(MuscleGroup.BICEP), "Cable", WorkoutType.PULL),
        ExerciseEntity("46", "Concentration Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("47", "Incline Dumbbell Curl", listOf(MuscleGroup.BICEP), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("48", "Machine Curl", listOf(MuscleGroup.BICEP), "Machine", WorkoutType.PULL),
        ExerciseEntity("49", "21s", listOf(MuscleGroup.BICEP), "Barbell", WorkoutType.PULL),
        ExerciseEntity("50", "Cable Hammer Curl", listOf(MuscleGroup.BICEP), "Cable", WorkoutType.PULL),
        
        // PULL EXERCISES - LEGS
        ExerciseEntity("51", "Barbell Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseEntity("52", "Front Squat", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseEntity("53", "Leg Press", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseEntity("54", "Romanian Deadlift", listOf(MuscleGroup.LEGS), "Barbell", WorkoutType.PULL),
        ExerciseEntity("55", "Leg Curl", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseEntity("56", "Leg Extension", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseEntity("57", "Walking Lunges", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("58", "Bulgarian Split Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("59", "Calf Raises", listOf(MuscleGroup.LEGS), "Machine", WorkoutType.PULL),
        ExerciseEntity("60", "Goblet Squat", listOf(MuscleGroup.LEGS), "Dumbbell", WorkoutType.PULL)
    )
}