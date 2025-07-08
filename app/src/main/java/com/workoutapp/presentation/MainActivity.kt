package com.workoutapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workoutapp.presentation.ui.home.HomeScreen
import com.workoutapp.presentation.ui.onboarding.OnboardingScreen
import com.workoutapp.presentation.ui.theme.WorkoutAppTheme
import com.workoutapp.presentation.ui.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WorkoutAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkoutNavigation()
                }
            }
        }
    }
}

@Composable
fun WorkoutNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "onboarding" // TODO: Check if first launch
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
                }
            )
        }
        
        composable("workout") {
            WorkoutScreen(
                onWorkoutComplete = {
                    navController.navigate("home") {
                        popUpTo("workout") { inclusive = true }
                    }
                }
            )
        }
    }
}