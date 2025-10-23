# Android Architecture & Implementation Patterns

**When to load this reference:**
- Working on ViewModels, Compose UI, or MVVM architecture
- Implementing dependency injection with Hilt
- Setting up Room database or DAOs
- Creating navigation flows or state management

**Load command:** Uncomment `@.claude/references/android-patterns.md` in `.claude/CLAUDE.md`

---

## Tech Stack

### Core Technologies
- **Language**: Kotlin 1.9.21
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room (SQLite)
- **Dependency Injection**: Hilt (Dagger)
- **Image Loading**: Coil
- **Async Operations**: Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose
- **Serialization**: Kotlin Serialization
- **State Management**: DataStore Preferences
- **Build System**: Gradle with Kotlin DSL

## Project Structure

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

## MVVM Architecture Pattern

### Data Flow
1. **User Action** → ViewModel
2. **ViewModel** → Use Case (business logic)
3. **Use Case** → Repository (data layer)
4. **Repository** → Local Database / Remote API
5. **Response** → Flow back to UI via StateFlow/MutableState

### ViewModel Pattern
```kotlin
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val generateWorkoutUseCase: GenerateWorkoutUseCase,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun generateWorkout() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val workout = generateWorkoutUseCase()
            _uiState.value = UiState.Success(workout)
        }
    }
}
```

## Room Database Patterns

### Entity Example
```kotlin
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<MuscleGroup>,  // Type converters needed
    val workoutType: WorkoutType,
    val imageUrl: String?,
    val equipment: String?,
    val isActive: Boolean = true,
    val isUserCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### DAO Pattern
```kotlin
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isActive = 1")
    fun getAllActiveExercises(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
}
```

### Type Converters
```kotlin
class Converters {
    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        return value.split(",").map { MuscleGroup.valueOf(it) }
    }
}
```

## Hilt Dependency Injection

### Application Setup
```kotlin
@HiltAndroidApp
class FortisLupusApplication : Application()
```

### Module Pattern
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fortis_lupus_db"
        ).build()
    }

    @Provides
    fun provideExerciseDao(database: AppDatabase): ExerciseDao {
        return database.exerciseDao()
    }
}
```

### Repository Binding
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository
}
```

## Jetpack Compose Patterns

### State Management
```kotlin
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> WorkoutContent(uiState.data)
        is UiState.Error -> ErrorMessage(uiState.message)
    }
}
```

### Reusable Components
```kotlin
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExerciseClick(exercise) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Card content
    }
}
```

## Navigation Pattern

### Navigation Setup
```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen(navController) }
        composable("workout/{workoutId}") { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId")
            WorkoutScreen(workoutId, navController)
        }
    }
}
```

## DataStore Preferences

### Preferences Setup
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = complete
        }
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETE] ?: false
        }
}
```

## Coroutines & Flow Best Practices

### Repository Pattern with Flow
```kotlin
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout.toEntity())
    }
}
```

### Error Handling
```kotlin
suspend fun executeWithErrorHandling(
    onError: (String) -> Unit,
    action: suspend () -> Unit
) {
    try {
        action()
    } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
    }
}
```

## Build Configuration

### Gradle Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```
