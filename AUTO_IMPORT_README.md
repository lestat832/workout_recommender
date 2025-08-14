# Auto-Import Debug Data Feature

## Overview
The app automatically imports 132 historical workouts from Strong app on first launch in debug builds. This is for testing purposes and is automatically disabled in production releases.

## How It Works

### Automatic Import
1. **On App Launch**: When the app starts in debug mode, it checks if debug data has been imported
2. **First Time Only**: Import only happens once (tracked via SharedPreferences)
3. **132 Workouts**: Imports all workouts from December 2023 to August 2025
4. **Automatic Exercise Mapping**: Maps Strong exercises to existing exercises or creates custom ones

### Data Location
- CSV file: `app/src/main/assets/debug/strong_workout_data.csv`
- Size: 288KB (2,490 lines)
- Format: Strong app export format (semicolon-delimited)

## Configuration

### Build Types
```kotlin
// app/build.gradle.kts
buildTypes {
    debug {
        buildConfigField("Boolean", "ENABLE_DEBUG_DATA_IMPORT", "true")  // ✅ Auto-import enabled
    }
    release {
        buildConfigField("Boolean", "ENABLE_DEBUG_DATA_IMPORT", "false") // ❌ Auto-import disabled
    }
}
```

### To Disable Auto-Import
Simply change the flag in `build.gradle.kts`:
```kotlin
buildConfigField("Boolean", "ENABLE_DEBUG_DATA_IMPORT", "false")
```

## Debug Menu Controls

Access the debug menu by tapping "FORTIS LUPUS" title 5 times:

### Auto-Import Status Section
- **Status Display**: Shows if debug data has been imported
- **Reset Import Flag**: Clears the import flag (allows re-import on next launch)
- **Force Re-import**: Immediately re-imports the data

### Manual Import Section
- **Select CSV File**: Manually import any Strong CSV file
- Shows import progress and results

## Files Involved

### Core Files
1. `ImportDebugDataUseCase.kt` - Handles auto-import logic
2. `strong_workout_data.csv` - Your workout data (132 workouts)
3. `WorkoutApplication.kt` - Triggers auto-import on app start
4. `build.gradle.kts` - Build configuration flag

### How to Update Data
1. Export new data from Strong app
2. Replace `app/src/main/assets/debug/strong_workout_data.csv`
3. Clear app data or use "Reset Import Flag" button
4. Restart app

## Production Release

When ready for production:
1. The `ENABLE_DEBUG_DATA_IMPORT` flag is automatically `false` in release builds
2. Consider removing the debug CSV file to reduce APK size:
   - Delete `app/src/main/assets/debug/` folder
   - Or add to `.aaptOptions` to exclude from release

## Testing

### Verify Auto-Import
1. Clean install the app (or clear app data)
2. Launch the app
3. Check "Pack History" - should show 132 workouts
4. Open debug menu - should show "✓ Debug data has been auto-imported"

### Test Re-Import
1. Open debug menu (tap title 5 times)
2. Click "Reset Import Flag"
3. Click "Force Re-import"
4. Check that workouts are re-imported

## Benefits
- ✅ No manual import needed for testing
- ✅ Consistent test data across builds
- ✅ Easy to disable for production
- ✅ Clean separation of debug/production code
- ✅ Preserves all workout history for testing