package com.workoutapp.domain.mapper

import com.workoutapp.domain.formatter.StravaDescriptionFormatter
import com.workoutapp.domain.model.*
import com.workoutapp.domain.model.Set as WorkoutSet
import java.util.*

/**
 * Test/Example usage for Strava mapping and formatting
 *
 * This file demonstrates how the mapper and formatter work together.
 * Run the main() function to see example output.
 *
 * Usage in production code:
 * ```kotlin
 * val stravaRequest = WorkoutToStravaMapper.mapToActivityRequest(
 *     workout = myWorkout,
 *     startTime = workoutStartMillis,
 *     endTime = workoutEndMillis
 * )
 *
 * // Then send stravaRequest to Strava API
 * stravaApi.createActivity(stravaRequest)
 * ```
 */
object StravaMapperTest {

    /**
     * Example workout for testing formatting
     */
    fun createSampleWorkout(): Workout {
        // Create sample exercises
        val benchPress = Exercise(
            id = "1",
            name = "Barbell Bench Press",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Barbell",
            category = WorkoutType.PUSH,
            imageUrl = null,
            instructions = emptyList(),
            isUserCreated = false
        )

        val inclinePress = Exercise(
            id = "2",
            name = "Incline Dumbbell Press",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = null,
            instructions = emptyList(),
            isUserCreated = false
        )

        val squats = Exercise(
            id = "3",
            name = "Squats",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Barbell",
            category = WorkoutType.PUSH,
            imageUrl = null,
            instructions = emptyList(),
            isUserCreated = false
        )

        val legPress = Exercise(
            id = "4",
            name = "Leg Press",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Machine",
            category = WorkoutType.PUSH,
            imageUrl = null,
            instructions = emptyList(),
            isUserCreated = false
        )

        // Create workout exercises with sets
        val workoutExercises = listOf(
            WorkoutExercise(
                id = "we1",
                workoutId = "w1",
                exercise = benchPress,
                sets = listOf(
                    WorkoutSet(reps = 10, weight = 135f, completed = true),
                    WorkoutSet(reps = 10, weight = 135f, completed = true),
                    WorkoutSet(reps = 10, weight = 135f, completed = true)
                )
            ),
            WorkoutExercise(
                id = "we2",
                workoutId = "w1",
                exercise = inclinePress,
                sets = listOf(
                    WorkoutSet(reps = 12, weight = 50f, completed = true),
                    WorkoutSet(reps = 12, weight = 50f, completed = true),
                    WorkoutSet(reps = 12, weight = 50f, completed = true)
                )
            ),
            WorkoutExercise(
                id = "we3",
                workoutId = "w1",
                exercise = squats,
                sets = listOf(
                    WorkoutSet(reps = 8, weight = 185f, completed = true),
                    WorkoutSet(reps = 8, weight = 185f, completed = true),
                    WorkoutSet(reps = 8, weight = 185f, completed = true),
                    WorkoutSet(reps = 8, weight = 185f, completed = true)
                )
            ),
            WorkoutExercise(
                id = "we4",
                workoutId = "w1",
                exercise = legPress,
                sets = listOf(
                    WorkoutSet(reps = 15, weight = 270f, completed = true),
                    WorkoutSet(reps = 15, weight = 270f, completed = true),
                    WorkoutSet(reps = 15, weight = 270f, completed = true)
                )
            )
        )

        // Create the workout
        return Workout(
            id = "w1",
            date = Date(),
            type = WorkoutType.PUSH,
            status = WorkoutStatus.COMPLETED,
            exercises = workoutExercises
        )
    }

    /**
     * Test the formatter with sample data
     */
    fun testFormatter() {
        println("=== STRAVA DESCRIPTION FORMATTER TEST ===\n")

        val workout = createSampleWorkout()

        // Simulate a 58-minute workout
        val startTime = System.currentTimeMillis() - (58 * 60 * 1000)
        val endTime = System.currentTimeMillis()

        val description = StravaDescriptionFormatter.format(
            workout = workout,
            startTime = startTime,
            endTime = endTime
        )

        println("Generated Description:")
        println("─".repeat(50))
        println(description)
        println("─".repeat(50))
        println()

        // Calculate expected volume
        val expectedVolume = (3 * 10 * 135) + (3 * 12 * 50) + (4 * 8 * 185) + (3 * 15 * 270)
        println("Expected Total Volume: $expectedVolume lbs")
        println()
    }

