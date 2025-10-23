# Fortis Lupus - Project Context

> **Lean core file with on-demand reference loading**

---

## 📱 Project Identity

**Fortis Lupus** (Latin: "Strength of the Wolf")

**Platform**: Android workout tracking app with intelligent exercise recommendations

**Philosophy**: Strength through smart training, symbolized by the wolf pack

**Tech Stack**: Kotlin 1.9.21 • Jetpack Compose • MVVM + Clean Architecture • Room Database (v7) • Hilt DI

---

## 🎯 Core Features

### Smart Workout System
- **Push/Pull Alternation**: "Alpha Training" (Push) ↔ "Pack Strength" (Pull)
- **7-Day Exercise Cooldown**: Prevents muscle fatigue, ensures recovery
- **Equipment Filtering**: Workouts adapt to available gym equipment
- **Multi-Muscle Group Support**: Exercises target multiple muscle groups

### Exercise Library
- **873+ Exercises** from Free Exercise DB
- **Custom Exercise Creation**: User-defined exercises with multi-muscle selection
- **Smart Equipment Matching**: Barbell includes variants (EZ Bar, Trap Bar, etc.)
- **Visual Instructions**: Images from GitHub CDN

### Data Management
- **Strong App Import**: CSV import with exercise mapping and auto-creation
- **Workout Tracking**: Set/rep/weight logging with real-time progress
- **Exercise Shuffling**: Swap exercises mid-workout
- **Workout History**: Expandable workout cards with detailed stats

### Strava Integration (Phase 5 Complete)
- **One-Way Sync**: Workout App → Strava
- **Auto-Sync on Completion**: Immediate sync when workout marked complete
- **OAuth Authentication**: Secure Strava account connection
- **Detailed Activity Format**: Emoji-rich descriptions with volume/duration stats
- **Sync Queue System**: Offline queue with retry mechanism

### Gym Equipment System (Phase 5 Complete)
- **Gym Setup**: Equipment selection during FTUE onboarding
- **Smart Matching**: Equipment variants (Barbell includes EZ Bar, Trap Bar, etc.)
- **Equipment Filtering**: Workout generation respects available equipment
- **Multiple Gym Support**: Infrastructure ready for future Phase 6 expansion
- **Default Gym**: Concept implemented with "Home Gym" default

### Wolf-Themed Branding
- **Custom Assets**: DALL-E generated wolf icon and splash screen
- **Color Palette**: wolf_charcoal, wolf_blue, moon_silver
- **Thematic Language**: "Begin the Hunt", "Pack History", "Alpha Training"

---

## 🏗️ Architecture

### Project Structure
```
app/
├── data/          # Room entities, DAOs, repositories
├── domain/        # Business models, use cases, interfaces
├── presentation/  # Compose screens, ViewModels, components
└── di/            # Hilt modules
```

### Key Patterns
- **MVVM**: ViewModel → Use Case → Repository → Room Database
- **Clean Architecture**: Separation of concerns, dependency inversion
- **Reactive**: Coroutines & Flow for async operations
- **State Management**: StateFlow for UI state, DataStore for preferences

### Database (Room v7)
- **Entities**: Exercise, Workout, WorkoutExercise, Gym, Set
- **Current Migration**: v6 → v7 (added Gym support)
- **Type Converters**: MuscleGroup lists, Date serialization

---

## 🔮 Current Status

### ✅ Completed (Phase 5)
- Gym equipment selection and filtering
- Smart equipment matching (variants)
- Equipment-first workout generation
- FTUE gym setup (two-step onboarding)
- Strava data mapping and description formatting
- Database migration 6 → 7

### 🔄 In Progress
- Home screen gym selector (Phase 6)
- Gym management UI (Add/Edit/Delete)
- Multiple gym switching

### 📋 Planned
- Workout analytics dashboard
- Progress tracking (PR history)
- Rest timer between sets
- Cloud backup/sync

---

## 📚 Reference Documentation

**Smart Loading**: Tell Claude what you're working on, and relevant references will be loaded automatically.

**Available References** (load on-demand by uncommenting):

### Core Implementation Patterns
<!-- @.claude/references/android-patterns.md -->
**When to load**: Working on ViewModels, Compose UI, Room database, Hilt DI, MVVM architecture

<!-- @.claude/references/workout-domain.md -->
**When to load**: Working on workout generation, exercise cooldown logic, Push/Pull alternation, muscle group targeting

### Feature-Specific Patterns
<!-- @.claude/references/strava-integration.md -->
**When to load**: Working on Strava OAuth, sync queue, activity formatting, API integration

<!-- @.claude/references/gym-equipment.md -->
**When to load**: Working on gym setup, equipment filtering, smart matching, FTUE onboarding

<!-- @.claude/references/data-import.md -->
**When to load**: Working on CSV import, Strong app migration, exercise mapping, weight conversion

### UI & Design
<!-- @.claude/references/ui-components.md -->
**When to load**: Working on Compose UI, wolf theming, onboarding screens, reusable components

---

## 🛠️ Development Setup

### Build Requirements
- Android Studio Arctic Fox+
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- JVM Target: 17

### Local Configuration
Create `local.properties` (optional, for Strava):
```properties
STRAVA_CLIENT_ID=your_client_id
STRAVA_CLIENT_SECRET=your_client_secret
```

### Quick Start
```bash
# Install debug build
./gradlew installDebug

# View logs
adb logcat | grep WorkoutApp

# Uninstall
adb uninstall com.workoutapp
```

### Debug Features
- **Date Offset Menu**: Tap "FORTIS LUPUS" title → test cooldown/alternation
- **Import Debug Menu**: Three-dot menu → "Import Marc's Workouts" (133 workouts)

---

## 📝 Important Files

### Documentation
- `CHANGELOG.md` - Version history
- `STRAVA_SYNC_SPEC.md` - Strava technical spec
- `claudedocs/` - Session documentation and backups

### Key Implementation
- `app/src/main/java/com/workoutapp/domain/usecase/GenerateWorkoutUseCase.kt` - Workout generation algorithm
- `app/src/main/java/com/workoutapp/data/local/AppDatabase.kt` - Database setup & migrations
- `app/src/main/java/com/workoutapp/presentation/screens/` - Compose UI screens

---

## 🐺 Wolf Theme Language

```
Start Workout → "Begin the Hunt"
Save Progress → "Save Progress"
Complete Workout → "Complete Hunt"
Workout History → "Pack History"
Push Workouts → "Alpha Training"
Pull Workouts → "Pack Strength"
Exercise Selection → "Build Your Pack"
```

---

## 🚨 Known Context

### Recent Changes (Latest Session)
- Phase 5: Gym equipment feature complete (database v7)
- Phase 5: Strava data mapping and formatting complete
- Build errors fixed: Set type conflicts, Equipment import issues
- Database migration resolved with fresh install

### Active Work Areas
- Phase 6: Home screen gym selector UI
- Phase 6: Gym management functionality

### Technical Debt
- No automated tests (manual testing only)
- Limited error handling
- No offline-first sync for ExerciseDB initial load

---

## 📞 Quick Reference

**Project Guide Backup**: `claudedocs/FORTIS_LUPUS_PROJECT_GUIDE.md.backup`

**Git Workflow**: Feature branches only, never commit on main/master

**Commit Format**:
```
type: brief description

Detailed explanation

🤖 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude <noreply@anthropic.com>
```

**Types**: feat, fix, docs, refactor, test, chore

---

**Last Updated**: October 2025
**Maintained By**: Marc Geraldez with Claude Code
**Database Version**: 7
**Build Version**: 1.0 (pre-release)
