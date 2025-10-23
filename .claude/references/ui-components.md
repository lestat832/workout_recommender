# UI Components & Wolf Theming

**When to load this reference:**
- Working on Compose UI components
- Implementing wolf-themed branding elements
- Building onboarding screens or FTUE flows
- Creating reusable composables
- Working on exercise selection UI
- Designing workout cards or history displays

**Load command:** Uncomment `@.claude/references/ui-components.md` in `.claude/CLAUDE.md`

---

## Wolf Theme Philosophy

### Brand Identity
**Fortis Lupus** (Latin: "Strength of the Wolf")

**Symbolism**:
- **Strength**: Individual power (Fortis = Strong)
- **Pack Mentality**: Training consistency and community
- **Adaptability**: Push/Pull alternation like hunting strategies
- **Endurance**: 7-day cooldown mimics recovery cycles in nature

### Thematic Language
```kotlin
object WolfTheme {
    // Workout Actions
    const val START_WORKOUT = "Begin the Hunt"
    const val SAVE_WORKOUT = "Save Progress"
    const val COMPLETE_WORKOUT = "Complete Hunt"
    const val DISCARD_WORKOUT = "Abandon Hunt"

    // Workout Types
    const val PUSH_WORKOUT = "Alpha Training"
    const val PULL_WORKOUT = "Pack Strength"

    // Screens
    const val HISTORY_TITLE = "Pack History"
    const val TODAY_WORKOUT = "Today's Hunt"
    const val EXERCISE_SELECTION = "Build Your Pack"

    // Empty States
    const val NO_WORKOUTS = "The pack awaits your first hunt"
    const val NO_EXERCISES = "Add exercises to build your pack"
}
```

## Color Palette

### Core Colors
```kotlin
// In theme/Color.kt
val wolf_charcoal = Color(0xFF2C2C2E)    // Dark gray, primary background
val wolf_blue = Color(0xFF0A84FF)        // Accent blue
val moon_silver = Color(0xFFE5E5EA)      // Light gray for text

// Additional theme colors
val wolf_dark = Color(0xFF1C1C1E)        // Darker variant
val wolf_gray = Color(0xFF48484A)        // Medium gray
val success_green = Color(0xFF34C759)    // Completion indicators
val warning_red = Color(0xFFFF3B30)      // Error states
```

### Material 3 Theme Integration
```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = wolf_blue,
    secondary = moon_silver,
    tertiary = wolf_gray,
    background = wolf_charcoal,
    surface = wolf_dark,
    onPrimary = Color.White,
    onSecondary = wolf_charcoal,
    onTertiary = moon_silver,
    onBackground = moon_silver,
    onSurface = moon_silver
)

@Composable
fun FortisLupusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
```

## Typography

