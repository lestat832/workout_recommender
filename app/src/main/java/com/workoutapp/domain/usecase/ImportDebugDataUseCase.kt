package com.workoutapp.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.workoutapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ImportDebugDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importWorkoutUseCase: ImportWorkoutUseCase
) {
    companion object {
        private const val PREFS_NAME = "debug_import_prefs"
        private const val KEY_DEBUG_DATA_IMPORTED = "debug_data_imported"
        private const val DEBUG_CSV_PATH = "debug/marc_workouts.csv"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    suspend operator fun invoke() {
        // Only import if enabled in build config
        if (!BuildConfig.ENABLE_DEBUG_DATA_IMPORT) {
            return
        }
        
        // Check if we've already imported the debug data
        if (hasDebugDataBeenImported()) {
            return
        }
        
        try {
            // Read the CSV from assets
            val csvContent = readCsvFromAssets()
            
            if (csvContent.isNotEmpty()) {
                // Import the workouts
                val result = importWorkoutUseCase.importFromCsvContent(csvContent)
                
                // If successful, mark as imported
                if (result.importedWorkouts > 0) {
                    markDebugDataAsImported()
                    println("Debug data import successful: ${result.importedWorkouts} workouts imported")
                }
            }
        } catch (e: Exception) {
            // Silently fail in debug - we don't want to crash the app if debug import fails
            println("Debug data import failed: ${e.message}")
        }
    }
    
    private fun readCsvFromAssets(): String {
        return try {
            val inputStream = context.assets.open(DEBUG_CSV_PATH)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.use { it.readText() }
        } catch (e: Exception) {
            // File might not exist in release builds or if not added yet
            ""
        }
    }
    
    private fun hasDebugDataBeenImported(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_DATA_IMPORTED, false)
    }
    
    private fun markDebugDataAsImported() {
        prefs.edit().putBoolean(KEY_DEBUG_DATA_IMPORTED, true).apply()
    }
    
    fun resetDebugDataImport() {
        prefs.edit().putBoolean(KEY_DEBUG_DATA_IMPORTED, false).apply()
    }
    
    fun isDebugDataImported(): Boolean {
        return hasDebugDataBeenImported()
    }
    
    suspend fun importManually() {
        try {
            // Read the CSV from assets
            val csvContent = readCsvFromAssets()
            
            if (csvContent.isNotEmpty()) {
                // Import the workouts without checking if already imported
                val result = importWorkoutUseCase.importFromCsvContent(csvContent)
                
                // If successful, mark as imported
                if (result.importedWorkouts > 0) {
                    markDebugDataAsImported()
                    println("Manual debug data import successful: ${result.importedWorkouts} workouts imported")
                }
            }
        } catch (e: Exception) {
            println("Manual debug data import failed: ${e.message}")
            throw e // Re-throw for error handling in UI
        }
    }
}