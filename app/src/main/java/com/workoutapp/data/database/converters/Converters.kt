package com.workoutapp.data.database.converters

import androidx.room.TypeConverter
import com.workoutapp.domain.model.Set
import java.util.Date

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class SetListConverter {
    @TypeConverter
    fun fromSetList(sets: List<Set>): String {
        return sets.joinToString(";") { "${it.reps},${it.weight},${it.completed}" }
    }

    @TypeConverter
    fun toSetList(data: String): List<Set> {
        return if (data.isEmpty()) {
            emptyList()
        } else {
            try {
                val sets = data.split(";").mapNotNull { setString ->
                    if (setString.isBlank()) return@mapNotNull null
                    val parts = setString.split(",")
                    if (parts.size >= 3) {
                        Set(
                            reps = parts[0].toIntOrNull() ?: 0,
                            weight = parts[1].toFloatOrNull() ?: 0f,
                            completed = parts[2].toBooleanStrictOrNull() ?: false
                        )
                    } else {
                        null
                    }
                }
                sets
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

class StringListConverter {
    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return strings.joinToString("|")
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isEmpty()) {
            emptyList()
        } else {
            data.split("|")
        }
    }
}