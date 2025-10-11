package com.workoutapp.presentation.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutapp.domain.model.Workout
import com.workoutapp.domain.model.WorkoutType
import com.workoutapp.presentation.viewmodel.HomeViewModel
import com.workoutapp.presentation.viewmodel.ImportState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.workoutapp.R
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    stravaAuthViewModel: com.workoutapp.presentation.settings.StravaAuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onStartWorkout: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val lastWorkout by viewModel.lastWorkout.collectAsState()
    val recentWorkouts by viewModel.recentWorkouts.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val stravaAuthState by stravaAuthViewModel.authState.collectAsState()

    var showDebugMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("debug_prefs", Context.MODE_PRIVATE)
    var testDateOffset by remember { mutableStateOf(prefs.getInt("date_offset", 0)) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // Multi-tap counter for debug menu
    var tapCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    
    // File picker for CSV import
    val csvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvContent = reader.use { it.readText() }
                    viewModel.importWorkouts(csvContent)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    )
    
    // Handle import state changes
    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                showImportDialog = true
            }
            is ImportState.Error -> {
                showImportDialog = true
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            tapCount++
                            if (tapCount >= 5) {
                                showDebugMenu = !showDebugMenu
                                tapCount = 0
                            }
                            // Reset tap count after 2 seconds
                            coroutineScope.launch {
                                delay(2000)
                                tapCount = 0
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_wolf_logo),
                            contentDescription = "Wolf Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            "FORTIS LUPUS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                },
                actions = {
                    // Strava connection status indicator
                    if (stravaAuthState?.isAuthenticated() == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🟠",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Strava",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }

                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        if (stravaAuthState?.isAuthenticated() == true) {
                            DropdownMenuItem(
                                text = { Text("Disconnect Strava") },
                                onClick = {
                                    showSettingsMenu = false
                                    stravaAuthViewModel.disconnect()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Connect to Strava") },
                                onClick = {
                                    showSettingsMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartWorkout,
                icon = { Icon(Icons.Default.Add, contentDescription = "Start Workout") },
                text = { Text("Begin the Hunt") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Debug menu for testing
            if (showDebugMenu) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Debug Menu (Testing Only)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Date Offset: $testDateOffset days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            Row {
                                TextButton(
                                    onClick = {
                                        testDateOffset = 0
                                        prefs.edit().putInt("date_offset", 0).apply()
                                    }
                                ) {
                                    Text("Reset")
                                }
                                
                                TextButton(
                                    onClick = {
                                        testDateOffset++
                                        prefs.edit().putInt("date_offset", testDateOffset).apply()
                                    }
                                ) {
                                    Text("+1 Day")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Import Marc's Workouts - Primary Action
                        Button(
                            onClick = {
                                viewModel.importMarcWorkouts()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = importState !is ImportState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Import",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                if (importState is ImportState.Loading) 
                                    "Importing Marc's Workouts..." 
                                else 
                                    "Import Marc's Workouts (133)"
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Import Strong Data section
                        Text(
                            "Import Other Strong Data",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                csvFilePicker.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = importState !is ImportState.Loading
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Import",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                if (importState is ImportState.Loading) 
                                    "Importing..." 
                                else 
                                    "Select CSV File to Import"
                            )
                        }
                        
                        // Show import status
                        when (importState) {
                            is ImportState.Loading -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {}
                        }
                        
                        // Auto-import status and reset
                        if (com.workoutapp.BuildConfig.ENABLE_DEBUG_DATA_IMPORT) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Auto-Import Status",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val isImported = viewModel.isDebugDataImported()
                            Text(
                                if (isImported) 
                                    "✓ Debug data has been auto-imported (132 workouts)" 
                                else 
                                    "Debug data not yet imported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resetDebugDataImport()
                                    },
                                    enabled = isImported
                                ) {
                                    Text("Reset Import Flag")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        viewModel.reimportDebugData()
                                    }
                                ) {
                                    Text("Force Re-import")
                                }
                            }
                        }
                    }
                }
            }
            
            NextWorkoutCard(lastWorkout, testDateOffset)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Pack History",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (recentWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wolf_howling_splash),
                            contentDescription = "Howling Wolf",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 16.dp)
                                .alpha(0.3f),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "The pack awaits...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Begin your first hunt below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentWorkouts) { workout ->
                        WorkoutHistoryCard(workout, viewModel)
                    }
                }
            }
        }
    }
    
    // Import Result Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                viewModel.resetImportState()
            },
            title = {
                Text(
                    text = when (importState) {
                        is ImportState.Success -> "Import Successful"
                        is ImportState.Error -> "Import Failed"
                        else -> "Import Status"
                    }
                )
            },
            text = {
                when (val state = importState) {
                    is ImportState.Success -> {
                        Column {
                            Text("Successfully imported your Strong workout data!")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Total workouts: ${state.result.totalWorkouts}")
                            Text("• Imported workouts: ${state.result.importedWorkouts}")
                            Text("• New exercises created: ${state.result.newExercises}")
                            Text("• Mapped exercises: ${state.result.mappedExercises}")
                            
                            if (state.result.errors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Some issues occurred:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                state.result.errors.take(3).forEach { error ->
                                    Text(
                                        "• $error",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    is ImportState.Error -> {
                        Text(state.message)
                    }
                    else -> {}
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        viewModel.resetImportState()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextWorkoutCard(lastWorkout: Workout?, dateOffset: Int = 0) {
    val nextWorkoutType = if (lastWorkout?.type == WorkoutType.PUSH) {
        WorkoutType.PULL
    } else {
        WorkoutType.PUSH
    }
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
    val today = calendar.time
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Hunt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = dateFormat.format(today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (nextWorkoutType) {
                    WorkoutType.PUSH -> "Alpha Training"
                    WorkoutType.PULL -> "Pack Strength"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = when (nextWorkoutType) {
                    WorkoutType.PUSH -> "Chest • Shoulders • Triceps"
                    WorkoutType.PULL -> "Legs • Back • Biceps"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryCard(workout: Workout, viewModel: HomeViewModel) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var isExpanded by remember { mutableStateOf(false) }
    val totalWeight = viewModel.calculateTotalWeight(workout)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed view - always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${workout.type.name} Workout",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateFormat.format(workout.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalWeight > 0) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%,.0f", totalWeight)} lbs total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Expanded view - shows exercise details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    workout.exercises.forEach { workoutExercise ->
                        ExerciseDetailRow(workoutExercise)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailRow(workoutExercise: com.workoutapp.domain.model.WorkoutExercise) {
    val exerciseTotal = workoutExercise.sets
        .filter { it.completed }
        .sumOf { (it.weight * it.reps).toDouble() }
        .toFloat()
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workoutExercise.exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (exerciseTotal > 0) {
                Text(
                    text = "${String.format("%,.0f", exerciseTotal)} lbs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        workoutExercise.sets.forEachIndexed { index, set ->
            if (set.completed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set ${index + 1}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${set.weight.toInt()} lbs × ${set.reps} reps",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}