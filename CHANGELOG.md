# Changelog

All notable changes to the Workout Tracker app will be documented in this file.

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