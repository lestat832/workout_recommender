# Fortis Lupus - Complete Project Guide

> **Purpose**: Comprehensive guide for Custom GPT to understand the Fortis Lupus workout tracking application

---

## 📱 Project Overview

**Fortis Lupus** (Latin: "Strength of the Wolf") is an Android workout tracking application with intelligent exercise recommendations, wolf-themed branding, and seamless integration with external fitness platforms.

### Core Identity
- **Name**: Fortis Lupus (formerly "Workout Tracker")
- **Platform**: Android (Kotlin, Jetpack Compose)
- **Philosophy**: Strength through intelligent training, symbolized by the wolf pack
- **Target Users**: Strength training enthusiasts who want structured, alternating push/pull workouts

---

## 🎯 Key Features

### 1. **Smart Workout Generation**
- **Alternating Push/Pull System**:
  - Alpha Training (Push): Chest, shoulders, triceps, quads
  - Pack Strength (Pull): Back, biceps, hamstrings, core
- **7-Day Exercise Cooldown**: Prevents muscle fatigue by ensuring exercises aren't repeated within 7 days
- **Intelligent Exercise Selection**: Automatically suggests exercises based on workout type and cooldown status
- **Multi-Muscle Group Support**: Exercises can target multiple muscle groups simultaneously

