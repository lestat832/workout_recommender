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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutapp.domain.model.WorkoutExercise
import com.workoutapp.domain.model.WorkoutFormat
import com.workoutapp.domain.usecase.RepPrescriber
import com.workoutapp.presentation.ui.components.FatigueWarningCaption
import com.workoutapp.presentation.viewmodel.ConditioningWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditioningWorkoutScreen(
    viewModel: ConditioningWorkoutViewModel = hiltViewModel(),
    onWorkoutComplete: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onWorkoutComplete()
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("This will discard the current workout.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelWorkout()
                    onNavigateBack()
                }) { Text("Cancel Workout") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Going") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isPreview) {
                            viewModel.cancelWorkout()
                            onNavigateBack()
                        } else {
                            showCancelDialog = true
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
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

        Column(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isPreview) {
                PreviewContent(
                    uiState = uiState,
                    onStart = { viewModel.beginWorkout() },
                    onShuffle = { exerciseId -> viewModel.shuffleExercise(exerciseId) }
                )
            } else {
                ActiveWorkoutContent(
                    uiState = uiState,
                    onIncrementRound = { viewModel.incrementRound() },
                    onTogglePause = { viewModel.togglePause() },
                    onComplete = { viewModel.completeWorkout() }
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(
    uiState: com.workoutapp.presentation.viewmodel.ConditioningUiState,
    onStart: () -> Unit,
    onShuffle: (String) -> Unit = {}
) {
    val sessionIds = uiState.exercises.map { it.exercise.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Exercises",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FatigueWarningCaption(warning = uiState.fatigueWarning)
            }
            items(uiState.exercises) { workoutExercise ->
                val repTarget = RepPrescriber.prescribe(
                    exerciseId = workoutExercise.exercise.id,
                    format = uiState.format,
                    sessionIds = sessionIds
                )
                ExerciseStationCard(
                    workoutExercise = workoutExercise,
                    repTarget = repTarget,
                    onShuffle = { onShuffle(workoutExercise.id) }
                )
            }

            // Coach's note
            if (uiState.coachNote.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Coach's Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.coachNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Begin the Hunt")
        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    uiState: com.workoutapp.presentation.viewmodel.ConditioningUiState,
    onIncrementRound: () -> Unit,
    onTogglePause: () -> Unit,
    onComplete: () -> Unit
) {
    val sessionIds = uiState.exercises.map { it.exercise.id }

    KeepScreenOn()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ClockDisplay(
            format = uiState.format,
            elapsedSeconds = uiState.elapsedSeconds,
            durationMinutes = uiState.durationMinutes ?: 20
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onTogglePause,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isPaused) "Resume" else "Pause")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.format == WorkoutFormat.AMRAP) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Rounds completed: ${uiState.rounds}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onIncrementRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOG ROUND",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Exercises",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val listState = rememberLazyListState()
        LaunchedEffect(uiState.currentStationIndex) {
            if (uiState.currentStationIndex >= 0) {
                listState.animateScrollToItem(uiState.currentStationIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FatigueWarningCaption(warning = uiState.fatigueWarning)
            }
            itemsIndexed(uiState.exercises) { index, workoutExercise ->
                val repTarget = RepPrescriber.prescribe(
                    exerciseId = workoutExercise.exercise.id,
                    format = uiState.format,
                    sessionIds = sessionIds
                )
                ExerciseStationCard(
                    workoutExercise = workoutExercise,
                    repTarget = repTarget,
                    isActive = index == uiState.currentStationIndex
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Complete Workout")
        }
    }
}

@Composable
private fun ExerciseStationCard(
    workoutExercise: WorkoutExercise,
    repTarget: String,
    isActive: Boolean = false,
    onShuffle: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workoutExercise.exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (onShuffle != null) {
                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Shuffle exercise",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = repTarget,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = workoutExercise.exercise.equipment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
