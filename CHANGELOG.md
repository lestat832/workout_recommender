# Changelog

All notable changes to the Workout Tracker app will be documented in this file.

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