    /**
     * Test the complete mapper
     */
    fun testMapper() {
        println("=== STRAVA ACTIVITY MAPPER TEST ===\n")

        val workout = createSampleWorkout()

        // Simulate workout that started at 2:00 PM and ended at 3:00 PM today
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = startTime + (58 * 60 * 1000) // 58 minutes later

        val activityRequest = WorkoutToStravaMapper.mapToActivityRequest(
            workout = workout,
            startTime = startTime,
            endTime = endTime
        )

        println("Generated Strava Activity Request:")
        println("─".repeat(50))
        println("Name: ${activityRequest.name}")
        println("Type: ${activityRequest.type}")
        println("Sport Type: ${activityRequest.sportType}")
        println("Start Date: ${activityRequest.startDateLocal}")
        println("Elapsed Time: ${activityRequest.elapsedTime} seconds (${activityRequest.elapsedTime / 60} minutes)")
        println()
        println("Description:")
        println(activityRequest.description)
        println("─".repeat(50))
        println()
    }

    /**
     * Test edge cases
     */
    fun testEdgeCases() {
        println("=== EDGE CASES TEST ===\n")

        // Test 1: Workout with no completed sets
        println("Test 1: Workout with incomplete sets")
        val incompleteWorkout = Workout(
            id = "w2",
            date = Date(),
            type = WorkoutType.PULL,
            status = WorkoutStatus.INCOMPLETE,
            exercises = listOf(
                WorkoutExercise(
                    id = "we1",
                    workoutId = "w2",
                    exercise = Exercise(
                        id = "1",
                        name = "Pull-ups",
                        muscleGroups = listOf(MuscleGroup.BACK),
                        equipment = "Bodyweight",
                        category = WorkoutType.PULL,
                        isUserCreated = false
                    ),
                    sets = listOf(
                        WorkoutSet(reps = 10, weight = 0f, completed = false),
                        WorkoutSet(reps = 10, weight = 0f, completed = false)
                    )
                )
            )
        )

        val description1 = StravaDescriptionFormatter.format(incompleteWorkout)
        println(description1)
        println()

        // Test 2: Workout with varying sets
        println("Test 2: Workout with varying weights/reps")
        val varyingWorkout = Workout(
            id = "w3",
            date = Date(),
            type = WorkoutType.PUSH,
            status = WorkoutStatus.COMPLETED,
            exercises = listOf(
                WorkoutExercise(
                    id = "we1",
                    workoutId = "w3",
                    exercise = Exercise(
                        id = "1",
                        name = "Barbell Bench Press (Progressive)",
                        muscleGroups = listOf(MuscleGroup.CHEST),
                        equipment = "Barbell",
                        category = WorkoutType.PUSH,
                        isUserCreated = false
                    ),
                    sets = listOf(
                        WorkoutSet(reps = 10, weight = 135f, completed = true),
                        WorkoutSet(reps = 8, weight = 155f, completed = true),
                        WorkoutSet(reps = 6, weight = 175f, completed = true),
                        WorkoutSet(reps = 4, weight = 185f, completed = true)
                    )
                )
            )
        )

        val description2 = StravaDescriptionFormatter.format(varyingWorkout)
        println(description2)
        println()
    }
}

/**
 * Run this to test the formatters
 *
 * In Android Studio:
 * 1. Right-click this file
 * 2. Select "Run 'StravaMapperTestKt'"
 *
 * Or add this to a debug screen in your app to see the output
 */
fun main() {
    StravaMapperTest.testFormatter()
    println("\n")
    StravaMapperTest.testMapper()
    println("\n")
    StravaMapperTest.testEdgeCases()
}
