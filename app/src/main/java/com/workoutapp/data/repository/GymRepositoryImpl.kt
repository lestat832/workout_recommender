package com.workoutapp.data.repository

import com.workoutapp.data.database.dao.GymDao
import com.workoutapp.data.database.entities.GymEntity
import com.workoutapp.domain.model.Gym
import com.workoutapp.domain.repository.GymRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of GymRepository using Room database.
 */
class GymRepositoryImpl @Inject constructor(
    private val gymDao: GymDao
) : GymRepository {

    override fun getAllGymsFlow(): Flow<List<Gym>> {
        return gymDao.getAllGymsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllGyms(): List<Gym> {
        return gymDao.getAllGyms().map { it.toDomain() }
    }

    override suspend fun getGymById(gymId: Long): Gym? {
        return gymDao.getGymById(gymId)?.toDomain()
    }

    override suspend fun getDefaultGym(): Gym? {
        return gymDao.getDefaultGym()?.toDomain()
    }

    override fun getDefaultGymFlow(): Flow<Gym?> {
        return gymDao.getDefaultGymFlow().map { it?.toDomain() }
    }

    override suspend fun insertGym(gym: Gym): Long {
        return gymDao.insert(GymEntity.fromDomain(gym))
    }

    override suspend fun updateGym(gym: Gym) {
        gymDao.update(GymEntity.fromDomain(gym))
    }

    override suspend fun deleteGym(gym: Gym) {
        gymDao.delete(GymEntity.fromDomain(gym))
    }

    override suspend fun setDefaultGym(gymId: Long) {
        gymDao.setDefaultGym(gymId)
    }

    override suspend fun hasGyms(): Boolean {
        return gymDao.getGymCount() > 0
    }
}
