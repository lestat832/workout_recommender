package com.workoutapp.presentation.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.presentation.viewmodel.OnboardingViewModel
import androidx.compose.ui.text.font.FontWeight

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()
    val selectedExercises by viewModel.selectedExercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    
    // Handle completion
    LaunchedEffect(isComplete) {
        if (isComplete) {
            onComplete()
        }
    }
    
    // Track which muscle groups are expanded
    val expandedGroups = remember { mutableStateMapOf<MuscleGroup, Boolean>() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Build Your Pack",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        Text(
            text = "Select exercises to strengthen your territory",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Temporary testing buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { viewModel.selectAllExercises() }) {
                Text("Select All")
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { viewModel.clearAllExercises() }) {
                Text("Clear All")
            }
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group exercises by primary muscle group and sort alphabetically within each group
            val groupedExercises = exercises
                .groupBy { it.muscleGroups.firstOrNull() ?: MuscleGroup.CHEST }
                .mapValues { entry -> 
                    entry.value.sortedBy { it.name }
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
                        selectedExercises = selectedExercises,
                        isExpanded = expandedGroups[muscleGroup] ?: false,
                        onToggleExpanded = { 
                            expandedGroups[muscleGroup] = !(expandedGroups[muscleGroup] ?: false)
                        },
                        onToggle = { exerciseId -> 
                            viewModel.toggleExerciseSelection(exerciseId) 
                        }
                    )
                }
            }
        }
        
        Button(
            onClick = {
                viewModel.saveSelectedExercises()
            },
            enabled = selectedExercises.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Join the Pack (${selectedExercises.size} selected)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.muscleGroupSection(
    muscleGroup: MuscleGroup,
    exercises: List<Exercise>,
    selectedExercises: Set<String>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit
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
                
                val selectedCount = exercises.count { it.id in selectedExercises }
                Text(
                    text = "$selectedCount / ${exercises.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // Exercises in this group (only show when expanded)
    if (isExpanded) {
        items(exercises) { exercise ->
            ExerciseSelectionCard(
                exercise = exercise,
                isSelected = exercise.id in selectedExercises,
                onToggle = { onToggle(exercise.id) }
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
fun ExerciseSelectionCard(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
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
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${exercise.muscleGroups.joinToString(", ") { it.name }} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}