### Font Styles
```kotlin
val Typography = Typography(
    // Display - Large headings
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),

    // Headings
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),

    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Labels
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

## Reusable Components

### WolfButton
```kotlin
@Composable
fun WolfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.PRIMARY
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (variant) {
                ButtonVariant.PRIMARY -> wolf_blue
                ButtonVariant.SECONDARY -> wolf_gray
                ButtonVariant.DANGER -> warning_red
            },
            contentColor = Color.White,
            disabledContainerColor = wolf_gray.copy(alpha = 0.5f),
            disabledContentColor = moon_silver.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER
}
```

### WorkoutCard
```kotlin
@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = wolf_dark
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (workout.type) {
                            WorkoutType.PUSH -> WolfTheme.PUSH_WORKOUT
                            WorkoutType.PULL -> WolfTheme.PULL_WORKOUT
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = wolf_blue
                    )
                    Text(
                        text = formatDate(workout.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = moon_silver.copy(alpha = 0.7f)
                    )
                }

                // Status badge
                WorkoutStatusBadge(status = workout.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WorkoutStat(
                    icon = Icons.Default.FitnessCenter,
                    label = "Exercises",
                    value = workout.exercises.size.toString()
                )

                WorkoutStat(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = formatDuration(workout.durationMinutes)
                )

                WorkoutStat(
                    icon = Icons.Default.TrendingUp,
                    label = "Volume",
                    value = formatVolume(workout.totalVolume)
                )
            }

            // Expandable exercise list
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = wolf_gray)
                Spacer(modifier = Modifier.height(16.dp))

                workout.exercises.forEach { exercise ->
                    ExerciseSummaryRow(exercise = exercise)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Expand/collapse button
            TextButton(
                onClick = onExpandToggle,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isExpanded) "Show Less" else "Show Details",
                    color = wolf_blue
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = wolf_blue
                )
            }
        }
    }
}
```

### ExerciseCard
```kotlin
@Composable
fun ExerciseCard(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                wolf_blue.copy(alpha = 0.2f)
            } else {
                wolf_dark
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, wolf_blue)
        } else {
            null
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise image
            AsyncImage(
                model = exercise.imageUrl,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.exercise_placeholder)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Exercise details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = moon_silver,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (exercise.isUserCreated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CustomBadge()
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Muscle groups
                Text(
                    text = exercise.muscleGroups.joinToString(", ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = moon_silver.copy(alpha = 0.7f)
                )

                // Equipment
                exercise.equipment?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = wolf_blue
                    )
                }
            }

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = wolf_blue,
                    uncheckedColor = wolf_gray
                )
            )
        }
    }
}
```

### CustomBadge
```kotlin
@Composable
fun CustomBadge() {
    Surface(
        color = warning_red.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, warning_red)
    ) {
        Text(
            text = "CUSTOM",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = warning_red,
            fontWeight = FontWeight.Bold
        )
    }
}
```

### WorkoutStatusBadge
```kotlin
@Composable
fun WorkoutStatusBadge(status: WorkoutStatus) {
    val (color, text) = when (status) {
        WorkoutStatus.ACTIVE -> wolf_blue to "In Progress"
        WorkoutStatus.COMPLETED -> success_green to "Completed"
        WorkoutStatus.INCOMPLETE -> warning_red to "Incomplete"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
```

## Onboarding Components

### GymSetupScreen
```kotlin
@Composable
fun GymSetupScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit
) {
    val gymName by viewModel.gymName.collectAsState()
    val selectedEquipment by viewModel.selectedEquipment.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "🏋️ Setup Your Gym",
            style = MaterialTheme.typography.headlineLarge,
            color = wolf_blue
        )

        Text(
            text = "Let us know what equipment you have access to",
            style = MaterialTheme.typography.bodyLarge,
            color = moon_silver.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Gym name input
        OutlinedTextField(
            value = gymName,
            onValueChange = { viewModel.setGymName(it) },
            label = { Text("Gym Name") },
            placeholder = { Text("Home Gym") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = wolf_blue,
                unfocusedBorderColor = wolf_gray,
                focusedLabelColor = wolf_blue,
                cursorColor = wolf_blue
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select Equipment (${selectedEquipment.size} selected)",
            style = MaterialTheme.typography.titleMedium,
            color = moon_silver
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Equipment grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(EquipmentType.ALL_EQUIPMENT) { equipment ->
                EquipmentSelectionCard(
                    equipment = equipment,
                    isSelected = equipment in selectedEquipment,
                    onToggle = { viewModel.toggleEquipmentSelection(equipment) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue button
        WolfButton(
            text = "Continue to Exercise Selection",
            onClick = onContinue,
            enabled = selectedEquipment.isNotEmpty() && gymName.isNotBlank()
        )
    }
}
```

### EquipmentSelectionCard
```kotlin
@Composable
fun EquipmentSelectionCard(
    equipment: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                wolf_blue.copy(alpha = 0.2f)
            } else {
                wolf_dark
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, wolf_blue)
        } else {
            BorderStroke(1.dp, wolf_gray)
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = getEquipmentIcon(equipment),
                    contentDescription = equipment,
                    tint = if (isSelected) wolf_blue else moon_silver,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = equipment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) wolf_blue else moon_silver,
                    textAlign = TextAlign.Center
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = wolf_blue,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun getEquipmentIcon(equipment: String): ImageVector {
    return when (equipment) {
        "Barbell" -> Icons.Default.FitnessCenter
        "Dumbbell" -> Icons.Default.FitnessCenter
        "Cable" -> Icons.Default.Cable
        "Machine" -> Icons.Default.Abc
        "Bodyweight" -> Icons.Default.AccessibilityNew
        "Bench" -> Icons.Default.Chair
        else -> Icons.Default.FitnessCenter
    }
}
```

### ExerciseSelectionScreen
```kotlin
@Composable
fun ExerciseSelectionScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()
    val selectedExercises by viewModel.selectedExercises.collectAsState()
    val exercisesByMuscle = exercises.groupBy { it.muscleGroups.firstOrNull() ?: MuscleGroup.CORE }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Surface(
            color = wolf_charcoal,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = WolfTheme.EXERCISE_SELECTION,
                    style = MaterialTheme.typography.headlineLarge,
                    color = wolf_blue
                )

                Text(
                    text = "${selectedExercises.size} exercises selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = moon_silver.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.selectAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All")
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }

        // Exercise list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            exercisesByMuscle.forEach { (muscleGroup, muscleExercises) ->
                item {
                    MuscleGroupSection(
                        muscleGroup = muscleGroup,
                        exercises = muscleExercises,
                        selectedExercises = selectedExercises,
                        onToggleExercise = { viewModel.toggleExerciseSelection(it) }
                    )
                }
            }
        }

        // Complete button
        Surface(
            color = wolf_charcoal,
            tonalElevation = 4.dp
        ) {
            WolfButton(
                text = "Complete Setup (${selectedExercises.size} selected)",
                onClick = onComplete,
                enabled = selectedExercises.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
    }
}
```

### MuscleGroupSection
```kotlin
@Composable
fun MuscleGroupSection(
    muscleGroup: MuscleGroup?,
    exercises: List<Exercise>,
    selectedExercises: Set<Long>,
    onToggleExercise: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        // Section header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = wolf_gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = muscleGroup?.displayName ?: "Other",
                    style = MaterialTheme.typography.titleMedium,
                    color = wolf_blue,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${exercises.count { it.id in selectedExercises }}/${exercises.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = moon_silver.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = wolf_blue
                    )
                }
            }
        }

        // Exercise list
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercises.forEach { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isSelected = exercise.id in selectedExercises,
                        onToggle = { onToggleExercise(exercise.id) }
                    )
                }
            }
        }
    }
}
```

## Utility Functions

### Formatting Helpers
```kotlin
object FormatUtils {
    fun formatDate(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                date.format(formatter)
            }
        }
    }

    fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
            }
        }
    }

    fun formatVolume(volume: Double): String {
        return when {
            volume < 1000 -> "${volume.toInt()} lbs"
            else -> {
                val thousands = volume / 1000.0
                String.format("%.1fK lbs", thousands)
            }
        }
    }

    fun formatWeight(weight: Double): String {
        return if (weight == 0.0) {
            "Bodyweight"
        } else {
            "${weight.toInt()} lbs"
        }
    }
}
```

## Empty States

### EmptyWorkoutHistory
```kotlin
@Composable
fun EmptyWorkoutHistory() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = wolf_gray.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = WolfTheme.NO_WORKOUTS,
            style = MaterialTheme.typography.titleLarge,
            color = moon_silver,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap \"${WolfTheme.START_WORKOUT}\" to begin your fitness journey",
            style = MaterialTheme.typography.bodyMedium,
            color = moon_silver.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
