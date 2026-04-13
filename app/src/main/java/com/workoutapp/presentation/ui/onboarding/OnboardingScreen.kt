package com.workoutapp.presentation.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.model.GymWorkoutStyle
import com.workoutapp.presentation.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val isComplete by viewModel.isComplete.collectAsState()

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onComplete()
        }
    }

    GymPickerStep(viewModel)
}

@Composable
fun GymPickerStep(viewModel: OnboardingViewModel) {
    val gyms by viewModel.gyms.collectAsState()
    val selectedGymId by viewModel.selectedGymId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Choose Your Territory",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select your primary hunting ground",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Gym cards
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            gyms.forEach { gym ->
                GymPickerCard(
                    gym = gym,
                    isSelected = gym.id == selectedGymId,
                    onClick = { viewModel.selectGym(gym.id) }
                )
            }
        }

        Button(
            onClick = { viewModel.selectGymAndComplete() },
            enabled = selectedGymId != null && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Enter the Territory")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymPickerCard(
    gym: Gym,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val styleLabel = when (gym.workoutStyle) {
        GymWorkoutStyle.STRENGTH -> "Strength Training"
        GymWorkoutStyle.CONDITIONING -> "Conditioning"
    }
    val styleDescription = when (gym.workoutStyle) {
        GymWorkoutStyle.STRENGTH -> "Push/Pull workouts with barbell, cable and machines"
        GymWorkoutStyle.CONDITIONING -> "EMOM & AMRAP with dumbbells, TRX and bodyweight"
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = gym.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = styleLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = styleDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
