package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.ExerciseDao
import com.workoutapp.data.database.dao.WorkoutDao
import com.workoutapp.data.database.entities.WorkoutEntity
import com.workoutapp.data.database.entities.WorkoutExerciseEntity
import com.workoutapp.domain.model.*
import com.workoutapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) : WorkoutRepository {
    
    override suspend fun createWorkout(workout: Workout): String {
        val workoutEntity = WorkoutEntity(
            id = workout.id,
            date = workout.date,
            type = workout.type,
            status = workout.status
        )
        workoutDao.insertWorkout(workoutEntity)
        return workout.id
    }
    
    override suspend fun updateWorkout(workout: Workout) {
        val workoutEntity = WorkoutEntity(
            id = workout.id,
            date = workout.date,
            type = workout.type,
            status = workout.status
        )
        workoutDao.updateWorkout(workoutEntity)
    }
    
    override suspend fun deleteWorkout(workoutId: String) {
        // Delete workout exercises first
        workoutDao.deleteWorkoutExercises(workoutId)
        // Then delete the workout
        workoutDao.deleteWorkout(workoutId)
    }
    
    override suspend fun getWorkoutById(id: String): Workout? {
        val workoutEntity = workoutDao.getWorkoutById(id) ?: return null
        val exerciseEntities = workoutDao.getWorkoutExercises(id)
        
        val exercises = exerciseEntities.map { exerciseEntity ->
            val exercise = exerciseDao.getExercisesByType(WorkoutType.PUSH)
                .firstOrNull { it.id == exerciseEntity.exerciseId }
                ?: exerciseDao.getExercisesByType(WorkoutType.PULL)
                    .firstOrNull { it.id == exerciseEntity.exerciseId }
            
            
            WorkoutExercise(
                id = exerciseEntity.id,
                workoutId = exerciseEntity.workoutId,
                exercise = exercise?.let {
                    Exercise(
                        id = it.id,
                        name = it.name,
                        muscleGroup = it.muscleGroup,
                        equipment = it.equipment,
                        category = it.category
                    )
                } ?: throw IllegalStateException("Exercise not found"),
                sets = exerciseEntity.sets
            )
        }
        
        return Workout(
            id = workoutEntity.id,
            date = workoutEntity.date,
            type = workoutEntity.type,
            status = workoutEntity.status,
            exercises = exercises
        )
    }
    
    override suspend fun getLastWorkout(): Workout? {
        val workoutEntity = workoutDao.getLastWorkout() ?: return null
        return getWorkoutById(workoutEntity.id)
    }
    
    override fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByStatus(status).map { workouts ->
            workouts.map { workoutEntity ->
                Workout(
                    id = workoutEntity.id,
                    date = workoutEntity.date,
                    type = workoutEntity.type,
                    status = workoutEntity.status
                )
            }
        }
    }
    
    override suspend fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise) {
        val entity = WorkoutExerciseEntity(
            id = exercise.id,
            workoutId = workoutId,
            exerciseId = exercise.exercise.id,
            sets = exercise.sets
        )
        workoutDao.insertWorkoutExercises(listOf(entity))
    }
    
    override suspend fun updateWorkoutExercise(exercise: WorkoutExercise) {
        val entity = WorkoutExerciseEntity(
            id = exercise.id,
            workoutId = exercise.workoutId,
            exerciseId = exercise.exercise.id,
            sets = exercise.sets
        )
        workoutDao.updateWorkoutExercise(entity)
    }
    
    override suspend fun getExerciseIdsFromLastWeek(): List<String> {
        val oneWeekAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        return workoutDao.getExerciseIdsFromDate(oneWeekAgo)
    }
    
    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { workouts ->
            workouts.map { workoutEntity ->
                Workout(
                    id = workoutEntity.id,
                    date = workoutEntity.date,
                    type = workoutEntity.type,
                    status = workoutEntity.status
                )
            }
        }
    }
}