```

## Wolf Assets

### Asset Locations
- **App Icon**: `res/mipmap-*/ic_launcher.png` (adaptive icon)
- **Splash Screen**: Howling wolf with moon configuration
- **Exercise Placeholder**: Wolf silhouette for custom exercises
- **Empty State Icons**: Material Design Icons with wolf color palette

### DALL-E Generation Prompts
Located in `design/dalle_prompts.md`:
- App icon: "Minimalist wolf head icon, howling at moon, monochrome, flat design"
- Splash screen: "Howling wolf silhouette against full moon with stars, dramatic lighting"
- Empty state: "Simple wolf paw print icon, minimal line art"

## Accessibility

### Content Descriptions
```kotlin
// Always provide content descriptions for icons
Icon(
    imageVector = Icons.Default.FitnessCenter,
    contentDescription = "Exercise icon",
    tint = wolf_blue
)

// Mark decorative images
AsyncImage(
    model = imageUrl,
    contentDescription = null,  // Decorative
    modifier = Modifier.size(40.dp)
)

// Provide context for interactive elements
Button(
    onClick = { /* action */ },
    modifier = Modifier.semantics {
        contentDescription = "Begin workout session"
    }
) {
    Text("Begin the Hunt")
}
```

### Touch Targets
```kotlin
// Ensure minimum 48.dp touch targets
IconButton(
    onClick = { /* action */ },
    modifier = Modifier.size(48.dp)  // Minimum touch target
) {
    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
}
```

### Color Contrast
- All text on `wolf_charcoal` background meets WCAG AA standards
- `wolf_blue` accent has sufficient contrast for interactive elements
- `moon_silver` text provides readable contrast ratios
