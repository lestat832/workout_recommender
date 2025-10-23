# Gym Equipment Filtering System

**When to load this reference:**
- Working on gym setup or equipment selection
- Implementing equipment-based filtering
- Building gym management features
- Debugging smart equipment matching
- Working on FTUE gym setup flow

**Load command:** Uncomment `@.claude/references/gym-equipment.md` in `.claude/CLAUDE.md`

---

## Feature Overview

**Purpose**: Filter workout exercises based on gym's available equipment

**Key Capabilities**:
- Set up gym equipment during FTUE onboarding
- Smart equipment matching (Barbell includes variants)
- Equipment filtering FIRST in workout generation
- Multiple gym support with default gym concept
- Bodyweight exercises always included

**Database Version**: 6 → 7 migration

## Equipment Types

### Available Equipment
```kotlin
object EquipmentType {
    const val BARBELL = "Barbell"
    const val DUMBBELL = "Dumbbell"
    const val CABLE = "Cable"
    const val MACHINE = "Machine"
    const val BODYWEIGHT = "Bodyweight"
    const val BENCH = "Bench"
    const val SMITH_MACHINE = "Smith Machine"
    const val KETTLEBELL = "Kettlebell"
    const val RESISTANCE_BAND = "Resistance Band"
    const val NONE = "None"
    const val OTHER = "Other"

    val ALL_EQUIPMENT = listOf(
        BARBELL, DUMBBELL, CABLE, MACHINE,
        BODYWEIGHT, BENCH, SMITH_MACHINE,
        KETTLEBELL, RESISTANCE_BAND, NONE, OTHER
    )
}
```

## Smart Equipment Matching

### Variant Sets
```kotlin
// Barbell includes variants
val BARBELL_VARIANTS = setOf(
    "Barbell", "Squat Bar", "Hex Bar",
    "Trap Bar", "EZ Bar", "Olympic Barbell"
)

// Dumbbell includes variants
val DUMBBELL_VARIANTS = setOf(
    "Dumbbell", "Dumbbells", "Adjustable Dumbbell"
)

// Bodyweight (always included)
val BODYWEIGHT_VARIANTS = setOf(
    "Bodyweight", "Body Only", "None", "No Equipment"
)
```

### Matching Logic
```kotlin
fun matches(exerciseEquipment: String, gymEquipment: String): Boolean {
    // Bodyweight exercises always match
    if (exerciseEquipment in BODYWEIGHT_VARIANTS) return true

    // Exact match (case-insensitive)
    if (exerciseEquipment.equals(gymEquipment, ignoreCase = true)) return true

    // Variant matching
    return when (gymEquipment) {
        BARBELL -> exerciseEquipment in BARBELL_VARIANTS
        DUMBBELL -> exerciseEquipment in DUMBBELL_VARIANTS
        BODYWEIGHT -> exerciseEquipment in BODYWEIGHT_VARIANTS
        // ... other variants
        else -> false
    }
}

fun canPerformExercise(
    exerciseEquipment: String,
    availableEquipment: List<String>
): Boolean {
    // Bodyweight exercises can always be performed
    if (exerciseEquipment in BODYWEIGHT_VARIANTS) return true

    // Check if any gym equipment matches
    return availableEquipment.any { gymEquipment ->
        matches(exerciseEquipment, gymEquipment)
    }
}
```

## Database Schema

### Gym Entity
```kotlin
@Entity(tableName = "gyms")
data class GymEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val equipmentList: String,  // Comma-separated
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Gym {
        return Gym(
            id = id,
            name = name,
            equipmentList = equipmentList.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() },
            isDefault = isDefault,
            createdAt = createdAt
        )
    }
}
```

### Workout Entity (Updated)
```kotlin
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val date: Date,
    val type: WorkoutType,
    val status: WorkoutStatus,
    val gymId: Long? = null  // NEW: Reference to gym
)
```

