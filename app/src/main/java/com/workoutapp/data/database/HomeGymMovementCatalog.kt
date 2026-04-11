package com.workoutapp.data.database

/**
 * Hand-curated movement catalog for Home Gym conditioning workouts. This is
 * the user's "finalized movement system" organized into 5 buckets:
 *
 *   1. Cardio — engine work (rower, jump rope, burpees, jumping jacks, high knees)
 *   2. Lower body — strength (DB squats/lunges, TRX squat variants)
 *   3. Upper body — strength (DB push press/row, push-ups, TRX rows/chest press)
 *   4. Core — ab wheel, planks, dead bugs, TRX pike/bodysaw/crunch
 *   5. Conditioning / bodyweight — movement conditioning (mountain climbers,
 *      jump squats, skaters, TRX burpee/sprinter/thruster)
 *
 * Builder rules the generator follows:
 *   - AMRAP (20 min): 1 cardio + 1 strength (lower or upper) + 1 (core or conditioning)
 *   - EMOM 3-station (default): same as AMRAP structure
 *   - EMOM 4-station (balanced): 1 cardio + 1 lower + 1 upper + 1 (core or conditioning)
 *
 * TRX is conceptual — it just fills slots in whichever bucket the movement fits.
 */
object HomeGymMovementCatalog {

    enum class Bucket {
        CARDIO,
        LOWER_BODY,
        UPPER_BODY,
        CORE,
        CONDITIONING_BODYWEIGHT
    }

    /**
     * One entry in the Home Gym catalog. The [id] field is the stable exercise
     * row id — it matches the corresponding row in the `exercises` table after
     * [HomeGymCatalogSeeder] has run.
     */
    data class Movement(
        val id: String,
        val name: String,
        val bucket: Bucket,
        val equipment: String,
        val instructions: String
    )

