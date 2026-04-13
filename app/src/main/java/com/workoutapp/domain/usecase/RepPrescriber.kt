package com.workoutapp.domain.usecase

import com.workoutapp.domain.model.WorkoutFormat

/**
 * Stateless rep prescriber for Home Gym conditioning workouts. Given a
 * movement id, the session format (EMOM/AMRAP), and the ordered list of
 * station ids in the session, returns a display string describing the
 * prescribed work for that station.
 *
 * Implements a tier-based coaching model from a 10x-trainer perspective:
 *
 *  1. Every Home Gym catalog movement is classified into an intensity tier
 *     (T1 heavy compound → T7 cardio time) based on loading, complexity,
 *     and recovery demand. Unilateral movements are tagged so the display
 *     shows per-side counts.
 *
 *  2. Each tier has a base rep/time range per format. EMOM ranges are
 *     higher because each 60s station has built-in rest at the end of the
 *     window; AMRAP ranges are lower to preserve round pace in a 15-minute
 *     circuit.
 *
 *  3. Session context applies small modifiers. The primary rule today:
 *     if an EMOM closer slot is a leg-dominant movement and the legs slot
 *     already loaded the lower body, reduce closer reps by 20% to respect
 *     the "don't stack the same pattern twice" guideline.
 *
 *  4. Cardio movements with intrinsic distance/time targets (row, jump
 *     rope) use hand-tuned special-case displays instead of the generic
 *     tier rubric.
 *
 * This is a pure function — no state, no repository access. Safe to call
 * from the UI layer directly during render.
 */
object RepPrescriber {

    enum class Tier { T1, T2, T3, T4, T5, T6, T7 }

    private data class Classification(
        val tier: Tier,
        val unilateral: Boolean = false
    )

    /**
     * Tier classification for all 63 Home Gym catalog movements, keyed by
     * stable exercise id. Any id missing from this map returns "—" from
     * [prescribe], which surfaces the gap in UI review.
     */
    private val classifications: Map<String, Classification> = mapOf(
        // ── CARDIO (5) — all T7 ─────────────────────────────────────────
        "custom_row_200_400m" to Classification(Tier.T7),
        "custom_jump_rope_40_60s" to Classification(Tier.T7),
        "custom_burpees" to Classification(Tier.T7),
        "custom_jumping_jacks" to Classification(Tier.T7),
        "custom_high_knees" to Classification(Tier.T7),

        // ── LOWER BODY (16) ─────────────────────────────────────────────
        "custom_heavy_db_goblet_squat" to Classification(Tier.T1),
        "custom_heavy_db_lunge" to Classification(Tier.T1, unilateral = true),
        "custom_pair_db_squat" to Classification(Tier.T2),
        "custom_pair_db_lunge" to Classification(Tier.T2, unilateral = true),
        "custom_trx_squat" to Classification(Tier.T3),
        "custom_trx_split_squat" to Classification(Tier.T3, unilateral = true),
        "custom_trx_pistol_squat" to Classification(Tier.T3, unilateral = true),
        "custom_heavy_db_deadlift" to Classification(Tier.T1),
        "custom_pair_db_rdl" to Classification(Tier.T2),
        "custom_trx_side_lunge" to Classification(Tier.T3, unilateral = true),
        "custom_trx_hamstring_curl" to Classification(Tier.T3),
        "custom_trx_hip_press" to Classification(Tier.T3),
        "custom_heavy_db_forward_lunge" to Classification(Tier.T1, unilateral = true),
        "custom_pair_db_forward_lunge" to Classification(Tier.T2, unilateral = true),
        "custom_kb_swing" to Classification(Tier.T2),
        "custom_kb_single_leg_deadlift" to Classification(Tier.T2, unilateral = true),

        // ── UPPER PULL (7) ──────────────────────────────────────────────
        "custom_pull_up" to Classification(Tier.T3),
        "custom_heavy_db_row" to Classification(Tier.T1, unilateral = true),
        "custom_trx_row" to Classification(Tier.T3),
        "custom_trx_high_row" to Classification(Tier.T3),
        "custom_trx_single_arm_row" to Classification(Tier.T3, unilateral = true),
        "custom_trx_y_fly" to Classification(Tier.T3),
        "custom_trx_power_pull" to Classification(Tier.T3, unilateral = true),

        // ── UPPER PUSH (9) ──────────────────────────────────────────────
        "custom_atomic_pushup_trx" to Classification(Tier.T3),
        "custom_atomic_pushup_sliders" to Classification(Tier.T3),
        "custom_heavy_db_push_press" to Classification(Tier.T1, unilateral = true),
        "custom_pair_db_push_press" to Classification(Tier.T2),
        "custom_pushup" to Classification(Tier.T3),
        "custom_trx_chest_press" to Classification(Tier.T3),
        "custom_trx_t_pushup" to Classification(Tier.T3, unilateral = true),
        "custom_trx_spiderman_pushup" to Classification(Tier.T3),
        "custom_trx_clap_pushup" to Classification(Tier.T3),

        // ── CORE (13) ───────────────────────────────────────────────────
        "custom_ab_wheel_rollout" to Classification(Tier.T6),
        "custom_plank" to Classification(Tier.T5),
        "custom_hollow_hold" to Classification(Tier.T5),
        "custom_dead_bug" to Classification(Tier.T6),
        "custom_trx_pike" to Classification(Tier.T6),
        "custom_trx_body_saw" to Classification(Tier.T6),
        "custom_trx_crunch" to Classification(Tier.T6),
        "custom_side_plank" to Classification(Tier.T5, unilateral = true),
        "custom_trx_oblique_crunch" to Classification(Tier.T6, unilateral = true),
        "custom_kb_windmill" to Classification(Tier.T6, unilateral = true),
        "custom_med_ball_russian_twist" to Classification(Tier.T6),
        "custom_med_ball_woodchopper" to Classification(Tier.T6, unilateral = true),
        "custom_med_ball_sit_up" to Classification(Tier.T6),

        // ── CONDITIONING / BODYWEIGHT (13) ──────────────────────────────
        "custom_mountain_climbers" to Classification(Tier.T4),
        "custom_jump_squats" to Classification(Tier.T4),
        "custom_lunges" to Classification(Tier.T4),
        "custom_skaters" to Classification(Tier.T4),
        "custom_trx_burpee" to Classification(Tier.T4),
        "custom_trx_sprinter_start" to Classification(Tier.T4),
        "custom_squat_thrusts" to Classification(Tier.T4),
        "custom_trx_mountain_climber" to Classification(Tier.T4),
        "custom_trx_squat_row" to Classification(Tier.T4),
        "custom_reverse_lunges" to Classification(Tier.T4),
        "custom_kb_clean" to Classification(Tier.T2, unilateral = true),
        "custom_kb_snatch" to Classification(Tier.T2, unilateral = true),
        "custom_wall_ball_squats" to Classification(Tier.T4)
    )