### Migration 6 → 7
```kotlin
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create gyms table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gyms (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                equipmentList TEXT NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """)

        // Add indexes
        db.execSQL("CREATE INDEX index_gyms_isDefault ON gyms(isDefault)")

        // Add gymId to workouts
        db.execSQL("ALTER TABLE workouts ADD COLUMN gymId INTEGER")
        db.execSQL("CREATE INDEX index_workouts_gymId ON workouts(gymId)")

        // Create default "Home Gym" with all equipment
        val allEquipment = "Barbell,Dumbbell,Cable,Machine,Bodyweight,Bench,Smith Machine,Kettlebell,Resistance Band,None,Other"
        val currentTime = System.currentTimeMillis()
        db.execSQL("""
            INSERT INTO gyms (name, equipmentList, isDefault, createdAt)
            VALUES ('Home Gym', '$allEquipment', 1, $currentTime)
        """)
    }
}
```

## Gym Domain Model

```kotlin
data class Gym(
    val id: Long = 0,
    val name: String,
    val equipmentList: List<String>,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_GYM_NAME = "Home Gym"

        fun createDefaultGym(): Gym {
            return Gym(
                name = DEFAULT_GYM_NAME,
                equipmentList = EquipmentType.ALL_EQUIPMENT,
                isDefault = true
            )
        }
    }

    fun canPerformExercise(exerciseEquipment: String): Boolean {
        return EquipmentType.canPerformExercise(
            exerciseEquipment,
            equipmentList
        )
    }
}
```

## Repository Pattern

### GymRepository
```kotlin
interface GymRepository {
    fun getAllGymsFlow(): Flow<List<Gym>>
    suspend fun getAllGyms(): List<Gym>
    suspend fun getGymById(gymId: Long): Gym?
    suspend fun getDefaultGym(): Gym?
    fun getDefaultGymFlow(): Flow<Gym?>
    suspend fun insertGym(gym: Gym): Long
    suspend fun updateGym(gym: Gym)
    suspend fun deleteGym(gym: Gym)
    suspend fun setDefaultGym(gymId: Long)
    suspend fun hasGyms(): Boolean
}
```

## Use Cases

### CreateGymUseCase
```kotlin
class CreateGymUseCase @Inject constructor(
    private val gymRepository: GymRepository
) {
    suspend operator fun invoke(
        name: String,
        equipmentList: List<String>,
        setAsDefault: Boolean = false
    ): Long {
        require(name.isNotBlank()) { "Gym name cannot be blank" }
        require(equipmentList.isNotEmpty()) { "Must have equipment" }

        val gym = Gym(
            name = name.trim(),
            equipmentList = equipmentList,
            isDefault = setAsDefault
        )

        val gymId = gymRepository.insertGym(gym)

        if (setAsDefault) {
            gymRepository.setDefaultGym(gymId)
        }

        return gymId
    }
}
```

### DeleteGymUseCase
```kotlin
class DeleteGymUseCase @Inject constructor(
    private val gymRepository: GymRepository,
    private val getAllGymsUseCase: GetAllGymsUseCase
) {
    suspend operator fun invoke(gym: Gym) {
        val allGyms = getAllGymsUseCase.get()

        // Prevent deletion of last gym
        require(allGyms.size > 1) {
            "Cannot delete the only gym"
        }

        gymRepository.deleteGym(gym)

        // If deleted gym was default, set another as default
        if (gym.isDefault) {
            val remainingGyms = getAllGymsUseCase.get()
            if (remainingGyms.isNotEmpty()) {
                gymRepository.setDefaultGym(remainingGyms.first().id)
            }
        }
    }
}
```

## Workout Generation Integration

