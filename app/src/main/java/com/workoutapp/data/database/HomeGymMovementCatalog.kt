package com.workoutapp.data.database

/**
 * Hand-curated movement catalog for Home Gym conditioning workouts. Organized
 * into 6 buckets that encode the sequencing rule **legs → pull → push → core**:
 *
 *   1. Cardio — engine work (rower, jump rope, burpees, jumping jacks, high knees)
 *   2. Lower body — strength (DB squats/lunges, TRX squat variants)
 *   3. Upper pull — horizontal/vertical pulling (DB rows, TRX row variants)
 *   4. Upper push — horizontal/vertical pressing (DB push press, push-ups, TRX chest press)
 *   5. Core — ab wheel, planks, dead bugs, TRX pike/bodysaw/crunch
 *   6. Conditioning / bodyweight — movement conditioning (mountain climbers,
 *      jump squats, skaters, TRX burpee/sprinter/thruster)
 *
 * Builder rules the generator follows:
 *   - EMOM (20 min, always 4-station): lower + pull + push + (core ∪ conditioning).
 *     Deterministic order — this is the "structured strength" format. No cardio.
 *   - AMRAP (20 min, always 3-station): cardio + strength (lower ∪ pull ∪ push)
 *     + (core ∪ conditioning). This is the "loose metcon" — cardio-first by design.
 *
 * TRX is conceptual — it just fills slots in whichever bucket the movement fits.
 */
object HomeGymMovementCatalog {

