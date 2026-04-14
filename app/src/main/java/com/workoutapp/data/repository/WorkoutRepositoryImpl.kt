package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.ExerciseDao
import com.workoutapp.data.database.dao.WorkoutDao
import com.workoutapp.data.database.entities.ExerciseEntity
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
            status = workout.status,
            gymId = workout.gymId,
            format = workout.format,
            durationMinutes = workout.durationMinutes,
            completedRounds = workout.completedRounds
        )
        workoutDao.insertWorkout(workoutEntity)
        return workout.id
    }

    override suspend fun updateWorkout(workout: Workout) {
        val workoutEntity = WorkoutEntity(
            id = workout.id,
            date = workout.date,
            type = workout.type,
            status = workout.status,
            gymId = workout.gymId,
            format = workout.format,
            durationMinutes = workout.durationMinutes,
            completedRounds = workout.completedRounds
        )
        workoutDao.updateWorkout(workoutEntity)
    }

    override suspend fun deleteWorkout(workoutId: String) {
        workoutDao.deleteWorkoutExercises(workoutId)
        workoutDao.deleteWorkout(workoutId)
    }

    override suspend fun getWorkoutById(id: String): Workout? {
        val workoutEntity = workoutDao.getWorkoutById(id) ?: return null
        val exerciseEntities = workoutDao.getWorkoutExercises(id)

        val exercises = exerciseEntities.map { exerciseEntity ->
            val exercise = exerciseDao.getExerciseById(exerciseEntity.exerciseId)
                ?: throw IllegalStateException("Exercise not found: ${exerciseEntity.exerciseId}")

            WorkoutExercise(
                id = exerciseEntity.id,
                workoutId = exerciseEntity.workoutId,
                exercise = exercise.toDomain(),
                sets = exerciseEntity.sets,
                prescription = exerciseEntity.prescription,
                rir = exerciseEntity.rir
            )
        }

        return workoutEntity.toDomain(exercises)
    }

    override suspend fun getLastWorkout(): Workout? {
        val workoutEntity = workoutDao.getLastWorkout() ?: return null
        return getWorkoutById(workoutEntity.id)
    }

    override suspend fun getLastCompletedWorkoutByGym(gymId: Long): Workout? {
        val workoutEntity = workoutDao.getLastCompletedWorkoutByGym(gymId) ?: return null
        return getWorkoutById(workoutEntity.id)
    }

    override suspend fun getConditioningWorkoutsInMonth(gymId: Long): List<Workout> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.time
        cal.add(Calendar.MONTH, 1)
        val monthEnd = cal.time
        val entities = workoutDao.getConditioningWorkoutsInRange(gymId, monthStart, monthEnd)
        return entities.mapNotNull { getWorkoutById(it.id) }
    }

    override fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByStatus(status).map { workouts ->
            workouts.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun addExerciseToWorkout(workoutId: String, exercise: WorkoutExercise) {
        val entity = WorkoutExerciseEntity(
            id = exercise.id,
            workoutId = workoutId,
            exerciseId = exercise.exercise.id,
            sets = exercise.sets,
            prescription = exercise.prescription,
            rir = exercise.rir
        )
        workoutDao.insertWorkoutExercises(listOf(entity))
    }

    override suspend fun replaceExercisesForWorkout(
        workoutId: String,
        exercises: List<WorkoutExercise>
    ) {
        val entities = exercises.map { exercise ->
            WorkoutExerciseEntity(
                id = exercise.id,
                workoutId = workoutId,
                exerciseId = exercise.exercise.id,
                sets = exercise.sets,
                prescription = exercise.prescription
            )
        }
        workoutDao.replaceWorkoutExercises(workoutId, entities)
    }

    override suspend fun updateWorkoutExercise(exercise: WorkoutExercise) {
        val entity = WorkoutExerciseEntity(
            id = exercise.id,
            workoutId = exercise.workoutId,
            exerciseId = exercise.exercise.id,
            sets = exercise.sets,
            prescription = exercise.prescription,
            rir = exercise.rir
        )
        workoutDao.updateWorkoutExercise(entity)
    }

    override suspend fun getExerciseIdsFromLastWeek(): List<String> {
        val oneWeekAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        return workoutDao.getExerciseIdsFromDate(oneWeekAgo)
    }

    override suspend fun getAllCompletedWorkoutsWithExercises(): List<Workout> {
        return workoutDao.getAllCompletedWorkouts().mapNotNull { entity ->
            getWorkoutById(entity.id)
        }
    }

    override suspend fun reassignWorkouts(oldGymId: Long, newGymId: Long) {
        workoutDao.reassignWorkouts(oldGymId, newGymId)
    }

    override suspend fun getInProgressStrengthWorkout(gymId: Long): Workout? {
        val entity = workoutDao.getInProgressStrengthWorkout(gymId) ?: return null
        return getWorkoutById(entity.id)
    }

    override suspend fun getInProgressConditioningWorkout(gymId: Long): Workout? {
        val entity = workoutDao.getInProgressConditioningWorkout(gymId) ?: return null
        return getWorkoutById(entity.id)
    }

    override fun getCompletedWorkoutsByGym(gymId: Long): Flow<List<Workout>> {
        return workoutDao.getCompletedWorkoutsByGym(gymId).map { workouts ->
            workouts.map { it.toDomain(emptyList()) }
        }
    }

    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { workouts ->
            workouts.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun getExerciseLastPerformedDates(): Map<String, Date> {
        return workoutDao.getExerciseLastPerformedDates()
            .associate { it.exerciseId to it.lastDate }
    }

    override suspend fun getCompletedWorkoutSummariesSince(since: Date): List<CompletedWorkoutSummary> {
        val rows = workoutDao.getCompletedWorkoutSummariesSince(since.time)
        return rows.mapNotNull { row ->
            val format = runCatching { WorkoutFormat.valueOf(row.format) }.getOrNull() ?: return@mapNotNull null
            val muscleGroups = row.muscleGroupsRaw
                .split('|', ',')
                .mapNotNull { token ->
                    val trimmed = token.trim()
                    if (trimmed.isEmpty()) null
                    else runCatching { MuscleGroup.valueOf(trimmed) }.getOrNull()
                }
                .toSet()
            CompletedWorkoutSummary(
                id = row.id,
                date = row.date,
                format = format,
                durationMinutes = row.durationMinutes,
                muscleGroups = muscleGroups
            )
        }
    }

    private fun WorkoutEntity.toDomain(exercises: List<WorkoutExercise>): Workout = Workout(
        id = id,
        date = date,
        type = type,
        status = status,
        gymId = gymId,
        format = format,
        durationMinutes = durationMinutes,
        completedRounds = completedRounds,
        exercises = exercises
    )

    private fun ExerciseEntity.toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        muscleGroups = muscleGroups,
        equipment = equipment,
        category = category,
        exerciseCategory = exerciseCategory,
        imageUrl = imageUrl,
        instructions = instructions,
        isUserCreated = isUserCreated
    )
}
