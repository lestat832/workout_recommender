package com.workoutapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.workoutapp.domain.usecase.ImportDebugDataUseCase
import com.workoutapp.domain.usecase.InitializeDatabaseUseCase
import com.workoutapp.domain.usecase.InitializeExercisesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkoutApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var initializeDatabaseUseCase: InitializeDatabaseUseCase

    @Inject
    lateinit var importDebugDataUseCase: ImportDebugDataUseCase

    @Inject
    lateinit var initializeExercisesUseCase: InitializeExercisesUseCase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize database with exercises
        CoroutineScope(Dispatchers.IO).launch {
            val result = initializeExercisesUseCase()
            if (result.isSuccess) {
                Log.d(TAG, "Exercises initialized")
            } else {
                Log.e(TAG, "Exercise init failed: ${result.exceptionOrNull()?.message}")
            }
            initializeDatabaseUseCase()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "WorkoutApp"
    }
}