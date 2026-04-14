#!/bin/bash

# ExerciseDB Integration Verification Script
# Run this to verify all files are in place before testing in Android Studio

echo "🔍 Verifying ExerciseDB Integration..."
echo ""

# Resolve the project root from the script location so the checks work on any
# clone, not just the author's machine. The script lives at the repo root.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
APP_SRC="$PROJECT_ROOT/app/src/main/java/com/workoutapp"

ERRORS=0
WARNINGS=0

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo "✅ $2"
    else
        echo "❌ MISSING: $2"
        ERRORS=$((ERRORS + 1))
    fi
}

# Function to check directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo "✅ Directory: $2"
    else
        echo "⚠️  Directory missing: $2"
        WARNINGS=$((WARNINGS + 1))
    fi
}

echo "📁 Checking New Files..."
check_file "$APP_SRC/data/remote/ExerciseDbJson.kt" "ExerciseDbJson.kt"
check_file "$APP_SRC/data/remote/ExerciseDbService.kt" "ExerciseDbService.kt"
check_file "$APP_SRC/data/mapper/ExerciseMapper.kt" "ExerciseMapper.kt"
check_file "$APP_SRC/domain/usecase/InitializeExercisesUseCase.kt" "InitializeExercisesUseCase.kt"

echo ""
echo "📝 Checking Modified Files..."
check_file "$APP_SRC/WorkoutApplication.kt" "WorkoutApplication.kt (should be updated)"
check_file "$APP_SRC/data/repository/ExerciseRepositoryImpl.kt" "ExerciseRepositoryImpl.kt (should be updated)"
check_file "$APP_SRC/data/database/WorkoutDatabase.kt" "WorkoutDatabase.kt"

echo ""
echo "📄 Checking Documentation..."
check_file "$PROJECT_ROOT/EXERCISEDB_INTEGRATION.md" "Integration Guide"
check_file "$PROJECT_ROOT/TESTING_EXERCISEDB.md" "Testing Guide"

echo ""
echo "🔐 Checking Permissions..."
if grep -q "android.permission.INTERNET" "$PROJECT_ROOT/app/src/main/AndroidManifest.xml" 2>/dev/null; then
    echo "✅ INTERNET permission found in AndroidManifest.xml"
else
    echo "❌ INTERNET permission missing in AndroidManifest.xml"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "📦 Checking Optional Assets..."
check_dir "$PROJECT_ROOT/app/src/main/assets" "assets directory"
if [ -f "$PROJECT_ROOT/app/src/main/assets/exercises.json" ]; then
    echo "✅ exercises.json bundled (offline support enabled)"
else
    echo "⚠️  exercises.json not bundled (will require internet on first launch)"
    WARNINGS=$((WARNINGS + 1))
fi

echo ""
echo "🔍 Checking Code Content..."

# Check if WorkoutApplication was updated
if grep -q "InitializeExercisesUseCase" "$APP_SRC/WorkoutApplication.kt" 2>/dev/null; then
    echo "✅ WorkoutApplication.kt contains InitializeExercisesUseCase"
else
    echo "❌ WorkoutApplication.kt NOT updated with InitializeExercisesUseCase"
    ERRORS=$((ERRORS + 1))
fi

# Check if ExerciseRepositoryImpl has sync method
if grep -q "syncExercisesFromApi" "$APP_SRC/data/repository/ExerciseRepositoryImpl.kt" 2>/dev/null; then
    echo "✅ ExerciseRepositoryImpl.kt contains syncExercisesFromApi()"
else
    echo "❌ ExerciseRepositoryImpl.kt missing syncExercisesFromApi() method"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "═══════════════════════════════════════════════════"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "🎉 SUCCESS! All files in place and ready to test!"
    echo ""
    echo "Next steps:"
    echo "1. Open Android Studio"
    echo "2. Sync Gradle (File → Sync Project with Gradle Files)"
    echo "3. Build → Clean Project"
    echo "4. Build → Rebuild Project"
    echo "5. Run the app and check Logcat for 'WorkoutApp' tag"
    echo ""
elif [ $ERRORS -eq 0 ]; then
    echo "⚠️  WARNING: $WARNINGS warnings found (non-critical)"
    echo ""
    echo "You can still test in Android Studio, but consider:"
    echo "- Bundling exercises.json for offline support"
    echo ""
else
    echo "❌ ERROR: $ERRORS critical issues found!"
    echo "⚠️  WARNING: $WARNINGS warnings found"
    echo ""
    echo "Please fix the errors before testing in Android Studio."
    echo ""
fi

echo "📖 For detailed testing instructions, see:"
echo "   $PROJECT_ROOT/TESTING_EXERCISEDB.md"
echo ""
