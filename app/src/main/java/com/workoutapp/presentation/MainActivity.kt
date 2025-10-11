package com.workoutapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workoutapp.presentation.ui.home.HomeScreen
import com.workoutapp.presentation.ui.onboarding.OnboardingScreen
import com.workoutapp.presentation.ui.theme.WorkoutAppTheme
import com.workoutapp.presentation.ui.workout.WorkoutScreen
import com.workoutapp.presentation.ui.workout.AddExerciseScreen
import com.workoutapp.presentation.ui.workout.CreateExerciseScreen
import com.workoutapp.presentation.settings.StravaAuthViewModel
import com.workoutapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val intentData = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle initial intent
        intentData.value = intent

        setContent {
            WorkoutAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkoutNavigation(intentState = intentData)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update the state to trigger recomposition
        intentData.value = intent
    }
}

@Composable
fun WorkoutNavigation(
    intentState: MutableState<Intent?>,
    mainViewModel: MainViewModel = hiltViewModel(),
    stravaAuthViewModel: StravaAuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isOnboardingComplete by mainViewModel.isOnboardingComplete.collectAsState(initial = false)
    val currentIntent by intentState

    // Handle Strava OAuth callback
    LaunchedEffect(currentIntent) {
        currentIntent?.data?.let { uri ->
            if (uri.scheme == "http" && uri.host == "localhost" && uri.path == "/strava-oauth") {
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    stravaAuthViewModel.handleAuthCallback(code)
                    // Navigate back to home after successful auth
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (isOnboardingComplete) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onStartWorkout = {
                    navController.navigate("workout")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            com.workoutapp.presentation.settings.StravaAuthScreen(
                viewModel = stravaAuthViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("workout") {
            WorkoutScreen(
                onWorkoutComplete = {
                    navController.navigate("home") {
                        popUpTo("workout") { inclusive = true }
                    }
                },
                onNavigateToAddExercise = { workoutType, exerciseIds, onExerciseSelected ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("onExerciseSelected", onExerciseSelected)
                    val exerciseIdsParam = if (exerciseIds.isEmpty()) "none" else exerciseIds.joinToString(",")
                    navController.navigate("addExercise/${workoutType}/${exerciseIdsParam}")
                }
            )
        }
        
        composable("addExercise/{workoutType}/{currentExerciseIds}") { backStackEntry ->
            val workoutType = backStackEntry.arguments?.getString("workoutType") ?: ""
            val exerciseIdsString = backStackEntry.arguments?.getString("currentExerciseIds") ?: ""
            val currentExerciseIds = if (exerciseIdsString.isNotBlank() && exerciseIdsString != "none") {
                exerciseIdsString.split(",").filter { it.isNotBlank() }
            } else {
                emptyList()
            }
            
            
            val onExerciseSelected = navController.previousBackStackEntry?.savedStateHandle?.get<(com.workoutapp.domain.model.Exercise) -> Unit>("onExerciseSelected")
            
            AddExerciseScreen(
                workoutType = workoutType,
                currentExerciseIds = currentExerciseIds,
                onExerciseSelected = { exercise ->
                    onExerciseSelected?.invoke(exercise)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateExercise = {
                    navController.navigate("createExercise")
                }
            )
        }
        
        composable("createExercise") {
            CreateExerciseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExerciseCreated = { exercise ->
                    // Navigate back to AddExerciseScreen after creating exercise
                    navController.popBackStack()
                }
            )
        }
    }
}