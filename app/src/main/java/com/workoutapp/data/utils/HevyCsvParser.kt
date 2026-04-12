package com.workoutapp.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HevyWorkoutData(
    val title: String,
    val startTime: Date,
    val endTime: Date,
    val durationMinutes: Int,
    val exercises: List<HevyExerciseData>
)

data class HevyExerciseData(
    val name: String,
    val sets: List<HevySetData>
)

data class HevySetData(
    val setIndex: Int,
    val setType: String,
    val weightLbs: Float,
    val reps: Int,
    val distanceMiles: Float?,
    val durationSeconds: Int?,
    val rpe: Float?
)

object HevyCsvParser {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US)

    fun parseCsvContent(csvContent: String): List<HevyWorkoutData> {
        val lines = csvContent.lines()
            .drop(1)
            .filter { it.isNotBlank() }

        // Group rows by session (keyed on start_time)
        val sessionRows = mutableMapOf<String, MutableList<List<String>>>()

        for (line in lines) {
            val fields = parseCsvLine(line)
            if (fields.size < 11) continue
            val startTime = fields[1].trim('"')
            sessionRows.getOrPut(startTime) { mutableListOf() }.add(fields)
        }

        return sessionRows.mapNotNull { (_, rows) ->
            parseSession(rows)
        }.sortedByDescending { it.startTime }
    }

    private fun parseSession(rows: List<List<String>>): HevyWorkoutData? {
        val first = rows.firstOrNull() ?: return null

        val title = first[0].trim('"')
        val startTime = parseDateSafe(first[1].trim('"')) ?: return null
        val endTime = parseDateSafe(first[2].trim('"')) ?: startTime
        val durationMin = ((endTime.time - startTime.time) / 60000).toInt()
            .coerceIn(0, 300) // cap at 5 hours to filter outliers

        val exerciseMap = linkedMapOf<String, MutableList<HevySetData>>()

        for (fields in rows) {
            val exerciseName = fields[4].trim('"')
            if (exerciseName.isBlank()) continue

            val setIndex = fields[7].trim('"').toIntOrNull() ?: 0
            val setType = fields[8].trim('"')
            val weightLbs = fields[9].trim('"').toFloatOrNull() ?: 0f
            val reps = fields[10].trim('"').toIntOrNull() ?: 0
            val distanceMiles = if (fields.size > 11) fields[11].trim('"').toFloatOrNull() else null
            val durationSecs = if (fields.size > 12) fields[12].trim('"').toIntOrNull() else null
            val rpe = if (fields.size > 13) fields[13].trim('"').toFloatOrNull() else null

            exerciseMap.getOrPut(exerciseName) { mutableListOf() }.add(
                HevySetData(
                    setIndex = setIndex,
                    setType = setType,
                    weightLbs = weightLbs,
                    reps = reps,
                    distanceMiles = distanceMiles,
                    durationSeconds = durationSecs,
                    rpe = rpe
                )
            )
        }

        val exercises = exerciseMap.map { (name, sets) ->
            HevyExerciseData(name = name, sets = sets.sortedBy { it.setIndex })
        }

        return HevyWorkoutData(
            title = title,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMin,
            exercises = exercises
        )
    }

    private fun parseDateSafe(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
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
