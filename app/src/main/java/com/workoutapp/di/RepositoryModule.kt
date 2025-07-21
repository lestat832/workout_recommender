package com.workoutapp.di

import com.workoutapp.data.repository.ExerciseRepositoryImpl
import com.workoutapp.data.repository.UserPreferencesRepositoryImpl
import com.workoutapp.data.repository.WorkoutRepositoryImpl
import com.workoutapp.domain.repository.ExerciseRepository
import com.workoutapp.domain.repository.UserPreferencesRepository
import com.workoutapp.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        exerciseRepositoryImpl: ExerciseRepositoryImpl
    ): ExerciseRepository
    
    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        workoutRepositoryImpl: WorkoutRepositoryImpl
    ): WorkoutRepository
    
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}