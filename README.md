# Workout Tracker App

An Android application for tracking workouts with intelligent exercise recommendations.

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