# Strava Workout Sync - Requirements Discovery

> **Instructions:** Answer the questions below to help design the Strava sync feature. You can answer inline by replacing the `[ ]` with `[x]` for checkboxes, or writing your answers after the **Answer:** prompts.

---

## 1. Sync Scope & Timing

### When should the sync happen?

- [ ] Immediately after completing a workout (auto-sync)
- [ ] Manual button press after workout ("Sync to Strava" button)
- [ ] Batch sync at end of day
- [ ] Background sync when connected to WiFi
- [ ] Other (describe below)

**Answer:**
immediately


### What defines a workout as "done"?

- [ ] When user marks workout as complete
- [ ] After all exercises are logged
- [ ] After workout is saved to database
- [ ] Other (describe below)

**Answer:**
when user marks workout as complete


---

## 2. Workout Data Mapping

### What workout data should be sent to Strava?

Check all that apply:

- [ x] Exercise names and sets/reps 
- [ x] Duration of workout (start/end time)
- [ ] Estimated calories burned
- [ ] Heart rate data (if tracked)
- [x ] Specific muscle groups worked
- [ ] Custom notes or comments
- [x] Workout type (PUSH/PULL)
- [x ] Total volume (sets × reps × weight)
- [ ] Other (describe below)

**Answer:**



### Strava Activity Type

Strava supports these activity types for strength training:
- **Workout** (generic)
- **Weight Training** (specific category)
- **Crossfit**
- **Custom naming** based on your workout type

**Which activity type should we use?**

- [ ] Generic "Workout"
- [x ] "Weight Training"
- [ ] Map to custom names (PUSH → "Upper Body Strength", PULL → "Back & Biceps")
- [ ] Let user choose per workout
- [ ] Other (describe below)

**Answer:**



### Strava Data Limitation Awareness

⚠️ **Important:** Strava's API has limited fields for strength training. It primarily supports:
- Activity name
- Activity type
- Start time
- Duration
- Description (free text)

**There are NO specific fields for:**
- Sets/reps
- Weight lifted
- Individual exercises

**How should we handle this?**

- [ x] Format all workout details in the description field (text summary)
- [ ] Use custom encoding in description (machine-readable format)
- [ ] Minimal data (just workout name and duration)
- [ ] Other approach (describe below)

**Answer:**



---

## 3. User Experience Flow

### Authentication

**When should users connect their Strava account?**

- [ ] During onboarding (first app launch)
- [x ] Later in settings (when they want to enable sync)
- [ ] First time they try to sync a workout
- [ ] Other (describe below)

**Answer:**



**What happens if user tries to sync without connecting Strava?**

- [ ] Show error message and prompt to connect
- [x ] Automatically redirect to Strava connection flow
- [ ] Queue workout for sync and prompt connection
- [ ] Other (describe below)

**Answer:**



### Sync Feedback

**What sync feedback should users see?**

Check all that apply:

- [ ] Real-time sync status (syncing, success, failed)
- [x ] Push notification when sync completes
- [ ] Sync history screen (view all synced workouts)
- [x ] Retry mechanism for failed syncs
- [ x] Sync status badge on workout card
- [ ] Other (describe below)

**Answer:**



---

## 4. Privacy & Permissions

### Default Sync Behavior

**Should workouts automatically sync to Strava?**

- [ x] Yes, auto-sync all workouts by default (user can disable globally)
- [ ] No, user must enable sync per workout (manual opt-in)
- [ ] Auto-sync with option to exclude specific workouts
- [ ] Ask user preference during onboarding
- [ ] Other (describe below)

**Answer:**



### Strava Privacy Settings

**Should users be able to control Strava activity privacy from your app?**

- [ ] Yes, let users choose (Public/Followers-Only/Private) before syncing
- [ x] No, use default Strava account settings
- [ ] Always private by default
- [ ] Other (describe below)

**Answer:**



---

## 5. Offline & Error Handling

### Offline Behavior

**What happens when device is offline?**

