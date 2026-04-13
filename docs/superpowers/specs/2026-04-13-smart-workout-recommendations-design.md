# Smart Workout Recommendations — Phase 1

## Context

Workout recommendations currently have significant intelligence gaps. Conditioning format (EMOM/AMRAP) is a coin flip. Exercise selection uses a binary 7-day cooldown instead of nuanced freshness. Weight progression is +5lb universally regardless of equipment type or individual progression rate. These produce workouts that feel random rather than coached.

This is Phase 1 of a 4-phase overhaul:
- **Phase 1** (this spec): Format alternation, exercise freshness, equipment-aware progression
- Phase 2: Periodization + deload automation
- Phase 3: Cross-gym fatigue awareness, intensity cycling
- Phase 4: RPE tracking, preference learning, injury management

## Changes

### 1. Conditioning Format Alternation

**File**: `GenerateConditioningWorkoutUseCase.kt`

Replace `Random.nextBoolean()` with history-aware alternation:

- Look up last completed conditioning workout at this gym via `workoutRepository`
- If last was EMOM → next is AMRAP
- If last was AMRAP → next is EMOM  
- No history → start with EMOM (structured builds base)
- Week reset: if last conditioning workout was before this week's Monday 00:00 → EMOM
- Skip button still overrides to the other format (existing behavior)

Mirrors the existing `resolveType()` pattern in `GenerateWorkoutUseCase`. The `predictNextFormat()` method changes from random to deterministic.

Requires: `WorkoutRepository.getLastCompletedWorkoutByGym(gymId)` already exists. Check the returned workout's `format` field.

### 2. Exercise Freshness Decay (LMU Strength)

**File**: `GenerateWorkoutUseCase.kt`

Replace binary 7-day cooldown with weighted random selection.

Current: `filterNot { it.id in recentExerciseIds }` — hard exclude for 7 days.

New: assign each candidate exercise a selection weight based on days since last performed:

```
days since last done → selection weight
0-2 days:   0.05 (nearly excluded — still recovering)
3-4 days:   0.30 (short window, deprioritize)
5-7 days:   1.00 (optimal rotation — full priority)
8-14 days:  1.10 (slightly overdue, mild boost)
14+ days:   1.00 (stale but not bonus — might be avoided for reason)
never done: 1.00 (eligible, no boost — new exercises introduced deliberately)
```

Key design decisions:
- 5-7 days is the peak because that's the optimal rotation for intermediates
- "Never done" gets no bonus — new exercises are introduced deliberately, not randomly
- Cap at 1 new exercise per workout, placed in position 2 (accessory slot, 3x10-12) — never position 0 (anchor compound)
- "New" = exercise has never appeared in any completed workout for this user (no entry in last-performed dates query)

Implementation: weighted random selection function that picks from the candidate pool using these weights. Each muscle group's pool gets independently weighted.

Requires: new DAO query to get exercise IDs with their last-performed dates (not just IDs). Something like `getExerciseLastPerformedDates(): Map<String, Date>` from completed workouts.

### 3. Conditioning Exercise Freshness (Home Gym)

**File**: `GenerateConditioningWorkoutUseCase.kt`

Apply the same freshness weighting when selecting from each EMOM/AMRAP bucket. When picking from the lower/pull/push/core pools, weight candidates by days since last performed in any conditioning workout.

Uses the same weight table as LMU strength. Prevents "same exercises, different order" sessions while allowing repeats when bucket is small.

Requires: query conditioning workout exercises with dates. Can reuse the same DAO query as #2.

### 4. Equipment-Aware Weight Progression

**File**: `StrengthSetPrescriber.kt`

Current: hardcoded `+5f` (`PROGRESSION_DELTA_LB`) for all exercises.

New: scale progression based on equipment type and individual progression rate.

```
Equipment   | Progression Rate < 5 lb/month | Rate >= 5 lb/month
------------|-------------------------------|--------------------
Barbell     | +5 lb                         | +5 lb
Dumbbell    | rep progression (+1 rep/set)  | +5 lb
Machine     | +5 lb                         | +5 lb
Cable       | +5 lb                         | +5 lb
Bodyweight  | rep progression (existing)    | rep progression
```

The `progressionRateLbPerMonth` field already exists on `ExerciseProfile` but is currently ignored. The prescriber needs the exercise's equipment type passed in (or the full Exercise object).

For rep progression on dumbbells: instead of recommending `weight + 5`, recommend same weight with `repsMax + 1`. When the user clears the new rep target for 2 sessions, THEN bump weight by 5lb. This is standard dumbbell programming — you earn the weight jump through rep mastery.

### 5. Empty Pool Safety Net

**File**: `GenerateWorkoutUseCase.kt`

Current: `mapNotNull` silently drops muscle groups with no candidates. User might get 1-2 exercises instead of 3.

New: if weighted pool for a muscle group produces no viable candidate (all weights near zero):
1. Fall back to full pool ignoring freshness (but still respect equipment + plateau)
2. Log warning: "Freshness fallback for [muscle group] — pool exhausted"
3. If still empty after fallback, skip the muscle group (existing behavior)

## Non-Goals

- Cross-gym fatigue awareness (Phase 3)
- Periodization blocks / deload automation (Phase 2)
- RPE/RIR tracking (Phase 4)
- User preference learning (Phase 4)
- Injury/limitation tracking (Phase 4)
- Conditioning rep prescription scaling per user strength (Phase 4)
- Adaptive template sizing (Phase 2)

## Verification

### Format alternation
- Complete an EMOM, verify next generated workout is AMRAP
- Complete an AMRAP, verify next is EMOM
- Wait past Monday boundary, verify first workout is EMOM regardless of last format
- Test skip button still overrides

### Exercise freshness
- Complete a workout with Exercise A on Day 1
- Generate workout on Day 2: Exercise A should almost never appear (~5% chance)
- Generate workout on Day 6: Exercise A should appear at full priority
- Generate workout on Day 10: Exercise A should appear with mild boost
- Verify new exercises (never performed) don't get bonus placement
- Check logcat for freshness weights: `adb logcat -s FortisLupus`

### Conditioning freshness
- Complete home gym EMOM with exercises X, Y, Z, W
- Generate next day's conditioning: X, Y, Z, W should be heavily deprioritized
- Generate 5 days later: X, Y, Z, W should be at full priority

### Weight progression
- Verify barbell exercise with slow progression still gets +5lb
- Verify dumbbell exercise with slow progression gets rep target bump instead of weight bump
- Verify machine exercise with normal progression gets +5lb
- Check logcat for progression decisions: `adb logcat -s FortisLupus`

### Empty pool
- Narrow gym equipment to force pool exhaustion
- Verify fallback produces exercises instead of empty workout
- Check logcat for "Freshness fallback" warnings
