package com.workoutapp

import android.app.Application
import com.workoutapp.domain.usecase.ImportDebugDataUseCase
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
    
    @Inject
    lateinit var importDebugDataUseCase: ImportDebugDataUseCase
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database with exercises
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the database with exercises
            initializeDatabaseUseCase()
            
            // Note: Debug data import removed - now manual via debug menu "Import Marc" button
        }
    }
}