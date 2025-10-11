package com.workoutapp.di

import android.content.Context
import com.workoutapp.data.database.WorkoutDatabase
import com.workoutapp.data.database.dao.ExerciseDao
import com.workoutapp.data.database.dao.StravaAuthDao
import com.workoutapp.data.database.dao.StravaSyncDao
import com.workoutapp.data.database.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWorkoutDatabase(
        @ApplicationContext context: Context
    ): WorkoutDatabase {
        return WorkoutDatabase.getDatabase(context)
    }

    @Provides
    fun provideExerciseDao(database: WorkoutDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideWorkoutDao(database: WorkoutDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideStravaAuthDao(database: WorkoutDatabase): StravaAuthDao {
        return database.stravaAuthDao()
    }

    @Provides
    fun provideStravaSyncDao(database: WorkoutDatabase): StravaSyncDao {
        return database.stravaSyncDao()
    }
}