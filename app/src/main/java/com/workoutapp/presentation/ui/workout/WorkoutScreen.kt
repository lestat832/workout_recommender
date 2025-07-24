package com.workoutapp.presentation.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.presentation.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel(),
    onWorkoutComplete: () -> Unit,
    onNavigateToAddExercise: (String, List<String>, (Exercise) -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Dialog states
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRemoveExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToRemove by remember { mutableStateOf<String?>(null) }
    var exerciseNameToRemove by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onWorkoutComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Hunt") },
                actions = {
                    val isEnabled = uiState.exercises.any { exercise ->
                        exercise.sets.any { it.completed }
                    }
                    TextButton(
                        onClick = { viewModel.completeWorkout() },
                        enabled = isEnabled
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Complete Workout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Complete")
                    }
                    
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cancel Workout") },
                                onClick = {
                                    showOverflowMenu = false
                                    showCancelDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    val workoutType = viewModel.getCurrentWorkoutType()?.name ?: "PUSH"
                    val currentExerciseIds = viewModel.getCurrentExerciseIds()
                    onNavigateToAddExercise(workoutType, currentExerciseIds) { selectedExercise ->
                        viewModel.addExerciseToWorkout(selectedExercise)
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Exercise") },
                text = { Text("Add Exercise") }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.exercises.size) { index ->
                        ExerciseCard(
                            exercise = uiState.exercises[index],
                            onAddSet = { viewModel.addSet(uiState.exercises[index].id) },
                            onRemoveSet = { setIndex ->
                                viewModel.removeSet(uiState.exercises[index].id, setIndex)
                            },
                            onUpdateSet = { setIndex, reps, weight ->
                                viewModel.updateSet(uiState.exercises[index].id, setIndex, reps, weight)
                            },
                            onToggleSetCompletion = { setIndex ->
                                viewModel.toggleSetCompletion(uiState.exercises[index].id, setIndex)
                            },
                            onShuffle = { viewModel.shuffleExercise(uiState.exercises[index].id) },
                            onRemoveExercise = {
                                exerciseToRemove = uiState.exercises[index].id
                                exerciseNameToRemove = uiState.exercises[index].exercise.name
                                showRemoveExerciseDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Cancel Workout Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout") },
            text = { Text("What would you like to do with your progress?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.saveWorkoutProgress()
                    }
                ) {
                    Text("Save Progress")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            viewModel.cancelWorkout()
                        }
                    ) {
                        Text("Discard")
                    }
                    TextButton(
                        onClick = { showCancelDialog = false }
                    ) {
                        Text("Continue")
                    }
                }
            }
        )
    }
    
    // Remove Exercise Dialog
    if (showRemoveExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveExerciseDialog = false },
            title = { Text("Remove Exercise") },
            text = { Text("Remove $exerciseNameToRemove from your workout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveExerciseDialog = false
                        exerciseToRemove?.let { exerciseId ->
                            // Check if this is the last exercise
                            if (uiState.exercises.size <= 1) {
                                showCancelDialog = true
                            } else {
                                viewModel.removeExercise(exerciseId)
                            }
                        }
                        exerciseToRemove = null
                        exerciseNameToRemove = ""
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRemoveExerciseDialog = false
                        exerciseToRemove = null
                        exerciseNameToRemove = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, Int, Float) -> Unit,
    onToggleSetCompletion: (Int) -> Unit,
    onShuffle: () -> Unit,
    onRemoveExercise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise Image - Removed for cleaner workout experience
            // exercise.exercise.imageUrl?.let { url ->
            //     AsyncImage(
            //         model = url,
            //         contentDescription = exercise.exercise.name,
            //         modifier = Modifier
            //             .fillMaxWidth()
            //             .height(200.dp)
            //             .padding(bottom = 16.dp),
            //         contentScale = ContentScale.Fit
            //     )
            // }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exercise.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Text(
                        text = "${exercise.exercise.muscleGroups.joinToString(", ") { it.name }} • ${exercise.exercise.equipment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Exercise",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Shuffle Exercise",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Set",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            exercise.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    onRemove = { onRemoveSet(index) },
                    onUpdate = { reps, weight -> onUpdateSet(index, reps, weight) },
                    onToggleCompletion = { onToggleSetCompletion(index) }
                )
            }
            
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetRow(
    setNumber: Int,
    set: Set,
    onRemove: () -> Unit,
    onUpdate: (Int, Float) -> Unit,
    onToggleCompletion: () -> Unit
) {
    fun formatWeight(value: Float): String {
        return if (value == 0f) "" 
        else if (value % 1 == 0f) value.toInt().toString()
        else value.toString()
    }
    
    // Use simple String state
    var weight by remember(set) { mutableStateOf(formatWeight(set.weight)) }
    var reps by remember(set) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    
    // Track if we should clear on next focus (for pre-filled values)
    var shouldClearWeight by remember(set) { mutableStateOf(set.weight > 0) }
    var shouldClearReps by remember(set) { mutableStateOf(set.reps > 0) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (set.completed) 0.9f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        OutlinedTextField(
            value = weight,
            onValueChange = { newValue ->
                // Clear the clear flag once user starts typing
                if (shouldClearWeight && newValue != weight) {
                    shouldClearWeight = false
                }
                // Allow only valid numeric input with optional decimal
                if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                    weight = newValue
                    val weightFloat = newValue.toFloatOrNull() ?: 0f
                    val repsInt = reps.toIntOrNull() ?: 0
                    onUpdate(repsInt, weightFloat)
                }
            },
            modifier = Modifier
                .weight(2f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && shouldClearWeight) {
                        // Clear the field on first focus if it has a pre-filled value
                        weight = ""
                        shouldClearWeight = false
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            placeholder = {
                Text(
                    text = "0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedTextField(
            value = reps,
            onValueChange = { newValue ->
                // Clear the clear flag once user starts typing
                if (shouldClearReps && newValue != reps) {
                    shouldClearReps = false
                }
                // Allow only valid numeric input
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    reps = newValue
                    val repsInt = newValue.toIntOrNull() ?: 0
                    val weightFloat = weight.toFloatOrNull() ?: 0f
                    onUpdate(repsInt, weightFloat)
                }
            },
            modifier = Modifier
                .weight(2f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && shouldClearReps) {
                        // Clear the field on first focus if it has a pre-filled value
                        reps = ""
                        shouldClearReps = false
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            placeholder = {
                Text(
                    text = "0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        )
        
        Checkbox(
            checked = set.completed,
            onCheckedChange = { onToggleCompletion() },
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}