    /**
     * Cardio movements with intrinsic distance/time/rep targets use hand-tuned
     * displays instead of the generic T7 rubric. EMOM variants are longer
     * (more time inside the 60s window to rest after); AMRAP variants are
     * shorter (keeps round pace under ~45s per station). Burpees break the
     * T7 "time-based" assumption because they're naturally counted, not
     * clocked — override them to rep targets to match how a trainer would
     * actually cue them.
     */
    private val specialDisplays: Map<String, Map<WorkoutFormat, String>> = mapOf(
        "custom_row_200_400m" to mapOf(
            WorkoutFormat.EMOM to "300m row",
            WorkoutFormat.AMRAP to "200m row"
        ),
        "custom_jump_rope_40_60s" to mapOf(
            WorkoutFormat.EMOM to "× 80-100",
            WorkoutFormat.AMRAP to "× 60-80"
        ),
        "custom_burpees" to mapOf(
            WorkoutFormat.EMOM to "× 8-10",
            WorkoutFormat.AMRAP to "× 6-8"
        ),
        "custom_jumping_jacks" to mapOf(
            WorkoutFormat.EMOM to "× 30-40",
            WorkoutFormat.AMRAP to "× 20-30"
        ),
        // High knees alternates feet, so reps are counted per foot tap like
        // the mountain climber family. Numbers are higher because the tempo
        // is faster than mountain climbers (foot barely leaves the floor).
        "custom_high_knees" to mapOf(
            WorkoutFormat.EMOM to "× 60-80 total",
            WorkoutFormat.AMRAP to "× 40-60 total"
        ),
        // Pull-ups break the T3 "bodyweight strength" rubric because vertical
        // pulling has a much steeper training-age gate than push-ups or
        // horizontal rows. Prescription is anchored to Marc's demonstrated
        // EMOM performance (6 reps × 5 rounds sustained). AMRAP reduces
        // per-round volume to ~60-75% of EMOM rate to account for the lack
        // of between-round rest — 6 × 0.6-0.75 ≈ 3-5 per round.
        "custom_pull_up" to mapOf(
            WorkoutFormat.EMOM to "× 5-6",
            WorkoutFormat.AMRAP to "× 3-5"
        ),
        // Alternating-leg family: 1 rep = 1 knee drive (standard CrossFit
        // convention). T4's default 12-15 target was calibrated for
        // symmetric movements (jump squats, burpees) and ends up too light
        // for these — bump to higher total-count prescriptions.
        "custom_mountain_climbers" to mapOf(
            WorkoutFormat.EMOM to "× 40-50 total",
            WorkoutFormat.AMRAP to "× 30-40 total"
        ),
        "custom_trx_mountain_climber" to mapOf(
            WorkoutFormat.EMOM to "× 40-50 total",
            WorkoutFormat.AMRAP to "× 30-40 total"
        ),
        "custom_trx_sprinter_start" to mapOf(
            WorkoutFormat.EMOM to "× 30-40 total",
            WorkoutFormat.AMRAP to "× 20-30 total"
        )
    )

