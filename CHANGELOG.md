# Changelog

All notable changes to the Fortis Lupus app will be documented in this file.

## [Unreleased] - 2025-08-14

### Added
- **Strong App Import Feature**: Import workout history from Strong app CSV exports
  - Created comprehensive CSV parser for Strong's semicolon-delimited format
  - Exercise mapping system matches 50+ Strong exercises to existing exercises
  - Automatic creation of custom exercises for unmapped Strong exercises
  - Manual import via "Import Marc's Workouts (133)" button in debug menu
  - Weight conversion from kg to lbs with proper rounding
  - Supports multiple CSV imports with import status tracking
  - Import result dialog shows statistics (workouts, exercises, errors)
- **Import Debug Infrastructure**: Development testing with embedded CSV
  - 133 real workouts from Dec 2023 to Aug 2025 embedded in app
  - SharedPreferences tracking to prevent duplicate imports
  - BuildConfig flag for debug/release build differentiation
  - Manual import only - no automatic import on app launch for clean FTUE

### Changed
- **Workout Display Limit**: Increased from 5 to 50 recent workouts in home screen
- **Weight Display**: All weights now show as whole numbers (135 lbs vs 134.99973)
- **Shuffle Logic**: Fixed to filter by muscle groups instead of workout type
- **Import Architecture**: Separated manual import from automatic import logic
  - Added `importManually()` method to bypass auto-import checks
  - Ensures clean first-time user experience with empty app state

### Fixed
- **Weight Decimal Display**: Fixed persistent decimal display in workout history
- **Shuffle Button**: Now correctly shows exercises from same muscle group
- **Auto-Import Issue**: Prevented unwanted automatic import on app launch
- **Exercise Mapping**: Fixed compilation errors with entity transformations

### Technical
- **New Use Cases**: ImportWorkoutUseCase, ImportDebugDataUseCase
- **CSV Parser**: StrongCsvParser with semicolon delimiter support
- **Exercise Mapper**: Maps Strong exercise names to app exercises
- **Database Updates**: Support for bulk workout and exercise imports
- **Import State Management**: Loading, Success, Error states with UI feedback

## [Unreleased] - 2025-01-24

### Added
- **Multiple Muscle Group Selection**: Custom exercises can now target multiple muscle groups
  - Select chest AND legs, or any combination of muscle groups
  - Custom exercises appear in ALL selected muscle group sections
  - Enhanced UI with multi-selection FilterChips
- **Show All Exercises**: Removed workout type filtering in Add Exercise screen
  - Users can add ANY exercise to their workout regardless of Push/Pull categorization
  - Maintains suggested exercises but allows full flexibility

### Changed
- **Exercise Data Model**: Updated from single `muscleGroup` to `List<MuscleGroup>`
  - Database migration from v4 to v5 with proper schema transformation
  - Added MuscleGroupListConverter for Room database type conversion
  - Updated all repository implementations and ViewModels
- **Remove Difficulty System**: Eliminated all references to exercise difficulty
  - Removed Difficulty enum from both app and shared modules
  - Cleaned up UI to remove beginner/intermediate/advanced labels
  - Simplified exercise creation and display
- **Enhanced Exercise Display**: Multi-muscle group exercises show in relevant sections
  - Uses flatMap to expand exercises across muscle groups
  - Maintains distinct exercise instances per muscle group section
  - Improved grouping logic for better organization

### Fixed
- **Exercise Filtering Bug**: Fixed issue where exercises already in workout still appeared in Add Exercise screen
  - Fixed navigation parameter handling for empty exercise lists
  - Improved URL construction to handle edge cases
  - Added robust filtering logic to prevent duplicate exercise display
- **Compilation Errors**: Resolved all model transformation issues
  - Updated GenerateWorkoutUseCase for new muscle group structure
  - Fixed InitializeDatabaseUseCase parameter mapping
  - Updated WorkoutScreen muscle group display
  - Fixed ExerciseData.kt type mismatches
  - Corrected WorkoutRepositoryImpl constructor calls

### Technical
- **Database Migration**: MIGRATION_4_5 transforms single muscle group to list
- **Type Converters**: New MuscleGroupListConverter for Room database
- **Navigation Improvements**: Better parameter handling and edge case management
- **State Management**: Enhanced lifecycle observers for screen refresh
- **Data Consistency**: Comprehensive model updates across all layers

## [Unreleased] - 2025-01-21