### 2. **Exercise Library Integration**
- **873+ Exercises** from [Free Exercise DB](https://github.com/yuhonas/free-exercise-db)
- **Automatic Classification**: Exercises mapped to 7 muscle groups (Chest, Shoulders, Back, Biceps, Triceps, Legs, Core)
- **Visual Instructions**: Each exercise includes images from GitHub CDN
- **Exercise Metadata**: Equipment type, muscle groups, workout type classification
- **Custom Exercise Creation**: Users can create custom exercises with:
  - Custom name and description
  - Multiple muscle group selection
  - Equipment selection
  - Automatic workout type assignment
  - "CUSTOM" badge for identification

### 3. **Strong App Import**
- **CSV Import**: Import workout history from Strong fitness app
- **Exercise Mapping**: Intelligent mapping of 50+ Strong exercises to Fortis Lupus exercises
- **Auto-Creation**: Unmapped exercises automatically created as custom exercises
- **Weight Conversion**: Automatic kg to lbs conversion with proper rounding
- **Import Statistics**: Shows workouts imported, exercises created, errors encountered
- **Manual Trigger Only**: Import via debug menu, no automatic import on app launch

### 4. **Workout Tracking**
- **Set/Rep/Weight Logging**: Track detailed workout performance
- **Real-time Progress**: Mark sets as completed with explicit checkboxes
- **Exercise Shuffling**: Swap exercises mid-workout while maintaining muscle group focus
- **Add Exercises**: Add additional exercises during workout via FAB button
- **Remove Exercises**: Delete exercises from active workout with confirmation
- **Total Volume Calculation**: Automatic calculation of total weight lifted per exercise
- **Workout Duration**: Track start/end times for accurate workout duration

### 5. **Workout Management**
- **Save Progress**: Mark workout as incomplete but preserve completed sets
- **Discard Workout**: Delete entire workout with no saved data
- **Edit After Completion**: Modify workouts post-completion
- **Expandable History**: View detailed exercise breakdowns in workout history
- **Recent Workouts**: Display up to 50 recent workouts on home screen

### 6. **Strava Integration** (In Development)
- **One-Way Sync**: Workout App → Strava
- **Auto-Sync on Completion**: Immediately sync when user marks workout complete
- **OAuth Authentication**: Secure Strava account connection via Settings
- **Detailed Activity Format**: Workout details formatted in Strava description:
  ```
  💪 PUSH Workout

  Chest:
  • Barbell Bench Press: 3×10 @ 135 lbs
  • Incline Dumbbell Press: 3×12 @ 50 lbs

  Total Volume: 12,450 lbs
  Duration: 58 minutes
  ```
- **Offline Queue**: Workouts queue for sync when offline, auto-sync on connection
- **Sync Status Indicators**: Visual badges on workout cards showing sync status
- **Push Notifications**: Notify when sync completes
- **Retry Mechanism**: Manual retry for failed syncs
- **Privacy**: Uses default Strava account privacy settings

### 7. **Wolf-Themed Branding**
- **Custom Wolf Icon**: DALL-E generated app icon
- **Splash Screen**: Howling wolf with moon and stars
- **Wolf Color Palette**: wolf_charcoal, wolf_blue, moon_silver
- **Themed Language**:
  - "Begin the Hunt" (Start Workout)
  - "Pack History" (Recent Workouts)
  - "Today's Hunt" (Today's Workout)
  - "Build Your Pack" (Exercise Selection)
  - "Alpha Training" (Push Workouts)
  - "Pack Strength" (Pull Workouts)

### 8. **Onboarding Experience**
- **First-Time User Experience (FTUE)**: Clean onboarding without auto-import
- **Exercise Selection**: Group exercises by muscle category with collapsible sections
- **Selection Counts**: Shows available exercises per muscle group
- **"Select All" / "Clear All"**: Quick selection controls for testing
- **Persistent Onboarding State**: Uses DataStore to prevent onboarding loop
- **Smooth Navigation**: Seamless transition to home screen after completion

### 9. **Developer Tools**
- **Date Testing Debug Menu**:
  - Tap "FORTIS LUPUS" title to access
  - Adjust date offset to test workout alternation
  - Test exercise cooldown periods
  - Simulate future/past states
- **Import Debug Menu**: Manual import trigger for Strong app data
- **Build Variants**: Debug vs Release builds with different features

---

## 🏗️ Technical Architecture

### Tech Stack
- **Language**: Kotlin 1.9.21
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room (SQLite) - Database Version 5
- **Dependency Injection**: Hilt (Dagger)
- **Image Loading**: Coil
- **Async Operations**: Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose
- **Serialization**: Kotlin Serialization
- **State Management**: DataStore Preferences
- **Build System**: Gradle with Kotlin DSL

### Project Structure
```
app/
├── data/
│   ├── local/           # Room database entities, DAOs
│   ├── remote/          # ExerciseDB API service
│   ├── repository/      # Repository implementations
│   └── mapper/          # Data transformations
├── domain/
│   ├── model/           # Business models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business logic use cases
├── presentation/
│   ├── screens/         # Compose UI screens
│   ├── viewmodel/       # ViewModels
│   └── components/      # Reusable UI components
└── di/                  # Hilt modules

shared/
└── src/main/kotlin/     # Kotlin Multiplatform shared code
    └── domain/model/    # Shared domain models
```

### Database Schema (Room v5)

#### Exercise Entity
```kotlin
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<MuscleGroup>,  // Multi-muscle support
    val workoutType: WorkoutType,         // PUSH or PULL
    val imageUrl: String?,
    val instructions: String?,
    val equipment: String?,
    val isActive: Boolean = true,
    val isUserCreated: Boolean = false,   // Custom exercise flag
    val createdAt: Long = System.currentTimeMillis()
)
```

#### Workout Entity
```kotlin
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val workoutType: WorkoutType,
    val status: WorkoutStatus,            // ACTIVE, COMPLETED, INCOMPLETE
    val startTime: Long?,
    val endTime: Long?,
    val notes: String?,
    val stravaSyncStatus: StravaSyncStatus? = null,
    val stravaActivityId: String? = null
)
```

#### WorkoutExercise Entity
```kotlin
@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val orderIndex: Int,
    val completed: Boolean = false
)
```

### Key Use Cases
- `GenerateWorkoutUseCase`: Creates alternating push/pull workouts
- `GetAvailableExercisesUseCase`: Filters exercises by cooldown and type
- `InitializeExercisesUseCase`: Loads ExerciseDB on first launch
- `ImportWorkoutUseCase`: Imports Strong app CSV data
- `CreateCustomExerciseUseCase`: Creates user-defined exercises
- `SyncToStravaUseCase`: Syncs completed workouts to Strava (planned)

### Data Flow
1. **User Action** → ViewModel
2. **ViewModel** → Use Case (business logic)
3. **Use Case** → Repository (data layer)
4. **Repository** → Local Database / Remote API
5. **Response** → Flow back to UI via StateFlow/MutableState

---

## 📊 Data Models

### Enums

#### MuscleGroup
```kotlin
enum class MuscleGroup {
    CHEST,      // Pectorals
    SHOULDER,   // Deltoids
    BACK,       // Lats, traps, rhomboids
    BICEP,      // Biceps
    TRICEP,     // Triceps
    LEGS,       // Quads, hamstrings, glutes, calves
    CORE        // Abs, obliques
}
```

#### WorkoutType
```kotlin
enum class WorkoutType {
    PUSH,       // Alpha Training
    PULL        // Pack Strength
}
```

#### WorkoutStatus
```kotlin
enum class WorkoutStatus {
    ACTIVE,     // In progress
    COMPLETED,  // Finished
    INCOMPLETE  // Partially completed
}
```

#### StravaSyncStatus (Planned)
```kotlin
enum class StravaSyncStatus {
    PENDING,    // Queued for sync
    SYNCING,    // Currently syncing
    SYNCED,     // Successfully synced
    FAILED,     // Sync failed
    NOT_SYNCED  // Not synced (manual only)
}
```

### Business Rules

#### Exercise Cooldown Logic
- Tracks last usage date for each exercise
- Prevents selection if used within 7 days
- Cooldown applies per exercise, not muscle group
- Custom exercises follow same cooldown rules

#### Workout Alternation Logic
- Push/Pull alternates based on last completed workout
- If last workout was PUSH → next is PULL
- If last workout was PULL → next is PUSH
- New users start with PUSH (Alpha Training)
- Date offset (debug mode) affects alternation

#### PUSH/PULL Classification
Primary muscles for PUSH:
- Chest, Shoulders, Triceps, Quadriceps, Calves, Glutes

Primary muscles for PULL:
- Back, Biceps, Hamstrings, Core

Exercises classified by:
1. ExerciseDB "force" field (86% coverage)
2. Fallback: Primary muscle group

---

## 🚀 Recent Development History

### Latest Major Features (2025)

#### August 2025: Strong App Import
- CSV import with exercise mapping
- Custom exercise auto-creation
- Weight conversion (kg → lbs)
- Manual import trigger (debug menu)
- Import statistics dialog

#### January 2025: Multi-Muscle Groups
- Support for exercises targeting multiple muscle groups
- Database migration v4 → v5
- Enhanced exercise filtering
- Improved exercise organization

#### January 2025: Custom Exercise Creation
- Mid-workout custom exercise creation
- Multi-muscle group selection
- Equipment selection
- "CUSTOM" badge system
- Onboarding persistence fix (DataStore)

#### January 2025: Wolf Rebranding
- Complete rebrand to "Fortis Lupus"
- Custom DALL-E wolf assets
- Wolf-themed UI language
- Wolf color palette

#### January 2025: Exercise Organization
- Grouped exercises by muscle category
- Collapsible sections
- Alphabetical sorting
- Selection counts per group

---

## 🔮 Planned Features

### Strava Integration (Active Development)
**Status**: Requirements gathering complete, implementation roadmap created

**Phase 1 (MVP)**:
- OAuth authentication flow
- One-way sync (App → Strava)
- Auto-sync on workout completion
- Detailed activity description format
- Sync status indicators

**Phase 2**:
- Offline queue and retry mechanism
- Push notifications for sync events
- Sync history screen
- Manual retry for failed syncs
- Edit Strava details before sync

**Phase 3**:
- Generate workout summary images
- Update Strava on workout edits
- Bulk historical workout sync
- Enhanced error handling

### Future Enhancements
- **Analytics**: Progress tracking, PR tracking, volume trends
- **Social Features**: Share workouts, workout templates
- **Smart Recommendations**: Weight progression suggestions
- **Rest Timer**: Countdown timer between sets
- **Cloud Sync**: Backup workouts to cloud
- **Export**: Export workout data (CSV, PDF)
- **Workout Templates**: Save and reuse favorite workouts
- **Supersets**: Support for paired exercises

---

## 🔧 Configuration & Setup

### Environment Requirements
- **Android Studio**: Arctic Fox or later
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **JVM Target**: 17

### Build Configuration

#### local.properties (for Strava)
```properties
STRAVA_CLIENT_ID=your_client_id_here
STRAVA_CLIENT_SECRET=your_client_secret_here
```

#### Build Variants
- **Debug**: Includes debug data import, test features, date offset
- **Release**: Production build, no debug features

### Dependencies Highlights
```gradle
// Core
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0

// Compose
compose-bom:2023.10.01
material3

// Navigation
navigation-compose:2.7.6

// Database
room-runtime:2.6.1
room-ktx:2.6.1

// DI
hilt-android:2.50
hilt-navigation-compose:1.1.0

// Network
retrofit:2.9.0
kotlinx-serialization-json:1.6.2

// Image Loading
coil-compose:2.5.0

// State
datastore-preferences:1.0.0
```

---

## 🧪 Testing & Development

### Debug Features

#### Date Offset Menu
**Access**: Tap "FORTIS LUPUS" title in header
**Purpose**: Test date-dependent features without waiting

**Use Cases**:
- Test workout alternation (+1 day)
- Test exercise cooldown (+8 days)
- Test future state (+30 days)
- Test workout history with various dates

#### Import Debug Menu
**Access**: Three-dot menu on home screen → "Import Marc's Workouts"
**Purpose**: Quickly populate app with real workout data

**Features**:
- Embedded 133 workouts (Dec 2023 - Aug 2025)
- One-time import prevention
- Manual trigger only
- Import statistics display

### Building for Testing

#### Development Install
```bash
# Quick install to connected device
./gradlew installDebug

# Or via Android Studio: Run button (▷)
```

#### Generate APK
```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

#### ADB Commands
```bash
# Check connected devices
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep WorkoutApp

# Uninstall
adb uninstall com.workoutapp
```

---

## 📁 Key Files Reference

### Documentation
- `README.md` - Project overview and setup
- `CHANGELOG.md` - Detailed version history
- `STRAVA_SYNC_REQUIREMENTS.md` - Strava feature requirements
- `STRAVA_SYNC_SPEC.md` - Strava technical specification
- `STRAVA_IMPLEMENTATION_ROADMAP.md` - Strava development plan
- `EXERCISEDB_INTEGRATION.md` - ExerciseDB integration guide
- `claudedocs/` - Claude Code session documentation

### Core Implementation Files
- `app/src/main/java/com/workoutapp/data/local/` - Database entities
- `app/src/main/java/com/workoutapp/data/repository/` - Data layer
- `app/src/main/java/com/workoutapp/domain/usecase/` - Business logic
- `app/src/main/java/com/workoutapp/presentation/` - UI layer
- `app/src/main/java/com/workoutapp/di/` - Dependency injection

### Configuration Files
- `app/build.gradle.kts` - App-level build configuration
- `build.gradle.kts` - Project-level build configuration
- `gradle.properties` - Gradle properties
- `local.properties` - Local environment variables (not in git)

---

## 🎨 Design Assets

### Color Palette
```kotlin
wolf_charcoal = Color(0xFF2C2C2E)    // Dark gray, primary background
wolf_blue = Color(0xFF0A84FF)        // Accent blue
moon_silver = Color(0xFFE5E5EA)      // Light gray for text
```

### Wolf Assets
- **App Icon**: `res/mipmap-*/ic_launcher.png` (adaptive icon)
- **Splash Screen**: Howling wolf with moon
- **Empty State**: Wolf placeholder for custom exercises
- **Logo**: Wolf silhouette in app header

### Image Sources
- **Wolf Assets**: DALL-E generated (see `design/dalle_prompts.md`)
- **Exercise Images**: [Free Exercise DB](https://github.com/yuhonas/free-exercise-db)
- **Icons**: Material Design Icons

---

## 🐺 Wolf Theme Philosophy

The wolf symbolizes:
- **Strength**: Individual power (Fortis = Strong)
- **Pack Mentality**: Training consistency and community
- **Adaptability**: Push/Pull alternation like hunting strategies
- **Endurance**: 7-day cooldown mimics recovery cycles in nature

Thematic elements:
- **"The Hunt"**: Workout session (active pursuit)
- **"Pack History"**: Collective achievements
- **"Alpha Training"**: Leading with push strength
- **"Pack Strength"**: Collective pull power

---

## 🔐 Privacy & Security

### Data Storage
- **Local Only**: All workout data stored locally in Room database
- **No Cloud Sync** (currently): User data never leaves device
- **Strava OAuth**: Secure token-based authentication (when implemented)
- **No User Accounts**: No registration or login required

### Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Usage**:
- INTERNET: ExerciseDB API, Strava sync, image loading
- ACCESS_NETWORK_STATE: Check connectivity before sync

---

## 📈 Usage Statistics (Embedded Test Data)

### Strong App Import Test Data
- **Total Workouts**: 133
- **Date Range**: December 2023 - August 2025
- **Exercises**: 50+ unique Strong exercises
- **Purpose**: Real-world testing of import functionality

---

## 🚨 Known Limitations

### Current Constraints
1. **Android Only**: No iOS version (Kotlin Multiplatform structure prepared)
2. **Local Storage**: No cloud backup (data loss if app uninstalled)
3. **Single User**: No multi-user support
4. **English Only**: No internationalization
5. **Strava Only**: No other platform integrations (yet)

### Technical Debt
1. **No automated tests**: Manual testing only
2. **Limited error handling**: Basic error messages
3. **No offline-first sync**: Requires internet for ExerciseDB initial load
4. **No workout templates**: Each workout created from scratch

---

## 🎯 Product Roadmap Priority

### High Priority (Next 3 Months)
1. ✅ Strava integration (in progress)
2. Workout analytics dashboard
3. Progress tracking (PR history)
4. Rest timer between sets

### Medium Priority (3-6 Months)
1. Cloud backup/sync
2. Workout templates
3. Export functionality
4. Social sharing features

### Low Priority (6+ Months)
1. iOS version
2. Wearable integration
3. Video exercise demonstrations
4. Meal tracking integration

---

## 🤝 Contributing Guidelines

### Code Style
- **Kotlin Official Style Guide**: Follow JetBrains conventions
- **Compose Best Practices**: Use remember, derivedStateOf, keys properly
- **Clean Architecture**: Maintain separation of concerns
- **SOLID Principles**: Single responsibility, dependency injection

### Commit Message Format
```
type: brief description

Detailed explanation if needed

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Types**: feat, fix, docs, refactor, test, chore

---

## 📞 Support & Resources

### Documentation
- **Project README**: `/README.md`
- **Change History**: `/CHANGELOG.md`
- **Claude Docs**: `/claudedocs/`
- **ExerciseDB Guide**: `/EXERCISEDB_INTEGRATION.md`
- **Strava Spec**: `/STRAVA_SYNC_SPEC.md`

### External Resources
- **Free Exercise DB**: https://github.com/yuhonas/free-exercise-db
- **Strava API Docs**: https://developers.strava.com/docs/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Room Database**: https://developer.android.com/training/data-storage/room

---

## 🏆 Success Metrics

### Current State (as of October 2025)
- ✅ 873+ exercises integrated
- ✅ Custom exercise creation
- ✅ Strong app import (133 workouts)
- ✅ Multi-muscle group support
- ✅ Complete wolf rebranding
- 🔄 Strava integration (in progress)

### Target Metrics
- **User Retention**: 30-day retention target
- **Workout Frequency**: Average 3-4 workouts/week
- **Exercise Variety**: Users utilizing 20+ different exercises
- **Strava Sync**: 90%+ successful sync rate

---

## 💡 Developer Notes

### Quick Start for New Developers
1. Clone repository
2. Open in Android Studio
3. Sync Gradle files
4. Add `local.properties` with Strava credentials (optional)
5. Run on emulator or physical device
6. Use debug menu to populate with test data

### Common Development Tasks

**Add New Exercise**:
- Database: `ExerciseEntity` in `data/local/`
- Use Case: `CreateCustomExerciseUseCase`
- UI: `CreateExerciseScreen`

**Modify Workout Logic**:
- Use Case: `GenerateWorkoutUseCase`
- Business rules in `domain/usecase/`

**Update UI**:
- Screens: `presentation/screens/`
- ViewModels: `presentation/viewmodel/`
- Components: `presentation/components/`

**Database Changes**:
- Update entities in `data/local/entity/`
- Create migration in `AppDatabase`
- Increment database version
- Test migration thoroughly

---

## 🔍 Troubleshooting

### Common Issues

**"No exercises showing"**:
- Check internet connection (first launch)
- Verify ExerciseDB initialization
- Check logs: `adb logcat | grep WorkoutApp`

**"Strava not connecting"**:
- Verify `local.properties` has credentials
- Check OAuth redirect URL configuration
- Ensure internet connection

**"Import failed"**:
- Check CSV file format (semicolon-delimited)
- Verify Strong app export version
- Check error dialog for details

**"Workout not alternating"**:
- Check last completed workout type
- Try date offset debug menu
- Verify workout completion logic

---

## 📝 Version Information

- **Current Version**: 1.0 (pre-release)
- **Database Version**: 5
- **Kotlin Version**: 1.9.21
- **Compose Version**: 2023.10.01
- **Target Android**: 14 (API 34)
- **Minimum Android**: 7.0 (API 24)

---

## 🎓 Learning Resources

### For Understanding the Codebase
1. **Clean Architecture**: Robert C. Martin's principles
2. **MVVM Pattern**: Android Architecture Components
3. **Jetpack Compose**: Modern Android UI toolkit
4. **Room Database**: Local persistence
5. **Hilt**: Dependency injection for Android
6. **Kotlin Coroutines**: Asynchronous programming

### Recommended Reading Order
1. Project README → Understand scope
2. CHANGELOG → See evolution
3. ExerciseDB Integration → Data source
4. Strava Sync Spec → Upcoming features
5. Code walkthrough in order: data → domain → presentation

---

## 🌟 Project Highlights

### What Makes Fortis Lupus Unique
1. **Intelligent Workout Planning**: Automatic push/pull alternation with science-backed cooldown
2. **Rich Exercise Library**: 873+ exercises with visual guides
3. **Seamless Import**: Migrate from Strong app with one click
4. **Beautiful Branding**: Cohesive wolf theme throughout
5. **Developer-Friendly**: Clean architecture, well-documented, testable
6. **Privacy-First**: Local-only data storage, no tracking
7. **Modern Tech Stack**: Latest Android development practices

---

**Last Updated**: October 2025
**Maintained By**: Marc Geraldez with Claude Code assistance
**License**: MIT (Open Source)