    /**
     * Closer-slot ids that count as "leg-dominant" for pattern-stacking
     * purposes. When one of these lands in an EMOM closer slot, the reps
     * are trimmed 20% because the legs slot already loaded the pattern.
     * (AMRAP doesn't get this modifier — its loose structure means the
     * "strength" slot could be anything, not necessarily legs.)
     */
    private val legDominantClosers: Set<String> = setOf(
        "custom_jump_squats",
        "custom_lunges",
        "custom_reverse_lunges",
        "custom_skaters",
        "custom_squat_thrusts",
        "custom_wall_ball_squats",
        "custom_kb_clean",
        "custom_kb_snatch"
    )

    /**
     * Prescribe the work target for one station given the workout format
     * and the ordered session ids. Returns a display string like "× 8-10"
     * or "30 sec/side" or "200m row".
     */
    fun prescribe(
        exerciseId: String,
        format: WorkoutFormat,
        sessionIds: List<String>
    ): String {
        specialDisplays[exerciseId]?.get(format)?.let { return it }

        val classification = classifications[exerciseId] ?: return "—"
        val base = baseRange(classification.tier, format, classification.unilateral)

        val isCloserSlot = sessionIds.indexOf(exerciseId) == sessionIds.lastIndex
        val modifier = if (
            format == WorkoutFormat.EMOM &&
            isCloserSlot &&
            exerciseId in legDominantClosers
        ) 0.8 else 1.0

        val adjusted = base.scale(modifier)
        return adjusted.format(classification.unilateral)
    }

    // ── internals ───────────────────────────────────────────────────────

    private data class Range(val min: Int, val max: Int, val unit: Unit) {
        enum class Unit { REPS, SECONDS }

        fun scale(m: Double): Range = copy(
            min = (min * m).toInt().coerceAtLeast(1),
            max = (max * m).toInt().coerceAtLeast(1)
        )

        fun format(unilateral: Boolean): String {
            val range = if (min == max) "$min" else "$min-$max"
            val sideSuffix = if (unilateral) "/side" else ""
            return when (unit) {
                Unit.REPS -> "× $range$sideSuffix"
                Unit.SECONDS -> "$range sec$sideSuffix"
            }
        }
    }

    private fun baseRange(tier: Tier, format: WorkoutFormat, unilateral: Boolean): Range =
        when (tier) {
            Tier.T1 -> when (format) {
                WorkoutFormat.EMOM -> if (unilateral) reps(5, 6) else reps(6, 8)
                WorkoutFormat.AMRAP -> if (unilateral) reps(4, 5) else reps(5, 6)
                else -> reps(6, 8)
            }
            Tier.T2 -> when (format) {
                WorkoutFormat.EMOM -> if (unilateral) reps(6, 8) else reps(8, 10)
                WorkoutFormat.AMRAP -> if (unilateral) reps(5, 6) else reps(6, 8)
                else -> reps(8, 10)
            }
            Tier.T3 -> when (format) {
                WorkoutFormat.EMOM -> if (unilateral) reps(6, 8) else reps(10, 15)
                WorkoutFormat.AMRAP -> if (unilateral) reps(5, 6) else reps(8, 12)
                else -> reps(10, 15)
            }
            Tier.T4 -> when (format) {
                WorkoutFormat.EMOM -> reps(15, 20)
                WorkoutFormat.AMRAP -> reps(12, 15)
                else -> reps(15, 20)
            }
            Tier.T5 -> when (format) {
                WorkoutFormat.EMOM -> if (unilateral) seconds(20, 30) else seconds(30, 40)
                WorkoutFormat.AMRAP -> if (unilateral) seconds(15, 20) else seconds(20, 30)
                else -> seconds(30, 40)
            }
            Tier.T6 -> when (format) {
                WorkoutFormat.EMOM -> if (unilateral) reps(8, 10) else reps(15, 20)
                WorkoutFormat.AMRAP -> if (unilateral) reps(6, 8) else reps(12, 15)
                else -> reps(15, 20)
            }
            Tier.T7 -> when (format) {
                WorkoutFormat.EMOM -> seconds(40, 50)
                WorkoutFormat.AMRAP -> seconds(30, 40)
                else -> seconds(40, 50)
            }
        }

    private fun reps(min: Int, max: Int): Range = Range(min, max, Range.Unit.REPS)
    private fun seconds(min: Int, max: Int): Range = Range(min, max, Range.Unit.SECONDS)
}