### Added
- **Custom Exercise Creation**: Complete feature for creating custom exercises during workouts
  - Create custom exercises mid-workout via "Create Custom" button in AddExerciseScreen
  - Muscle group selection: Chest, Shoulders, Back, Biceps, Triceps, Legs, Core
  - Equipment options: None, Bodyweight, Dumbbell, Barbell, Cable, Machine, Other
  - Real-time exercise preview with wolf placeholder image
  - Automatic workout type assignment (Push/Pull) based on muscle group
  - Custom exercise validation with duplicate name prevention
  - "CUSTOM" badge distinguishes user-created exercises throughout app
  - Custom exercises appear in appropriate muscle group sections
- **Onboarding Persistence Fix**: Resolved critical issue where onboarding showed every app launch
  - Added DataStore preferences system for persistent app-level settings
  - Dynamic navigation: first launch → onboarding, subsequent → home screen
  - UserPreferencesRepository with proper dependency injection
  - Fixed race condition in onboarding completion flow
  - MainViewModel provides reactive onboarding status to navigation

### Changed
- **Database Schema**: Updated to version 4 with migration support
  - Added `isUserCreated` boolean field to distinguish custom exercises
  - Added `createdAt` timestamp field for custom exercise tracking
- **ExerciseRepository**: Extended with custom exercise management methods
  - `createCustomExercise()` for saving new custom exercises
  - `getCustomExerciseByName()` for duplicate validation
  - `getCustomExercises()` for retrieving user-created exercises
- **Navigation Architecture**: Enhanced with CreateExerciseScreen integration
  - Added "createExercise" route with proper parameter passing
  - Seamless navigation flow: Workout → Add Exercise → Create Custom → Back
- **AddExerciseScreen**: Enhanced with custom exercise creation capability
  - "Create Custom" button in top app bar for easy access
  - Custom exercises display with distinctive "CUSTOM" badge

### Fixed
- **Onboarding Loop**: Eliminated infinite onboarding screen on every app launch
- **Smart Cast Error**: Fixed Kotlin smart cast issue in CreateExerciseScreen LaunchedEffect
- **Async Operation Race Condition**: Proper completion handling in onboarding flow
- **Exercise Repository**: Updated all entity mapping to include new custom exercise fields

### Technical
- **DataStore Integration**: Added androidx.datastore:datastore-preferences dependency
- **Hilt Modules**: Created DataStoreModule for dependency injection
- **Database Migration**: MIGRATION_3_4 adds custom exercise support
- **ViewModels**: New CreateExerciseViewModel and MainViewModel
- **Repository Pattern**: UserPreferencesRepositoryImpl with DataStore backend
- **State Management**: Proper async operation handling with completion states

## [Unreleased] - 2025-01-20

### Added
- **Remove Exercise Feature**: Delete button on each exercise card with confirmation dialog
  - Red trash icon for clear visual indication
  - "Remove [Exercise Name] from workout?" confirmation
  - Smart logic: removing last exercise prompts to cancel entire workout
- **Cancel Workout Options**: Three-dot overflow menu in workout screen top bar
  - "Save Progress" - Mark workout as incomplete but preserve completed sets
  - "Discard" - Delete workout entirely with no saved data
  - "Continue" - Return to workout without changes
- **Working Add Exercise**: Fixed non-functional FAB button
  - Opens filtered exercise selection screen mid-workout
  - Shows only exercises matching current workout type (Push/Pull)
  - Excludes exercises already in workout and those in 7-day cooldown
  - Organized by muscle groups with collapsible sections
- **AddExerciseScreen**: New screen for mid-workout exercise selection
  - Familiar UI matching "Build Your Pack" onboarding
  - Grouped by muscle categories (Chest, Shoulders, Back, Biceps, Legs, Triceps, Core)
  - Alphabetical sorting within each group
  - Shows availability count per muscle group
- **Enhanced Navigation**: Proper screen flow for adding exercises
  - Callback-based navigation between workout and exercise selection
  - Seamless return to workout with selected exercise added

### Changed
- **Workout Data Model**: Added INCOMPLETE status for partially completed workouts
- **Repository Layer**: Added deleteWorkout and deleteWorkoutExercises methods
- **Navigation Architecture**: Updated to support mid-workout exercise addition
- **README Documentation**: Added comprehensive debug menu documentation
  - Instructions for accessing hidden date testing feature
  - Testing scenarios for workout alternation and exercise cooldowns
  - Clear guidance for developers and testers

### Fixed
- **Import Errors**: Resolved LocalNavController and lifecycle compose import issues
- **Navigation Dependencies**: Simplified navigation approach using callbacks
- **Workout State Management**: Proper handling of exercise addition/removal
- **Database Integrity**: Proper cleanup when deleting workouts and exercises

