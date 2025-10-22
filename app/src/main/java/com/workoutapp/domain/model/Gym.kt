package com.workoutapp.domain.model

/**
 * Domain model representing a gym with available equipment.
 *
 * @property id Unique identifier for the gym
 * @property name User-defined name for the gym (e.g., "Home Gym", "24 Hour Fitness", "Work Gym")
 * @property equipmentList List of equipment available at this gym
 * @property isDefault Whether this is the default gym (only one gym can be default)
 * @property createdAt Timestamp when the gym was created
 */
data class Gym(
    val id: Long = 0,
    val name: String,
    val equipmentList: List<String>,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Default gym name used during FTUE migration.
         */
        const val DEFAULT_GYM_NAME = "Home Gym"

        /**
         * Creates a default gym with all equipment available.
         * Used during database migration for existing users.
         */
        fun createDefaultGym(): Gym {
            return Gym(
                name = DEFAULT_GYM_NAME,
                equipmentList = EquipmentType.ALL_EQUIPMENT,
                isDefault = true
            )
        }
    }

    /**
     * Check if this gym has the specified equipment.
     */
    fun hasEquipment(equipment: String): Boolean {
        return equipmentList.any { it.equals(equipment, ignoreCase = true) }
    }

    /**
     * Check if an exercise can be performed at this gym.
     */
    fun canPerformExercise(exerciseEquipment: String): Boolean {
        return EquipmentType.canPerformExercise(exerciseEquipment, equipmentList)
    }
}
