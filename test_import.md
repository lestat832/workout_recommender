# Testing Strong Import Feature

## How to Test

1. **Open the app**
2. **Access Debug Menu**: Tap on "FORTIS LUPUS" title 5 times
3. **Find Import Section**: In the debug menu, scroll down to "Import Strong Data"
4. **Click "Select CSV File to Import"**
5. **Select the Strong export file**: Navigate to `/Users/marcgeraldez/Downloads/strong3730064339565542036.csv`
6. **Wait for import to complete**
7. **Check the import results dialog**:
   - Should show 132 total workouts
   - Number of imported workouts
   - Number of new exercises created
   - Number of mapped exercises

## Expected Results

- All 132 workouts from December 2023 to August 2025 should be imported
- Workouts should appear in "Pack History" section
- New custom exercises should be created for unmapped exercises
- Existing exercises should be mapped correctly (e.g., "Bench Press (Barbell)" → "Barbell Bench Press")
- All weights should be converted from kg to lbs

## Files Created/Modified

### New Files:
- `/app/src/main/java/com/workoutapp/data/utils/StrongCsvParser.kt` - CSV parsing logic
- `/app/src/main/java/com/workoutapp/data/utils/ExerciseMapper.kt` - Exercise name mapping
- `/app/src/main/java/com/workoutapp/domain/usecase/ImportWorkoutUseCase.kt` - Import business logic

### Modified Files:
- `/app/src/main/java/com/workoutapp/presentation/viewmodel/HomeViewModel.kt` - Added import functionality
- `/app/src/main/java/com/workoutapp/presentation/ui/home/HomeScreen.kt` - Added import UI

## Known Exercise Mappings

The following Strong exercises will be mapped to existing exercises:
- Bench Press (Barbell) → Barbell Bench Press
- Bench Press (Dumbbell) → Dumbbell Bench Press
- Deadlift (Barbell) → Deadlift
- Squat (Barbell) → Barbell Squat
- Bicep Curl (Dumbbell) → Dumbbell Curl
- Lat Pulldown (Cable) → Lat Pulldown
- And many more...

Custom exercises will be created for:
- Deadlift (Dumbbell)
- Bench / Shrug
- Back Extension
- And other unmapped exercises