### Technical
- **New ViewModels**: AddExerciseViewModel for exercise selection logic
- **Enhanced WorkoutViewModel**: Methods for exercise management (add, remove, cancel)
- **Navigation Updates**: Support for passing data between workout screens
- **Database Schema**: Extended with delete operations and incomplete status

## [Unreleased] - 2025-01-13

### Added
- **Wolf Branding**: Complete rebrand to "Fortis Lupus" (Strength of the Wolf)
  - Custom wolf-themed app icon using DALL-E generated assets
  - Howling wolf splash screen with moon and stars
  - Wolf logo in app headers
  - Empty state wolf imagery
  - Wolf-themed color palette (wolf_charcoal, wolf_blue, moon_silver)
- **Exercise Organization**: Grouped exercises by muscle category in onboarding
  - Organized into: Chest, Shoulders, Back, Biceps, Legs, Triceps, Core
  - Alphabetical sorting within each muscle group
  - Collapsible sections with expand/collapse functionality
  - Selection count per muscle group
- **CORE Muscle Group**: Added new muscle group enum for core exercises
- **Set Completion Checkbox**: Added explicit checkbox for marking sets as completed

### Changed
- **App Name**: Changed from "Workout Tracker" to "Fortis Lupus"
- **UI Language**: Updated all strings to wolf-themed language
  - "Begin the Hunt" instead of "Start Workout"
  - "Pack History" instead of "Recent Workouts"
  - "Today's Hunt" instead of "Today's Workout"
  - "Build Your Pack" for exercise selection
  - "Alpha Training" (Push) and "Pack Strength" (Pull)
- **Onboarding Screen**: Exercises now grouped by muscle with collapsible sections
- **Theme Colors**: Updated to wolf-themed color scheme throughout

### Fixed
- **Set Tracking Bug**: Fixed issue where sets with unchanged values weren't saved
- **Icon Caching**: Resolved Android launcher icon cache issues
- **Missing Imports**: Added missing dp/sp imports in HomeScreen
- **Alpha Modifier**: Added missing import for alpha modifier

### Technical
- **Image Assets**: Created comprehensive DALL-E prompt guide for consistent branding
- **Adaptive Icons**: Properly configured adaptive icons with wolf_charcoal background
- **Resource Organization**: Added drawable-nodpi for image assets

## [Unreleased] - 2025-01-09

### Added
- **Splash Screen**: Added launch screen with "Workout Recommender" branding
- **Date Display**: Today's Workout card now shows the current date
- **Expandable Workout History**: Recent workouts can be expanded to show exercise details
- **Exercise Subtotals**: Shows total weight lifted per exercise in expanded view
- **Shuffle Exercise**: Added refresh button to swap exercises during workout
- **Add Exercise Button**: Floating action button to add more exercises to current workout
- **Debug Menu**: Tap on "Workout Tracker" title to access date offset for testing
- **Database Expansion**: Added 10 new exercises (total: 40 exercises)
  - Cable Crossover, Face Pull, Hammer Curl, Leg Extension
  - T-Bar Row, Bulgarian Split Squat, Arnold Press
  - Cable Lateral Raise, Hack Squat, Preacher Curl

### Changed
- **Complete Workout Button**: Moved from bottom bar to top app bar for better UX
- **Input Fields**: Improved decimal number handling in workout screen
- **Exercise Selection**: Added "Select All" and "Clear All" buttons for testing
- **Header Alignment**: Centered "Workout Tracker" title
- **Workout Label**: Changed "Next Workout" to "Today's Workout"

### Fixed
- **Icon References**: Fixed unresolved references (ExpandLess/More → KeyboardArrowUp/Down, Shuffle → Refresh)
- **Database Initialization**: Fixed issue where only 8 exercises were loading
- **Input Field Behavior**: Fixed decimal point display issue when entering whole numbers
- **Repository Method**: Fixed getSelectedExercises to use getUserActiveExercisesByType

### Technical
- **ViewModel Update**: WorkoutViewModel now extends AndroidViewModel for context access
- **Date Testing**: Added SharedPreferences-based date offset for testing future dates
- **UI Components**: Added AnimatedVisibility for smooth expand/collapse animations

## [0.1.0] - 2025-01-08

### Initial Release
- Basic workout tracking functionality
- Exercise selection from 30 exercises with images
- Push/Pull workout alternation
- Sets and reps tracking
- 7-day exercise cooldown period
- Material Design 3 UI