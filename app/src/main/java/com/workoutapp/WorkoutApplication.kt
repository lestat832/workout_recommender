package com.workoutapp

import android.app.Application
import com.workoutapp.domain.usecase.InitializeDatabaseUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkoutApplication : Application() {
    
    @Inject
    lateinit var initializeDatabaseUseCase: InitializeDatabaseUseCase
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database with exercises
        CoroutineScope(Dispatchers.IO).launch {
            initializeDatabaseUseCase()
        }
    }
}