- [ x] Queue workouts for sync when connection returns
- [ ] Show error and require manual retry
- [ ] Silent failure (don't sync)
- [ ] Other (describe below)

**Answer:**



**If queuing for sync:**

- [ ] Show pending syncs indicator
- [x ] Auto-sync when connection detected
- [ ] Max queue size: _____ workouts
- [ ] Max retry attempts: _____ times

**Answer:**



### Failed Sync Handling

**What should happen if sync fails?**

- [ x] Show error notification with retry button
- [ ] Auto-retry up to X times
- [ ] Mark workout as "sync failed" in UI
- [ ] Allow user to manually retry later
- [ ] Other (describe below)

**Answer:**



---

## 6. Workout Editing & Updates

### Post-Sync Editing

**If user edits a workout AFTER syncing to Strava:**

- [ ] Update the Strava activity automatically
- [ ] Leave Strava as-is (no updates)
- [ x] Ask user if they want to update Strava
- [ ] Mark as "out of sync" and require manual re-sync
- [ ] Other (describe below)

**Answer:**



### Deleting Workouts

**If user deletes a workout that was synced:**

- [ ] Also delete from Strava
- [x ] Leave on Strava (don't delete)
- [ ] Ask user if they want to delete from Strava too
- [ ] Other (describe below)

**Answer:**



---

## 7. Historical Data & Bulk Sync

### Past Workouts

**Should we support syncing historical workouts?**

- [ ] Yes, add "Sync All Past Workouts" feature
- [ ] Yes, but limit to last _____ days/workouts
- [x ] No, only sync new workouts going forward
- [ ] Other (describe below)

**Answer:**



---

## 8. Integration with Strong App Import

### Strong App Connection

You mentioned wanting to import workouts from Strong app earlier.

**Should Strava sync integrate with Strong import?**

- [ ] Yes, auto-sync imported Strong workouts to Strava
- [ ] No, keep them separate (manual sync only)
- [ ] Ask user during import if they want to sync to Strava
- [ x] Not applicable / don't care
- [ ] Other (describe below)

**Answer:**



---

## 9. MVP Scope Definition

### Minimum Viable Product

**What's the MINIMUM feature set that would be useful to you?**

Rank these features in priority order (1 = highest, 8 = lowest):

- [ ] _____ One-way sync (App → Strava only)
- [ ] _____ Auto-sync on workout completion
- [ ] _____ Manual sync button per workout
- [ ] _____ Sync status indicator
- [ ] _____ Strava OAuth authentication
- [ ] _____ Offline queue and retry
- [ ] _____ Sync history screen
- [ ] _____ Edit Strava activity details before syncing

**Answer:**



### Phase 1 vs Phase 2

**What MUST be in Phase 1 (launch)?**

**Answer:**



**What can wait for Phase 2 (later enhancement)?**

**Answer:**



---

## 10. Personal Context

### Your Use Case

**Are you currently manually entering workouts into Strava?**

- [ ] Yes, after every workout
- [ ] Yes, occasionally
- [ x] No, but I want to start
- [ ] No, but users have requested this

**Answer:**



**How often do you work out?**

- [ ] 1-2 times per week
- [ x] 3-4 times per week
- [ ] 5-6 times per week
- [ ] Daily
- [ ] Other: _____

**Answer:**



**Is this feature solving:**

- [x ] A personal pain point (you need this)
- [ ] A user request (others have asked for it)
- [ ] Nice-to-have feature (would be cool)
- [ ] Competitive feature (other apps have it)
- [ ] Other (describe below)

**Answer:**



---

## 11. Technical Constraints

### Current Time Tracking

**Do you currently track workout start/end times?**

- [ ] Yes, automatically
- [ x] Yes, manually
- [ ] No, but can add it
- [ ] No, and don't want to

**Answer:**



**If not tracking time, how should we calculate workout duration for Strava?**

- [ ] Estimate based on number of exercises and sets
- [ ] Use a fixed duration (e.g., 45 minutes)
- [ ] Ask user to enter duration before syncing
- [ ] Other (describe below)

**Answer:**



---

## 12. Data Format Preferences

### Strava Description Format

**How should workout details appear in Strava description?**

**Example Workout:**
- 3 sets × 10 reps Barbell Bench Press @ 135 lbs
- 4 sets × 8 reps Squats @ 185 lbs
- 3 sets × 12 reps Dumbbell Rows @ 50 lbs

**Format Option A (Detailed):**
```
💪 PUSH Workout

Chest:
• Barbell Bench Press: 3×10 @ 135 lbs
• Incline Dumbbell Press: 3×12 @ 50 lbs

Legs:
• Squats: 4×8 @ 185 lbs
• Leg Press: 3×15 @ 270 lbs

Total Volume: 12,450 lbs
Duration: 58 minutes
```

**Format Option B (Compact):**
```
PUSH • 58 min • 12.4K lbs
Bench 3×10@135 | Squat 4×8@185 | Rows 3×12@50 | Press 3×15@270
```

**Format Option C (Minimal):**
```
Push workout - 4 exercises, 13 sets, 58 minutes
```

**Which format do you prefer?**

- [ ] Option A (Detailed with emojis)
- [ ] Option B (Compact, data-dense)
- [ ] Option C (Minimal)
- [ ] Custom format (describe below)

**Answer:**
format a


---

## 13. Additional Features

### Optional Enhancements

**Would any of these features be valuable?**

- [ ] Share workout to social media (Instagram, Twitter)
- [x ] Generate workout summary image for sharing
- [ ] Compare Strava stats with app stats
- [ ] Sync photos from workout to Strava
- [ ] Tag friends/workout partners in Strava activity
- [ ] Add location/gym to Strava activity
- [ ] Other ideas (describe below)

**Answer:**



---

## 14. Open Questions

**Any other requirements, concerns, or ideas?**

**Answer:**



---

## Summary

Once you've answered these questions, I'll create:

1. **Detailed technical specification**
2. **UX flow diagrams**
3. **Data mapping documentation**
4. **Implementation plan with phases**
5. **API integration guide**
6. **Testing strategy**

**Next Step:** Fill out this document and let me know when you're ready to review! 🚀