    enum class Bucket {
        CARDIO,
        LOWER_BODY,
        UPPER_PULL,
        UPPER_PUSH,
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
            name = "Row",
            bucket = Bucket.CARDIO,
            equipment = "Indoor Rower",
            instructions = "Row at a hard but sustainable pace. Drive with the legs, hinge at the hips, pull with the arms last."
        ),
        Movement(
            id = "custom_jump_rope_40_60s",
            name = "Jump Rope",
            bucket = Bucket.CARDIO,
            equipment = "Jump Rope",
            instructions = "Skip at a steady pace. Keep wrists relaxed and land on the balls of your feet."
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
        Movement(
            id = "custom_bag_rounds",
            name = "Bag Rounds",
            bucket = Bucket.CARDIO,
            equipment = "Punching Bag",
            instructions = "Work the bag at a steady pace mixing jabs, crosses, hooks, and uppercuts. Stay on the balls of your feet and keep your hands up between combos. Maintain rhythm over power."
        ),
        Movement(
            id = "custom_bag_combos",
            name = "Bag Combos",
            bucket = Bucket.CARDIO,
            equipment = "Punching Bag",
            instructions = "Throw structured punch combinations: 1-2 (jab-cross), 1-2-3 (jab-cross-hook), 1-2-5-2 (jab-cross-uppercut-cross). Reset stance between combos. Focus on clean technique and snap."
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
            name = "Heavy DB Reverse Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at your chest in a goblet position. Step backward into a reverse lunge, drop the back knee toward the floor, drive through the front heel to return to start. Alternate legs."
        ),
        Movement(
            id = "custom_pair_db_squat",
            name = "Pair DB Front Squat",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand racked at the shoulders in a front rack position. Keep elbows high and chest tall. Squat down with control, drive up through the heels."
        ),
        Movement(
            id = "custom_pair_db_lunge",
            name = "Pair DB Reverse Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at your sides. Step backward into a reverse lunge, drop the back knee toward the floor, drive through the front heel to return to start. Alternate legs."
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
        Movement(
            id = "custom_heavy_db_deadlift",
            name = "Heavy DB Deadlift",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Stand with one heavy dumbbell between the feet. Hinge at the hips with a flat back, grip the dumbbell, and stand up by driving the hips forward. Lower with control by pushing the hips back."
        ),
        Movement(
            id = "custom_pair_db_rdl",
            name = "Pair DB RDL",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Stand holding a dumbbell in each hand in front of the thighs. Hinge at the hips with a slight knee bend, lowering the dumbbells along the shins. Feel the hamstrings stretch, then drive the hips forward to return to standing."
        ),
        Movement(
            id = "custom_trx_side_lunge",
            name = "TRX Side Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor and grip the handles for balance. Step out wide to one side, lowering into a lateral lunge with the stepping leg bent and the other leg straight. Drive back to center and alternate sides."
        ),
        Movement(
            id = "custom_trx_hamstring_curl",
            name = "TRX Hamstring Curl",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Lie on your back with heels in the foot cradles, arms flat on the floor. Press the hips up into a bridge, then curl the heels toward the glutes by contracting the hamstrings. Extend back out under control."
        ),
        Movement(
            id = "custom_trx_hip_press",
            name = "TRX Hip Press",
            bucket = Bucket.LOWER_BODY,
            equipment = "Suspension Trainer",
            instructions = "Lie on your back with heels in the foot cradles, knees bent. Drive the hips up toward the ceiling by squeezing the glutes, then lower under control. Keep the core braced throughout."
        ),
        Movement(
            id = "custom_heavy_db_forward_lunge",
            name = "Heavy DB Forward Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at your chest in a goblet position. Step forward into a lunge, drop the back knee toward the floor, drive through the front heel to push back to start. Alternate legs."
        ),
        Movement(
            id = "custom_pair_db_forward_lunge",
            name = "Pair DB Forward Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at your sides. Step forward into a lunge, drop the back knee toward the floor, drive through the front heel to push back to start. Alternate legs."
        ),
        Movement(
            id = "custom_slider_reverse_lunge",
            name = "Slider Reverse Lunge",
            bucket = Bucket.LOWER_BODY,
            equipment = "Sliders",
            instructions = "Stand with one foot on a slider. Slide the foot back into a reverse lunge, dropping the back knee toward the floor. Drive through the front heel to return to standing. Alternate legs."
        ),
        Movement(
            id = "custom_slider_hamstring_curl",
            name = "Slider Hamstring Curl",
            bucket = Bucket.LOWER_BODY,
            equipment = "Sliders",
            instructions = "Lie on your back with both heels on sliders, arms flat on the floor. Press the hips up into a bridge, then curl the heels toward the glutes by contracting the hamstrings. Extend back out under control."
        ),
        Movement(
            id = "custom_kb_swing",
            name = "KB Swing",
            bucket = Bucket.LOWER_BODY,
            equipment = "Kettlebell",
            instructions = "Stand with the kettlebell a foot in front of you. Hinge at the hips and grip the bell with both hands. Hike it back between the legs, then drive the hips forward explosively to swing the bell to chest height. Let it fall back between the legs and repeat."
        ),
        Movement(
            id = "custom_kb_single_leg_deadlift",
            name = "KB Single-Leg Deadlift",
            bucket = Bucket.LOWER_BODY,
            equipment = "Kettlebell",
            instructions = "Hold the kettlebell in one hand. Shift weight to the opposite leg. Hinge at the hips while extending the free leg straight back behind you, keeping a flat back. Lower the bell toward the floor until you feel a hamstring stretch. Drive the hips forward to return. Alternate sides."
        ),

        // ── UPPER BODY PULL ─────────────────────────────────────────────────
        Movement(
            id = "custom_pull_up",
            name = "Pull-Up",
            bucket = Bucket.UPPER_PULL,
            equipment = "Pull-Up Bar",
            instructions = "Grip the bar overhand just wider than shoulder width. Hang at full extension, then pull your chest toward the bar by driving the elbows down and back. Lower under control to a dead hang."
        ),
        Movement(
            id = "custom_heavy_db_row",
            name = "Heavy DB Row",
            bucket = Bucket.UPPER_PULL,
            equipment = "Dumbbell",
            instructions = "Hinge at the hips with one heavy dumbbell. Row the dumbbell to the hip, squeeze the lat, lower under control."
        ),
        Movement(
            id = "custom_trx_row",
            name = "TRX Row",
            bucket = Bucket.UPPER_PULL,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor, lean back with arms extended, pull the chest toward the handles by driving the elbows back. Squeeze the shoulder blades."
        ),
        Movement(
            id = "custom_trx_high_row",
            name = "TRX High Row",
            bucket = Bucket.UPPER_PULL,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor with straps at chest height. Pull the handles toward your face, elbows high and wide. Squeeze the rear delts and mid-back."
        ),
        Movement(
            id = "custom_trx_single_arm_row",
            name = "TRX Single-Arm Row",
            bucket = Bucket.UPPER_PULL,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor gripping one handle with one hand. Lean back with the working arm extended. Pull your chest toward the handle by driving the elbow back, rotating slightly at the top. Lower under control and alternate sides."
        ),
        Movement(
            id = "custom_trx_y_fly",
            name = "TRX Y Fly",
            bucket = Bucket.UPPER_PULL,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor with arms extended out front at chest height. Lean back. Raise both arms out and overhead into a Y shape, squeezing the rear delts and upper traps. Return with control."
        ),
        Movement(
            id = "custom_trx_power_pull",
            name = "TRX Power Pull",
            bucket = Bucket.UPPER_PULL,
            equipment = "Suspension Trainer",
            instructions = "Grip one handle with one hand, the other hand reaching back behind you. Lean back with core braced. Pull yourself up toward the anchor with the working arm while rotating the free arm forward. Lower under control. Alternate sides."
        ),

        // ── UPPER BODY PUSH ─────────────────────────────────────────────────
        Movement(
            id = "custom_atomic_pushup_trx",
            name = "Atomic Push-Up (TRX)",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Suspension Trainer",
            instructions = "Place your feet in the foot cradles in a plank position. Perform a push-up. At the top, pull your knees toward your chest into a tuck. Extend the legs back to plank and repeat."
        ),
        Movement(
            id = "custom_atomic_pushup_sliders",
            name = "Atomic Push-Up (Sliders)",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Sliders",
            instructions = "Start in a plank with both feet on sliders. Perform a push-up. At the top, slide your knees toward your chest into a tuck. Slide the legs back to plank and repeat. Keep the core braced throughout."
        ),
        Movement(
            id = "custom_heavy_db_push_press",
            name = "Heavy DB Push Press",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Dumbbell",
            instructions = "Hold one heavy dumbbell at the shoulder. Dip at the knees, drive up, press overhead. Lower under control."
        ),
        Movement(
            id = "custom_pair_db_push_press",
            name = "Pair DB Push Press",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Dumbbell",
            instructions = "Hold a dumbbell in each hand at the shoulders. Dip at the knees, drive up, press both dumbbells overhead. Lower with control."
        ),
        Movement(
            id = "custom_pushup",
            name = "Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Hands shoulder-width apart, body in a plank line. Lower the chest to the floor, press back up. Keep the core tight."
        ),
        Movement(
            id = "custom_trx_chest_press",
            name = "TRX Chest Press",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor with arms extended. Lower the chest between the handles, press back up by contracting the chest and triceps."
        ),
        Movement(
            id = "custom_trx_t_pushup",
            name = "TRX T Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor with hands in the handles in a plank. Perform a push-up. At the top, rotate the torso to one side, extending the free arm overhead into a T position. Return to plank and repeat on the other side."
        ),
        Movement(
            id = "custom_trx_spiderman_pushup",
            name = "TRX Spiderman Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor with hands in the handles in a plank. As you lower into a push-up, drive one knee up toward the same-side elbow. Press back up and alternate sides with each rep."
        ),
        Movement(
            id = "custom_trx_clap_pushup",
            name = "TRX Clap Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Suspension Trainer",
            instructions = "Face away from the anchor with hands in the handles in a plank. Perform an explosive push-up driving the body up fast enough to clap the hands together briefly at the top before catching and lowering under control."
        ),
        Movement(
            id = "custom_clap_pushup",
            name = "Clap Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Start in a push-up position with hands slightly wider than shoulders. Lower under control, then press up explosively so the hands leave the ground. Clap once at the top, land with soft elbows, and absorb back into the next rep."
        ),
        Movement(
            id = "custom_decline_pushup",
            name = "Decline Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Place your feet on an elevated surface — a box, bench, chair, or couch — with hands on the floor in a plank. Lower the chest toward the floor, press back up. The elevated feet shift emphasis to the upper chest and front delts."
        ),
        Movement(
            id = "custom_diamond_pushup",
            name = "Diamond Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Place the hands under the chest with thumbs and index fingers touching to form a diamond. Lower the chest toward the hands, keeping elbows tucked. Press back up. Emphasizes triceps."
        ),
        Movement(
            id = "custom_spiderman_pushup",
            name = "Spiderman Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Start in a push-up position. As you lower into the push-up, drive one knee up toward the same-side elbow. Press back up and alternate sides with each rep."
        ),
        Movement(
            id = "custom_single_leg_pushup",
            name = "Single-Leg Push-Up",
            bucket = Bucket.UPPER_PUSH,
            equipment = "Bodyweight",
            instructions = "Start in a push-up position and lift one leg off the floor, keeping it straight and hips square. Perform a push-up. Complete the set, then switch legs."
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
        Movement(
            id = "custom_side_plank",
            name = "Side Plank",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your side with legs stacked. Prop up on the forearm, elbow under shoulder. Lift the hips so the body forms a straight line from shoulders to feet. Hold 30 seconds each side."
        ),
        Movement(
            id = "custom_trx_oblique_crunch",
            name = "TRX Oblique Crunch",
            bucket = Bucket.CORE,
            equipment = "Suspension Trainer",
            instructions = "Feet in the foot cradles in a plank. Pull the knees toward one elbow, contracting the obliques. Extend back to plank and alternate sides."
        ),
        Movement(
            id = "custom_slider_pike",
            name = "Slider Pike",
            bucket = Bucket.CORE,
            equipment = "Sliders",
            instructions = "Start in a plank with both feet on sliders. Pike the hips up toward the ceiling while keeping legs straight, sliding the feet toward the hands. Return to plank under control."
        ),
        Movement(
            id = "custom_slider_body_saw",
            name = "Slider Body Saw",
            bucket = Bucket.CORE,
            equipment = "Sliders",
            instructions = "Start in a forearm plank with both feet on sliders. Push the body forward by extending the shoulders, then pull back to the start. Keep the core braced throughout. The motion is small but intense."
        ),
        Movement(
            id = "custom_kb_windmill",
            name = "KB Windmill",
            bucket = Bucket.CORE,
            equipment = "Kettlebell",
            instructions = "Press the kettlebell overhead with one arm. Turn the feet at a 45-degree angle away from the loaded arm. Keeping the bell locked out overhead, push the hips out and bend sideways at the waist, reaching the free hand toward the opposite foot. Return to standing under control. Alternate sides."
        ),
        Movement(
            id = "custom_med_ball_russian_twist",
            name = "Medicine Ball Russian Twist",
            bucket = Bucket.CORE,
            equipment = "Medicine Ball",
            instructions = "Sit on the floor with knees bent and feet lifted. Hold the medicine ball at chest height with both hands. Rotate the torso to tap the ball to one side of the hips, then the other. Keep the core engaged throughout."
        ),
        Movement(
            id = "custom_med_ball_woodchopper",
            name = "Medicine Ball Woodchopper",
            bucket = Bucket.CORE,
            equipment = "Medicine Ball",
            instructions = "Stand with feet shoulder-width apart, holding the ball overhead on one side. Rotate and bring the ball down diagonally across the body to the opposite hip in a chopping motion. Keep the arms long. Return to start under control. Alternate sides."
        ),
        Movement(
            id = "custom_med_ball_sit_up",
            name = "Medicine Ball Sit-Up",
            bucket = Bucket.CORE,
            equipment = "Medicine Ball",
            instructions = "Lie on your back with knees bent, holding the medicine ball at your chest. Sit up while pressing the ball overhead. Lower back down under control. Keep the core braced throughout."
        ),
        Movement(
            id = "custom_bicycle_crunch",
            name = "Bicycle Crunch",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with hands behind the head and knees bent. Lift the shoulders off the floor. Bring one elbow toward the opposite knee while extending the other leg. Alternate in a pedaling motion."
        ),
        Movement(
            id = "custom_scissor_kicks",
            name = "Scissor Kicks",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with hands under the glutes, legs extended and lifted a few inches off the floor. Cross one leg over the other in a scissoring motion, alternating continuously. Keep the lower back pressed into the floor."
        ),
        Movement(
            id = "custom_flutter_kicks",
            name = "Flutter Kicks",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with hands under the glutes, legs extended and lifted a few inches off the floor. Flutter the legs up and down in small, alternating movements. Keep the lower back pressed into the floor."
        ),
        Movement(
            id = "custom_leg_raises",
            name = "Leg Raises",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with legs extended and hands at your sides or under the glutes. Keeping the legs straight, lift them toward the ceiling until the hips are slightly off the floor. Lower under control without letting the heels touch the floor."
        ),
        Movement(
            id = "custom_suitcase_crunch",
            name = "Suitcase Crunch",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with arms extended overhead and legs straight. Simultaneously lift the torso and bend the knees toward the chest, bringing the hands to meet the shins. Lower back to the start under control."
        ),
        Movement(
            id = "custom_v_sit",
            name = "V-Sit",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with legs extended and arms overhead. Simultaneously lift the legs and torso into a V-shape, reaching the hands toward the feet. Lower back to the start under control. Keep the core engaged throughout."
        ),
        Movement(
            id = "custom_windshield_wipers",
            name = "Windshield Wipers",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with arms extended out to the sides for stability and legs lifted straight up toward the ceiling. Rotate the legs together to one side, stopping just before they touch the floor. Rotate back through center to the other side."
        ),
        Movement(
            id = "custom_elevated_knee_crunch",
            name = "Elevated Knee Crunch",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Lie on your back with knees bent at 90 degrees and shins parallel to the floor in a tabletop position. Crunch the shoulders up toward the knees, contracting the upper abs. Lower with control without letting the feet drop."
        ),
        Movement(
            id = "custom_russian_twist",
            name = "Russian Twist",
            bucket = Bucket.CORE,
            equipment = "Bodyweight",
            instructions = "Sit on the floor with knees bent and feet lifted. Lean back slightly and clasp the hands together at chest height. Rotate the torso to tap the hands to one side of the hips, then the other. Keep the core engaged throughout."
        ),

        // ── CONDITIONING / BODYWEIGHT ───────────────────────────────────────
        Movement(
            id = "custom_slider_mountain_climber",
            name = "Slider Mountain Climber",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Sliders",
            instructions = "Start in a plank with both feet on sliders. Drive the knees alternately toward the chest, sliding the feet along the floor. Keep the hips stable and core braced. The sliders make the movement smoother and more core-intensive."
        ),
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
            id = "custom_squat_thrusts",
            name = "Squat Thrusts",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Stand tall, squat down and plant the hands on the floor. Kick the feet back into a plank. Hop the feet forward to the hands and stand back up. No push-up, no jump — continuous rhythm."
        ),
        Movement(
            id = "custom_trx_mountain_climber",
            name = "TRX Mountain Climber",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Suspension Trainer",
            instructions = "Place both feet in the foot cradles in a plank position. Drive the knees alternately toward the chest at a fast pace, keeping the hips stable and core braced."
        ),
        Movement(
            id = "custom_trx_squat_row",
            name = "TRX Squat Row",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Suspension Trainer",
            instructions = "Face the anchor holding the handles. Squat down while extending the arms forward. As you drive up out of the squat, simultaneously row the handles to the ribs. One continuous motion."
        ),
        Movement(
            id = "custom_reverse_lunges",
            name = "Reverse Lunges",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Bodyweight",
            instructions = "Stand tall with hands on hips or at the sides. Step backward into a reverse lunge, dropping the back knee toward the floor. Drive through the front heel to return to standing. Alternate legs."
        ),
        Movement(
            id = "custom_kb_clean",
            name = "KB Clean",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Kettlebell",
            instructions = "Start with the kettlebell on the floor between your feet. Hinge and grip it with one hand. Drive the hips forward to pull the bell up along the body, rotating the wrist as it reaches the shoulder to catch it in the front rack. Lower and repeat. Alternate sides."
        ),
        Movement(
            id = "custom_kb_snatch",
            name = "KB Snatch",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Kettlebell",
            instructions = "Start with the kettlebell on the floor between your feet. Hinge and grip it with one hand. Drive the hips forward and pull the bell up in one motion, punching through overhead so it rotates around the wrist into a locked-out position above. Lower and repeat. Alternate sides."
        ),
        Movement(
            id = "custom_wall_ball_squats",
            name = "Wall Ball Squats",
            bucket = Bucket.CONDITIONING_BODYWEIGHT,
            equipment = "Medicine Ball",
            instructions = "Hold the medicine ball at chest in a front rack position. Squat down under control. Stand explosively and toss the ball straight up overhead. Catch the ball as you descend into the next squat. Continuous rhythm."
        )
    )

    fun byBucket(): Map<Bucket, List<Movement>> = movements.groupBy { it.bucket }

    fun byId(id: String): Movement? = movements.firstOrNull { it.id == id }
}
