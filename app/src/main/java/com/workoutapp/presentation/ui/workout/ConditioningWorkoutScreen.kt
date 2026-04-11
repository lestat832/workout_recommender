package com.workoutapp.presentation.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.presentation.viewmodel.ConditioningWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditioningWorkoutScreen(
    viewModel: ConditioningWorkoutViewModel = hiltViewModel(),
    onWorkoutComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onWorkoutComplete()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val durationLabel = "${(uiState.durationMinutes ?: 20)}:00"
                    val formatLabel = when (uiState.format) {
                        WorkoutFormat.EMOM -> "EMOM $durationLabel"
                        WorkoutFormat.AMRAP -> "AMRAP $durationLabel"
                        WorkoutFormat.STRENGTH -> "STRENGTH"
                    }
                    Text(
                        text = formatLabel,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading workout...")
            }
            return@Scaffold
        }

        uiState.error?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ClockDisplay(
                format = uiState.format,
                elapsedSeconds = uiState.elapsedSeconds,
                durationMinutes = uiState.durationMinutes ?: 20
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.format == WorkoutFormat.AMRAP) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rounds: ${uiState.rounds}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { viewModel.incrementRound() }) {
                        Text("+1 Round")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Exercises",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.exercises) { workoutExercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = workoutExercise.exercise.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = workoutExercise.exercise.equipment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.completeWorkout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Complete Workout")
            }
        }
    }
}

@Composable
private fun ClockDisplay(
    format: WorkoutFormat,
    elapsedSeconds: Int,
    durationMinutes: Int
) {
    val totalSeconds = durationMinutes * 60
    val displaySeconds = when (format) {
        WorkoutFormat.EMOM -> (totalSeconds - elapsedSeconds).coerceAtLeast(0)
        else -> elapsedSeconds.coerceAtMost(totalSeconds)
    }
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val formatted = "%02d:%02d".format(minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatted,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = when (format) {
                    WorkoutFormat.EMOM -> "time remaining"
                    WorkoutFormat.AMRAP -> "time elapsed"
                    WorkoutFormat.STRENGTH -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
