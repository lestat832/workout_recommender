package com.workoutapp.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class StrongWorkoutData(
    val workoutNumber: Int,
    val date: Date,
    val workoutName: String,
    val duration: Int, // in seconds
    val exercises: List<StrongExerciseData>
)

data class StrongExerciseData(
    val name: String,
    val sets: List<StrongSetData>
)

data class StrongSetData(
    val setOrder: Int,
    val weight: Float, // in pounds (converted from kg)
    val reps: Int,
    val rpe: String?,
    val distance: Float?,
    val seconds: Float?,
    val notes: String?
)

object StrongCsvParser {
    private const val KG_TO_LBS = 2.20462f
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    fun parseCsvContent(csvContent: String): List<StrongWorkoutData> {
        val lines = csvContent.lines()
            .drop(1) // Skip header
            .filter { it.isNotBlank() }
        
        val workoutMap = mutableMapOf<Int, MutableList<String>>()
        
        // Group lines by workout number
        lines.forEach { line ->
            val fields = parseCsvLine(line)
            if (fields.size >= 13) {
                val workoutNumber = fields[0].trim('"').toIntOrNull() ?: return@forEach
                workoutMap.getOrPut(workoutNumber) { mutableListOf() }.add(line)
            }
        }
        
        // Parse each workout
        return workoutMap.map { (workoutNumber, workoutLines) ->
            parseWorkout(workoutNumber, workoutLines)
        }.sortedBy { it.date }
    }
    
    private fun parseWorkout(workoutNumber: Int, lines: List<String>): StrongWorkoutData {
        val firstLine = parseCsvLine(lines.first())
        val date = dateFormat.parse(firstLine[1].trim('"')) ?: Date()
        val workoutName = firstLine[2].trim('"')
        val duration = firstLine[3].trim('"').toIntOrNull() ?: 0
        
        val exerciseMap = mutableMapOf<String, MutableList<StrongSetData>>()
        
        lines.forEach { line ->
            val fields = parseCsvLine(line)
            val exerciseName = fields[4].trim('"')
            
            // Skip rest timer entries
            if (exerciseName == "Rest Timer" || fields[5].trim('"') == "Rest Timer") {
                return@forEach
            }
            
            val setOrder = fields[5].trim('"').toIntOrNull() ?: return@forEach
            val weightKg = fields[6].trim('"').toFloatOrNull() ?: 0f
            val weightLbs = (weightKg * KG_TO_LBS).roundToInt().toFloat()
            val reps = fields[7].trim('"').toIntOrNull() ?: 0
            val rpe = fields[8].trim('"').takeIf { it.isNotEmpty() }
            val distance = fields[9].trim('"').toFloatOrNull()
            val seconds = fields[10].trim('"').toFloatOrNull()
            val notes = fields[11].trim('"').takeIf { it.isNotEmpty() }
            
            val setData = StrongSetData(
                setOrder = setOrder,
                weight = weightLbs,
                reps = reps,
                rpe = rpe,
                distance = distance,
                seconds = seconds,
                notes = notes
            )
            
            exerciseMap.getOrPut(exerciseName) { mutableListOf() }.add(setData)
        }
        
        val exercises = exerciseMap.map { (name, sets) ->
            StrongExerciseData(
                name = name,
                sets = sets.sortedBy { it.setOrder }
            )
        }
        
        return StrongWorkoutData(
            workoutNumber = workoutNumber,
            date = date,
            workoutName = workoutName,
            duration = duration,
            exercises = exercises
        )
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ';' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        
        return result
    }
}