package com.workoutapp.presentation.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.presentation.viewmodel.AddExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    workoutType: String,
    currentExerciseIds: List<String>,
    onExerciseSelected: (Exercise) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCreateExercise: () -> Unit,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Track which muscle groups are expanded
    val expandedGroups = remember { mutableStateMapOf<MuscleGroup, Boolean>() }
    
    // Reload exercises whenever parameters change, including when returning to screen
    LaunchedEffect(workoutType, currentExerciseIds) {
        viewModel.loadAvailableExercises(workoutType, currentExerciseIds)
    }
    
    // Also reload when screen becomes visible (user returns from another screen)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAvailableExercises(workoutType, currentExerciseIds)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onNavigateToCreateExercise
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Custom")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No additional exercises available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All eligible exercises are either in your current workout or in cooldown period",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Create entries for each muscle group an exercise belongs to
                val expandedExercises = exercises.flatMap { exercise ->
                    exercise.muscleGroups.map { muscleGroup ->
                        muscleGroup to exercise
                    }
                }
                
                // Group by muscle group and remove duplicates, then sort alphabetically
                val groupedExercises = expandedExercises
                    .groupBy { it.first }
                    .mapValues { entry -> 
                        entry.value.map { it.second }.distinct().sortedBy { it.name }
                    }
                
                // Display in the specified order
                val muscleGroupOrder = listOf(
                    MuscleGroup.CHEST,
                    MuscleGroup.SHOULDER,
                    MuscleGroup.BACK,
                    MuscleGroup.BICEP,
                    MuscleGroup.LEGS,
                    MuscleGroup.TRICEP,
                    MuscleGroup.CORE
                )
                
                muscleGroupOrder.forEach { muscleGroup ->
                    val exercisesInGroup = groupedExercises[muscleGroup] ?: emptyList()
                    if (exercisesInGroup.isNotEmpty()) {
                        muscleGroupSection(
                            muscleGroup = muscleGroup,
                            exercises = exercisesInGroup,
                            isExpanded = expandedGroups[muscleGroup] ?: false,
                            onToggleExpanded = { 
                                expandedGroups[muscleGroup] = !(expandedGroups[muscleGroup] ?: false)
                            },
                            onExerciseSelected = onExerciseSelected
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.muscleGroupSection(
    muscleGroup: MuscleGroup,
    exercises: List<Exercise>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    // Section header
    item {
        Card(
            onClick = onToggleExpanded,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = when (muscleGroup) {
                            MuscleGroup.CHEST -> "Chest"
                            MuscleGroup.SHOULDER -> "Shoulders"
                            MuscleGroup.BACK -> "Back"
                            MuscleGroup.BICEP -> "Biceps"
                            MuscleGroup.LEGS -> "Legs"
                            MuscleGroup.TRICEP -> "Triceps"
                            MuscleGroup.CORE -> "Core"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${exercises.size} available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // Exercises in this group (only show when expanded)
    if (isExpanded) {
        items(exercises) { exercise ->
            AddExerciseCard(
                exercise = exercise,
                onExerciseSelected = onExerciseSelected
            )
        }
    }
    
    // Add spacing after each section
    item {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseCard(
    exercise: Exercise,
    onExerciseSelected: (Exercise) -> Unit
) {
    Card(
        onClick = { onExerciseSelected(exercise) },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise Image
            exercise.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = exercise.name,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Exercise Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (exercise.isUserCreated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "CUSTOM",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = "${exercise.muscleGroups.joinToString(", ") { it.name }} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}