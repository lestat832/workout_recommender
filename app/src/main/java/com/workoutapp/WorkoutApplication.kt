package com.workoutapp

import android.app.Application
import android.util.Log
import com.workoutapp.domain.usecase.ImportDebugDataUseCase
import com.workoutapp.domain.usecase.InitializeDatabaseUseCase
import com.workoutapp.domain.usecase.InitializeExercisesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkoutApplication : Application() {

    @Inject
    lateinit var initializeDatabaseUseCase: InitializeDatabaseUseCase

    @Inject
    lateinit var importDebugDataUseCase: ImportDebugDataUseCase

    @Inject
    lateinit var initializeExercisesUseCase: InitializeExercisesUseCase

    override fun onCreate() {
        super.onCreate()

        // Initialize database with exercises
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the database with exercises from ExerciseDB (first run only)
            val result = initializeExercisesUseCase()

            if (result.isSuccess) {
                val count = result.getOrNull()
                if (count != null) {
                    Log.d(TAG, "✅ Loaded $count exercises from ExerciseDB")
                } else {
                    Log.d(TAG, "✅ Exercises already initialized (${initializeExercisesUseCase.getCachedExerciseCount()} exercises)")
                }
            } else {
                Log.e(TAG, "❌ Failed to initialize exercises: ${result.exceptionOrNull()?.message}")
            }

            // Legacy initialization (if needed for other data)
            initializeDatabaseUseCase()

            // Note: Debug data import removed - now manual via debug menu "Import Marc" button
        }
    }

    companion object {
        private const val TAG = "WorkoutApp"
    }
}