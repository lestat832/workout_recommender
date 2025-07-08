package com.workoutapp.presentation.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workoutapp.domain.model.Set
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.presentation.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel(),
    onWorkoutComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onWorkoutComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout") }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { viewModel.completeWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.exercises.any { exercise ->
                        exercise.sets.any { it.completed }
                    }
                ) {
                    Text("Complete Workout")
                }
            }
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, Int, Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise Image
            exercise.exercise.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = exercise.exercise.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                text = "${exercise.exercise.muscleGroup.name} • ${exercise.exercise.equipment}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            exercise.sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    onRemove = { onRemoveSet(index) },
                    onUpdate = { reps, weight -> onUpdateSet(index, reps, weight) }
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
    onUpdate: (Int, Float) -> Unit
) {
    var weight by remember(set) { mutableStateOf(set.weight.toString()) }
    var reps by remember(set) { mutableStateOf(set.reps.toString()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = weight,
            onValueChange = { newValue ->
                weight = newValue
                val weightFloat = newValue.toFloatOrNull() ?: 0f
                val repsInt = reps.toIntOrNull() ?: 0
                onUpdate(repsInt, weightFloat)
            },
            modifier = Modifier.weight(2f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedTextField(
            value = reps,
            onValueChange = { newValue ->
                reps = newValue
                val repsInt = newValue.toIntOrNull() ?: 0
                val weightFloat = weight.toFloatOrNull() ?: 0f
                onUpdate(repsInt, weightFloat)
            },
            modifier = Modifier.weight(2f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}