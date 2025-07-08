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
            data.split(";").map { setString ->
                val parts = setString.split(",")
                Set(
                    reps = parts[0].toInt(),
                    weight = parts[1].toFloat(),
                    completed = parts[2].toBoolean()
                )
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