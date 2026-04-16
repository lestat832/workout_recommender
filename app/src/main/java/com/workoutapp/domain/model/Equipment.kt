package com.workoutapp.domain.model

/**
 * Equipment utilities for gym-based exercise filtering.
 * Provides smart matching for equipment variants (e.g., Barbell includes EZ Bar, Squat Bar, etc.)
 */
object EquipmentType {
    // Equipment type constants
    const val BARBELL = "Barbell"
    const val DUMBBELL = "Dumbbell"
    const val CABLE = "Cable"
    const val MACHINE = "Machine"
    const val BODYWEIGHT = "Bodyweight"
    const val BENCH = "Bench"
    const val SMITH_MACHINE = "Smith Machine"
    const val KETTLEBELL = "Kettlebell"
    const val RESISTANCE_BAND = "Resistance Band"
    const val SUSPENSION_TRAINER = "Suspension Trainer"
    const val MEDICINE_BALL = "Medicine Ball"
    const val AB_WHEEL = "Ab Wheel"
    const val INDOOR_ROWER = "Indoor Rower"
    const val INDOOR_BIKE = "Indoor Bike"
    const val JUMP_ROPE = "Jump Rope"
    const val SLIDERS = "Sliders"
    const val PUNCHING_BAG = "Punching Bag"
    const val NONE = "None"
    const val OTHER = "Other"

    // Equipment variant sets for smart matching
    val BARBELL_VARIANTS = setOf(
        "Barbell",
        "Squat Bar",
        "Hex Bar",
        "Trap Bar",
        "EZ Bar",
        "Olympic Barbell",
        "Standard Barbell"
    )

    val DUMBBELL_VARIANTS = setOf(
        "Dumbbell",
        "Dumbbells",
        "Adjustable Dumbbell"
    )

    val CABLE_VARIANTS = setOf(
        "Cable",
        "Cables",
        "Cable Machine",
        "Pulley"
    )

    val MACHINE_VARIANTS = setOf(
        "Machine",
        "Leg Press",
        "Leg Extension",
        "Leg Curl",
        "Chest Press Machine",
        "Shoulder Press Machine",
        "Lat Pulldown",
        "Seated Row Machine",
        "Smith Machine"
    )

    val BODYWEIGHT_VARIANTS = setOf(
        "Bodyweight",
        "Body Only",
        "None",
        "No Equipment"
    )

    val BENCH_VARIANTS = setOf(
        "Bench",
        "Flat Bench",
        "Incline Bench",
        "Decline Bench",
        "Adjustable Bench"
    )

    val KETTLEBELL_VARIANTS = setOf(
        "Kettlebell",
        "Kettlebells"
    )

    val RESISTANCE_BAND_VARIANTS = setOf(
        "Resistance Band",
        "Bands",
        "Elastic Band"
    )

    val SUSPENSION_TRAINER_VARIANTS = setOf(
        "Suspension Trainer",
        "TRX",
        "Suspension Straps"
    )

    val MEDICINE_BALL_VARIANTS = setOf(
        "Medicine Ball",
        "Med Ball",
        "Slam Ball"
    )

    val AB_WHEEL_VARIANTS = setOf(
        "Ab Wheel",
        "Ab Roller",
        "Ab Wheel Roller"
    )

    val INDOOR_ROWER_VARIANTS = setOf(
        "Indoor Rower",
        "Rower",
        "Rowing Machine"
    )

    val INDOOR_BIKE_VARIANTS = setOf(
        "Indoor Bike",
        "Stationary Bike",
        "Exercise Bike",
        "Assault Bike"
    )

    val JUMP_ROPE_VARIANTS = setOf(
        "Jump Rope",
        "Skipping Rope"
    )

    val SLIDERS_VARIANTS = setOf(
        "Sliders",
        "Sliding Discs",
        "Gliding Discs"
    )

    val PUNCHING_BAG_VARIANTS = setOf(
        "Punching Bag",
        "Heavy Bag",
        "Boxing Bag"
    )

    /**
     * All equipment types available for gym setup.
     * Displayed in this order in the UI.
     */
    val ALL_EQUIPMENT = listOf(
        BARBELL,
        DUMBBELL,
        CABLE,
        MACHINE,
        BODYWEIGHT,
        BENCH,
        SMITH_MACHINE,
        KETTLEBELL,
        RESISTANCE_BAND,
        SUSPENSION_TRAINER,
        MEDICINE_BALL,
        AB_WHEEL,
        INDOOR_ROWER,
        INDOOR_BIKE,
        JUMP_ROPE,
        SLIDERS,
        PUNCHING_BAG,
        NONE,
        OTHER
    )

    /**
     * Smart equipment matching logic.
     *
     * Rules:
     * 1. Bodyweight exercises always match (no equipment needed)
     * 2. Exact match (case-insensitive) always passes
     * 3. Variant matching for equipment families (e.g., Barbell includes EZ Bar)
     *
     * @param exerciseEquipment The equipment required by the exercise
     * @param gymEquipment The equipment available at the gym
     * @return true if the gym equipment can be used for the exercise
     */
    fun matches(exerciseEquipment: String, gymEquipment: String): Boolean {
        // Exact match (case-insensitive)
        if (exerciseEquipment.equals(gymEquipment, ignoreCase = true)) return true

        // Variant matching
        return when (gymEquipment) {
            BARBELL -> exerciseEquipment in BARBELL_VARIANTS
            DUMBBELL -> exerciseEquipment in DUMBBELL_VARIANTS
            CABLE -> exerciseEquipment in CABLE_VARIANTS
            MACHINE -> exerciseEquipment in MACHINE_VARIANTS
            BODYWEIGHT -> exerciseEquipment in BODYWEIGHT_VARIANTS
            BENCH -> exerciseEquipment in BENCH_VARIANTS
            KETTLEBELL -> exerciseEquipment in KETTLEBELL_VARIANTS
            RESISTANCE_BAND -> exerciseEquipment in RESISTANCE_BAND_VARIANTS
            SUSPENSION_TRAINER -> exerciseEquipment in SUSPENSION_TRAINER_VARIANTS
            MEDICINE_BALL -> exerciseEquipment in MEDICINE_BALL_VARIANTS
            AB_WHEEL -> exerciseEquipment in AB_WHEEL_VARIANTS
            INDOOR_ROWER -> exerciseEquipment in INDOOR_ROWER_VARIANTS
            INDOOR_BIKE -> exerciseEquipment in INDOOR_BIKE_VARIANTS
            JUMP_ROPE -> exerciseEquipment in JUMP_ROPE_VARIANTS
            SLIDERS -> exerciseEquipment in SLIDERS_VARIANTS
            PUNCHING_BAG -> exerciseEquipment in PUNCHING_BAG_VARIANTS
            SMITH_MACHINE -> exerciseEquipment == "Smith Machine"
            NONE -> exerciseEquipment in BODYWEIGHT_VARIANTS
            OTHER -> false // "Other" doesn't match anything by default
            else -> false
        }
    }

    /**
     * Check if an exercise can be performed with available gym equipment.
     *
     * @param exerciseEquipment The equipment required by the exercise
     * @param availableEquipment List of equipment available at the gym
     * @return true if the exercise can be performed with available equipment
     */
    fun canPerformExercise(exerciseEquipment: String, availableEquipment: List<String>): Boolean {
        // Check if any gym equipment matches the exercise requirement
        return availableEquipment.any { gymEquipment ->
            matches(exerciseEquipment, gymEquipment)
        }
    }
}
