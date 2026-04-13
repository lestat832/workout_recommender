package com.workoutapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.model.GymWorkoutStyle

/**
 * Room entity representing a gym in the database.
 *
 * @property id Auto-generated unique identifier
 * @property name User-defined gym name
 * @property equipmentList Comma-separated list of equipment (stored as String for Room)
 * @property isDefault Whether this is the default gym (only one should be true)
 * @property createdAt Timestamp when the gym was created
 * @property workoutStyle Whether this gym produces strength or conditioning workouts
 */
@Entity(tableName = "gyms")
data class GymEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val equipmentList: String, // Stored as comma-separated string
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val workoutStyle: GymWorkoutStyle = GymWorkoutStyle.STRENGTH
) {
    /**
     * Convert database entity to domain model.
     */
    fun toDomain(): Gym {
        return Gym(
            id = id,
            name = name,
            equipmentList = equipmentList.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            isDefault = isDefault,
            createdAt = createdAt,
            workoutStyle = workoutStyle
        )
    }

    companion object {
        /**
         * Convert domain model to database entity.
         */
        fun fromDomain(gym: Gym): GymEntity {
            return GymEntity(
                id = gym.id,
                name = gym.name,
                equipmentList = gym.equipmentList.joinToString(","),
                isDefault = gym.isDefault,
                createdAt = gym.createdAt,
                workoutStyle = gym.workoutStyle
            )
        }
    }
}