    val movements: List<Movement> = listOf(
        // ── CARDIO ──────────────────────────────────────────────────────────
        Movement(
            id = "custom_row_200_400m",
            name = "Row (200-400m)",
            bucket = Bucket.CARDIO,
            equipment = "Indoor Rower",
            instructions = "Row 200-400m at a hard but sustainable pace. Drive with the legs, hinge at the hips, pull with the arms last."
        ),
        Movement(
            id = "custom_jump_rope_40_60s",
            name = "Jump Rope (40-60s)",
            bucket = Bucket.CARDIO,
            equipment = "Jump Rope",
            instructions = "Skip at a steady pace for 40-60 seconds. Keep wrists relaxed and land on the balls of your feet."
        ),
        Movement(
            id = "custom_burpees",
            name = "Burpees",
            bucket = Bucket.CARDIO,
            equipment = "Bodyweight",
            instructions = "Drop to a plank, chest to the floor, pop back up and jump. Keep a steady rhythm."
        ),
        Movement(
            id = "custom_jumping_jacks",
            name = "Jumping Jacks",
            bucket = Bucket.CARDIO,
            equipment = "Bodyweight",
            instructions = "Jump feet wide while raising arms overhead, then return. Keep the tempo up."
        ),
        Movement(
            id = "custom_high_knees",
            name = "High Knees",
            bucket = Bucket.CARDIO,
            equipment = "Bodyweight",
            instructions = "Run in place bringing knees to hip height. Stay on the balls of your feet."
        ),

        // ── LOWER BODY STRENGTH ─────────────────────────────────────────────
        Movement(
            id = "custom_heavy_db_goblet_squat",
            name = "Heavy DB Goblet Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at chest height. Squat down keeping chest up and knees tracking over toes. Drive through the heels."
        ),
        Movement(
            id = "custom_heavy_db_lunge",
            name = "Heavy DB Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at your chest or goblet position. Step forward into a lunge, drop the back knee toward the floor, drive back to start."
        ),
        Movement(
            id = "custom_pair_db_squat",
            name = "Pair DB Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at the shoulders or by your sides. Squat down with control, drive up through the heels."
        ),
        Movement(
            id = "custom_pair_db_lunge",
            name = "Pair DB Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at your sides. Step forward into a lunge, drop the back knee toward the floor, drive back to start."
        ),
        Movement(
            id = "custom_trx_squat",
            name = "TRX Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor, hold the handles for balance, squat down with hips back and chest up. Drive through the heels to stand."
        ),
        Movement(
            id = "custom_trx_split_squat",
            name = "TRX Split Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Place one foot in the foot cradle behind you. Lower into a single-leg squat on the front leg. Drive up and repeat."
        ),
        Movement(
            id = "custom_trx_pistol_squat",
            name = "TRX Pistol Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Grip the handles for balance, extend one leg forward, lower into a single-leg squat on the standing leg. Use the straps for balance assist, not to pull yourself up."
        ),

        // ── UPPER BODY STRENGTH ─────────────────────────────────────────────
        Movement(
            id = "custom_heavy_db_push_press",
            name = "Heavy DB Push Press",
            bucket = Bucket.UPPER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at the shoulder. Dip at the knees, drive up, press overhead. Lower under control."
        ),
        Movement(
            id = "custom_heavy_db_row",
            name = "Heavy DB Row",
            bucket = Bucket.UPPER_BODY,
            equipment = "Dumbbell",
            instructions = "Hinge at the hips with one heavy dumbbell. Row the dumbbell to the hip, squeeze the lat, lower under control."
        ),
        Movement(
            id = "custom_pair_db_push_press",
            name = "Pair DB Push Press",
            bucket = Bucket.UPPER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at the shoulders. Dip at the knees, drive up, press both dumbbells overhead. Lower with control."
        ),
        Movement(
            id = "custom_pair_db_row",
            name = "Pair DB Row",
            bucket = Bucket.UPPER_BODY,
            equipment = "Dumbbell",
            instructions = "Hinge at the hips with a dumbbell in each hand. Row both dumbbells to the hips, squeeze the lats, lower under control."
        ),
        Movement(
            id = "custom_pushup",
            name = "Push-Up",
            bucket = Bucket.UPPER_BODY,
            equipment = "Bodyweight",
            instructions = "Hands shoulder-width apart, body in a plank line. Lower the chest to the floor, press back up. Keep the core tight."
        ),
        Movement(
            id = "custom_trx_row",
            name = "TRX Row",
            bucket = Bucket.UPPER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor, lean back with arms extended, pull the chest toward the handles by driving the elbows back. Squeeze the shoulder blades."
        ),
        Movement(
            id = "custom_trx_chest_press",
            name = "TRX Chest Press",
            bucket = Bucket.UPPER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor with arms extended. Lower the chest between the handles, press back up by contracting the chest and triceps."
        ),
        Movement(
            id = "custom_trx_high_row",
            name = "TRX High Row",
            bucket = Bucket.UPPER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor with straps at chest height. Pull the handles toward your face, elbows high and wide. Squeeze the rear delts and mid-back."
        ),

        // ── CORE ────────────────────────────────────────────────────────────
        Movement(
            id = "custom_ab_wheel_rollout",
            name = "Ab Wheel Rollout",
            bucket = Bucket.CORE,
            equipment = "Ab Wheel",
            instructions = "Kneel gripping the ab wheel. Roll forward while keeping the core braced and hips tucked. Pull back to the start without sagging the lower back."
        ),
        Movement(
            id = "custom_plank",
            name = "Plank",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Hold a forearm plank with the body in a straight line. Keep glutes squeezed and core braced. Aim for a quality hold."
        ),
        Movement(
            id = "custom_hollow_hold",
            name = "Hollow Hold",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back, press lower back into the floor, lift arms and legs into a hollow position. Hold."
        ),
        Movement(
            id = "custom_dead_bug",
            name = "Dead Bug",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with arms extended up and knees bent at 90 degrees. Lower opposite arm and leg together while keeping lower back pressed into the floor. Alternate sides."
        ),
        Movement(
            id = "custom_trx_pike",
            name = "TRX Pike",
            bucket = Bucket.CORE,
            equipment = "Suspension Trainer",
            instructions = "Place feet in the foot cradles in a plank. Pike the hips up toward the ceiling while keeping legs straight. Return to plank."
        ),
        Movement(
            id = "custom_trx_body_saw",
            name = "TRX Body Saw",
            bucket = Bucket.CORE,
            equipment = "Suspension Trainer",
            instructions = "Feet in the foot cradles, forearm plank. Push the body forward then pull back, keeping the core braced. The motion is small but intense."
        ),
        Movement(
            id = "custom_trx_crunch",
            name = "TRX Crunch",
            bucket = Bucket.CORE,
            equipment = "Suspension Trainer",
            instructions = "Feet in the foot cradles in a plank. Pull the knees toward the chest by contracting the core. Extend back to plank."
        ),

        // ── CONDITIONING / BODYWEIGHT ───────────────────────────────────────
        Movement(
            id = "custom_mountain_climbers",
            name = "Mountain Climbers",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Start in a plank. Drive knees toward the chest alternating, keeping the hips stable and core braced."
        ),
        Movement(
            id = "custom_jump_squats",
            name = "Jump Squats",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Squat down, explode up into a jump, land softly with knees bent. Reset and repeat."
        ),
        Movement(
            id = "custom_lunges",
            name = "Lunges",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Step forward into a lunge, drop the back knee toward the floor, drive back to start. Alternate legs."
        ),
        Movement(
            id = "custom_skaters",
            name = "Skaters",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Bound laterally from one leg to the other, landing softly on the outside leg. Stay low and athletic."
        ),
        Movement(
            id = "custom_trx_burpee",
            name = "TRX Burpee",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Suspension Trainer",
            instructions = "Place feet in the foot cradles. Drop to a push-up, perform one push-up, pull knees into a pike or chest, then jump back out. Fast and continuous."
        ),
        Movement(
            id = "custom_trx_sprinter_start",
            name = "TRX Sprinter Start",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor, hold the handles in a sprinter start position. Drive the knees forward alternating, leaning into the straps."
        ),
        Movement(
            id = "custom_trx_thruster",
            name = "TRX Thruster",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor in a squat with handles at chest. Squat down, drive up and press the handles out as you stand. Continuous motion."
        )
    )

    fun byBucket(): Map<Bucket, List<Movement>> = movements.groupBy { it.bucket }

    fun byId(id: String): Movement? = movements.firstOrNull { it.id == id }
}