### Updated GenerateWorkoutUseCase
```kotlin
suspend operator fun invoke(): List<Exercise> {
    // 1. Get default gym for equipment filtering
    val defaultGym = gymRepository.getDefaultGym()

    // 2. Determine workout type
    val lastWorkout = workoutRepository.getLastWorkout()
    val workoutType = if (lastWorkout?.type == WorkoutType.PUSH) {
        WorkoutType.PULL
    } else {
        WorkoutType.PUSH
    }

    // 3. Get exercises from last 7 days (cooldown)
    val recentExerciseIds = workoutRepository.getExerciseIdsFromLastWeek()

    // 4. Filter available exercises
    // PRIORITY: Equipment → Type → Cooldown → Muscle Groups
    val availableExercises = exerciseRepository
        .getUserActiveExercisesByType(workoutType)
        .filter { exercise ->
            // Equipment filter FIRST
            defaultGym?.let { gym ->
                EquipmentType.canPerformExercise(
                    exercise.equipment,
                    gym.equipmentList
                )
            } ?: true  // If no gym, include all
        }
        .filterNot { it.id in recentExerciseIds }

    // 5. Select exercises by muscle group
    // ... (same as before)
}
```

## FTUE Onboarding Flow

### Two-Step Process

**Step 1: Gym Setup**
```kotlin
@Composable
fun GymSetupStep(viewModel: OnboardingViewModel) {
    val gymName by viewModel.gymName.collectAsState()
    val selectedEquipment by viewModel.selectedEquipment.collectAsState()

    Column {
        // Gym name input
        OutlinedTextField(
            value = gymName,
            onValueChange = { viewModel.setGymName(it) },
            label = { Text("Gym Name") }
        )

        // Equipment grid (2 columns)
        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
            items(EquipmentType.ALL_EQUIPMENT) { equipment ->
                EquipmentSelectionCard(
                    equipment = equipment,
                    isSelected = equipment in selectedEquipment,
                    onToggle = { viewModel.toggleEquipmentSelection(equipment) }
                )
            }
        }

        // Continue button
        Button(
            onClick = { viewModel.proceedToExerciseSelection() },
            enabled = selectedEquipment.isNotEmpty() && gymName.isNotBlank()
        ) {
            Text("Continue (${selectedEquipment.size} selected)")
        }
    }
}
```

**Step 2: Exercise Selection** (filtered by equipment)
```kotlin
private fun loadExercises() {
    viewModelScope.launch {
        exerciseRepository.getAllExercises().collect { exerciseList ->
            // Filter by selected equipment
            val filteredExercises = exerciseList.filter { exercise ->
                EquipmentType.canPerformExercise(
                    exercise.equipment,
                    _selectedEquipment.value.toList()
                )
            }
            _exercises.value = filteredExercises
        }
    }
}
```

### OnboardingViewModel
```kotlin
enum class OnboardingStep {
    GYM_SETUP,
    EXERCISE_SELECTION
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val createGymUseCase: CreateGymUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.GYM_SETUP)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _gymName = MutableStateFlow("Home Gym")
    val gymName: StateFlow<String> = _gymName.asStateFlow()

    private val _selectedEquipment = MutableStateFlow<Set<String>>(emptySet())
    val selectedEquipment: StateFlow<Set<String>> = _selectedEquipment.asStateFlow()

    fun toggleEquipmentSelection(equipment: String) {
        _selectedEquipment.value = if (equipment in _selectedEquipment.value) {
            _selectedEquipment.value - equipment
        } else {
            _selectedEquipment.value + equipment
        }
    }

    fun proceedToExerciseSelection() {
        viewModelScope.launch {
            // Create gym
            createGymUseCase(
                name = _gymName.value,
                equipmentList = _selectedEquipment.value.toList(),
                setAsDefault = true
            )

            // Move to next step
            _currentStep.value = OnboardingStep.EXERCISE_SELECTION
            loadExercises()
        }
    }
}
```

## Future Enhancements (Phase 6)

**TODO - Home Screen**:
- Add gym selector dropdown above "Begin the Hunt"
- Switch between gyms
- Gym management UI (Add/Edit/Delete)

**TODO - Custom Equipment**:
- Allow users to add custom equipment types
- Support for specialized equipment (e.g., "Landmine", "Battle Ropes")
