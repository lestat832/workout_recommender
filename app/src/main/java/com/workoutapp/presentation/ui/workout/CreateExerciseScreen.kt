package com.workoutapp.presentation.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workoutapp.R
import com.workoutapp.domain.model.Exercise
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.presentation.viewmodel.CreateExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    onExerciseCreated: (Exercise) -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipment by remember { mutableStateOf("None") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CreateExerciseViewModel.UiState.Success -> {
                onExerciseCreated(state.exercise)
            }
            is CreateExerciseViewModel.UiState.Error -> {
                errorMessage = state.message
                showError = true
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Custom Exercise") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Name Input
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { 
                    exerciseName = it
                    showError = false
                },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && exerciseName.isBlank(),
                singleLine = true
            )
            
            // Muscle Group Selection
            Text(
                text = "Select Muscle Group",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Upper Body Muscle Groups
            Text(
                text = "Upper Body",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val upperBodyMuscles = listOf(
                    MuscleGroup.CHEST to "Chest",
                    MuscleGroup.SHOULDER to "Shoulders",
                    MuscleGroup.BACK to "Back",
                    MuscleGroup.BICEP to "Biceps",
                    MuscleGroup.TRICEP to "Triceps"
                )
                items(upperBodyMuscles) { (muscle, label) ->
                    FilterChip(
                        selected = selectedMuscleGroup == muscle,
                        onClick = { 
                            selectedMuscleGroup = muscle
                            showError = false
                        },
                        label = { Text(label) }
                    )
                }
            }
            
            // Lower Body & Core
            Text(
                text = "Lower Body & Core",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val lowerBodyMuscles = listOf(
                    MuscleGroup.LEGS to "Legs",
                    MuscleGroup.CORE to "Core"
                )
                items(lowerBodyMuscles) { (muscle, label) ->
                    FilterChip(
                        selected = selectedMuscleGroup == muscle,
                        onClick = { 
                            selectedMuscleGroup = muscle
                            showError = false
                        },
                        label = { Text(label) }
                    )
                }
            }
            
            // Equipment Selection
            Text(
                text = "Equipment (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val equipmentOptions = listOf(
                    "None", "Bodyweight", "Dumbbell", "Barbell", "Cable", "Machine", "Other"
                )
                items(equipmentOptions) { equipment ->
                    FilterChip(
                        selected = selectedEquipment == equipment,
                        onClick = { selectedEquipment = equipment },
                        label = { Text(equipment) }
                    )
                }
            }
            
            // Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExercisePreviewCard(
                        name = exerciseName.ifBlank { "Exercise Name" },
                        muscleGroup = selectedMuscleGroup,
                        equipment = selectedEquipment
                    )
                }
            }
            
            // Error Message
            if (showError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        when {
                            exerciseName.isBlank() -> {
                                errorMessage = "Please enter an exercise name"
                                showError = true
                            }
                            selectedMuscleGroup == null -> {
                                errorMessage = "Please select a muscle group"
                                showError = true
                            }
                            else -> {
                                viewModel.createExercise(
                                    name = exerciseName.trim(),
                                    muscleGroup = selectedMuscleGroup!!,
                                    equipment = selectedEquipment
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is CreateExerciseViewModel.UiState.Loading
                ) {
                    if (uiState is CreateExerciseViewModel.UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Exercise")
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePreviewCard(
    name: String,
    muscleGroup: MuscleGroup?,
    equipment: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wolf placeholder image
        AsyncImage(
            model = R.drawable.ic_wolf_logo,
            contentDescription = "Custom Exercise",
            modifier = Modifier
                .size(60.dp)
                .padding(4.dp),
            contentScale = ContentScale.Fit
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Custom exercise indicator
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
            Text(
                text = "${muscleGroup?.name ?: "Select Muscle"} • $equipment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "BEGINNER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}