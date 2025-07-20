# Fortis Lupus (Workout Tracker App)

An Android application for tracking workouts with intelligent exercise recommendations, featuring a wolf-themed design and powerful workout planning capabilities.

## Features

- **Exercise Selection**: Choose from 30+ exercises with images and detailed instructions
- **Smart Workout Generation**: Alternating push/pull workouts with automatic exercise selection
- **Progress Tracking**: Track sets, reps, and weights for each exercise
- **Exercise Images**: Visual guides from the Free Exercise DB
- **7-Day Cooldown**: Prevents exercise repetition within a week
- **Offline Support**: All data stored locally using Room database

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room (SQLite)
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **Async**: Coroutines & Flow

## Screenshots

(Add screenshots here)

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run on emulator or physical device

## Project Structure

```
app/
├── data/           # Database, repositories
├── domain/         # Business logic, models
├── presentation/   # UI, ViewModels
└── di/            # Dependency injection
```

## Testing & Debug Features

### Date Testing Debug Menu

The app includes a hidden debug menu for testing date-dependent features without waiting for actual days to pass.

#### Accessing the Debug Menu
1. Tap on the **"FORTIS LUPUS"** title in the app header
2. A debug dialog will appear with date offset controls

#### Using the Debug Menu
- **Adjust Date**: Use the `-` and `+` buttons to change the date offset (in days)
- **Current Offset**: The center displays the current offset value
- **Apply Changes**: Click "Apply" to save and immediately update the app's date
- **Reset**: Set offset to 0 and click "Apply" to return to the current date

#### What the Date Offset Affects
- **Workout Type**: Alternates between Alpha Training (Push) and Pack Strength (Pull)
- **Exercise Cooldown**: 7-day cooldown period for exercises
- **Today's Hunt**: The date displayed on the main screen
- **Pack History**: Ordering and display of recent workouts

#### Testing Scenarios
- **Test Exercise Cooldown**: Set offset to +8 days to make all exercises available again
- **Test Workout Alternation**: Set offset to +1 to switch between Push/Pull workouts
- **Test Future State**: Set offset to +30 to see how the app behaves a month ahead
- **Test Workout History**: Create workouts with different date offsets to populate history

**Note**: This is a development/testing feature only and should not be included in production builds.

## Exercise Data

Exercises include images from the [Free Exercise DB](https://github.com/yuhonas/free-exercise-db), an open-source exercise dataset.

## Future Enhancements

- Weight progression suggestions based on performance
- Rest timer between sets
- Workout history and analytics
- Cloud sync capability
- Social features

## License

This project is open source and available under the MIT License.