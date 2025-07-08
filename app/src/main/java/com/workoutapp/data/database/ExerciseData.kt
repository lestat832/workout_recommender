package com.workoutapp.data.database

import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

object ExerciseData {
    val exercises = listOf(
        // PUSH EXERCISES - CHEST
        ExerciseEntity("1", "Barbell Bench Press", MuscleGroup.CHEST, "Barbell", WorkoutType.PUSH),
        ExerciseEntity("2", "Dumbbell Bench Press", MuscleGroup.CHEST, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("3", "Incline Barbell Press", MuscleGroup.CHEST, "Barbell", WorkoutType.PUSH),
        ExerciseEntity("4", "Incline Dumbbell Press", MuscleGroup.CHEST, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("5", "Dumbbell Flyes", MuscleGroup.CHEST, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("6", "Cable Crossover", MuscleGroup.CHEST, "Cable", WorkoutType.PUSH),
        ExerciseEntity("7", "Push-ups", MuscleGroup.CHEST, "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("8", "Chest Dips", MuscleGroup.CHEST, "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("9", "Machine Chest Press", MuscleGroup.CHEST, "Machine", WorkoutType.PUSH),
        ExerciseEntity("10", "Decline Bench Press", MuscleGroup.CHEST, "Barbell", WorkoutType.PUSH),
        
        // PUSH EXERCISES - SHOULDER
        ExerciseEntity("11", "Overhead Press", MuscleGroup.SHOULDER, "Barbell", WorkoutType.PUSH),
        ExerciseEntity("12", "Dumbbell Shoulder Press", MuscleGroup.SHOULDER, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("13", "Arnold Press", MuscleGroup.SHOULDER, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("14", "Lateral Raises", MuscleGroup.SHOULDER, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("15", "Front Raises", MuscleGroup.SHOULDER, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("16", "Cable Lateral Raises", MuscleGroup.SHOULDER, "Cable", WorkoutType.PUSH),
        ExerciseEntity("17", "Face Pulls", MuscleGroup.SHOULDER, "Cable", WorkoutType.PUSH),
        ExerciseEntity("18", "Upright Row", MuscleGroup.SHOULDER, "Barbell", WorkoutType.PUSH),
        ExerciseEntity("19", "Machine Shoulder Press", MuscleGroup.SHOULDER, "Machine", WorkoutType.PUSH),
        ExerciseEntity("20", "Rear Delt Flyes", MuscleGroup.SHOULDER, "Dumbbell", WorkoutType.PUSH),
        
        // PUSH EXERCISES - TRICEP
        ExerciseEntity("21", "Close-Grip Bench Press", MuscleGroup.TRICEP, "Barbell", WorkoutType.PUSH),
        ExerciseEntity("22", "Tricep Dips", MuscleGroup.TRICEP, "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("23", "Overhead Tricep Extension", MuscleGroup.TRICEP, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("24", "Cable Tricep Pushdown", MuscleGroup.TRICEP, "Cable", WorkoutType.PUSH),
        ExerciseEntity("25", "Diamond Push-ups", MuscleGroup.TRICEP, "Bodyweight", WorkoutType.PUSH),
        ExerciseEntity("26", "Tricep Kickbacks", MuscleGroup.TRICEP, "Dumbbell", WorkoutType.PUSH),
        ExerciseEntity("27", "Cable Overhead Extension", MuscleGroup.TRICEP, "Cable", WorkoutType.PUSH),
        ExerciseEntity("28", "EZ-Bar Skullcrushers", MuscleGroup.TRICEP, "EZ-Bar", WorkoutType.PUSH),
        ExerciseEntity("29", "Machine Tricep Press", MuscleGroup.TRICEP, "Machine", WorkoutType.PUSH),
        ExerciseEntity("30", "Rope Pushdowns", MuscleGroup.TRICEP, "Cable", WorkoutType.PUSH),
        
        // PULL EXERCISES - BACK
        ExerciseEntity("31", "Deadlift", MuscleGroup.BACK, "Barbell", WorkoutType.PULL),
        ExerciseEntity("32", "Pull-ups", MuscleGroup.BACK, "Bodyweight", WorkoutType.PULL),
        ExerciseEntity("33", "Bent-Over Barbell Row", MuscleGroup.BACK, "Barbell", WorkoutType.PULL),
        ExerciseEntity("34", "T-Bar Row", MuscleGroup.BACK, "T-Bar", WorkoutType.PULL),
        ExerciseEntity("35", "Lat Pulldown", MuscleGroup.BACK, "Cable", WorkoutType.PULL),
        ExerciseEntity("36", "Seated Cable Row", MuscleGroup.BACK, "Cable", WorkoutType.PULL),
        ExerciseEntity("37", "Single-Arm Dumbbell Row", MuscleGroup.BACK, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("38", "Machine Row", MuscleGroup.BACK, "Machine", WorkoutType.PULL),
        ExerciseEntity("39", "Chin-ups", MuscleGroup.BACK, "Bodyweight", WorkoutType.PULL),
        ExerciseEntity("40", "Cable Pullover", MuscleGroup.BACK, "Cable", WorkoutType.PULL),
        
        // PULL EXERCISES - BICEP
        ExerciseEntity("41", "Barbell Curl", MuscleGroup.BICEP, "Barbell", WorkoutType.PULL),
        ExerciseEntity("42", "Dumbbell Curl", MuscleGroup.BICEP, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("43", "Hammer Curl", MuscleGroup.BICEP, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("44", "Preacher Curl", MuscleGroup.BICEP, "EZ-Bar", WorkoutType.PULL),
        ExerciseEntity("45", "Cable Curl", MuscleGroup.BICEP, "Cable", WorkoutType.PULL),
        ExerciseEntity("46", "Concentration Curl", MuscleGroup.BICEP, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("47", "Incline Dumbbell Curl", MuscleGroup.BICEP, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("48", "Machine Curl", MuscleGroup.BICEP, "Machine", WorkoutType.PULL),
        ExerciseEntity("49", "21s", MuscleGroup.BICEP, "Barbell", WorkoutType.PULL),
        ExerciseEntity("50", "Cable Hammer Curl", MuscleGroup.BICEP, "Cable", WorkoutType.PULL),
        
        // PULL EXERCISES - LEGS
        ExerciseEntity("51", "Barbell Squat", MuscleGroup.LEGS, "Barbell", WorkoutType.PULL),
        ExerciseEntity("52", "Front Squat", MuscleGroup.LEGS, "Barbell", WorkoutType.PULL),
        ExerciseEntity("53", "Leg Press", MuscleGroup.LEGS, "Machine", WorkoutType.PULL),
        ExerciseEntity("54", "Romanian Deadlift", MuscleGroup.LEGS, "Barbell", WorkoutType.PULL),
        ExerciseEntity("55", "Leg Curl", MuscleGroup.LEGS, "Machine", WorkoutType.PULL),
        ExerciseEntity("56", "Leg Extension", MuscleGroup.LEGS, "Machine", WorkoutType.PULL),
        ExerciseEntity("57", "Walking Lunges", MuscleGroup.LEGS, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("58", "Bulgarian Split Squat", MuscleGroup.LEGS, "Dumbbell", WorkoutType.PULL),
        ExerciseEntity("59", "Calf Raises", MuscleGroup.LEGS, "Machine", WorkoutType.PULL),
        ExerciseEntity("60", "Goblet Squat", MuscleGroup.LEGS, "Dumbbell", WorkoutType.PULL)
    )
}