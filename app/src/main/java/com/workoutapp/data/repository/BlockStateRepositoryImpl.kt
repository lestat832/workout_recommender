package com.workoutapp.data.repository

import android.content.Context
import com.workoutapp.domain.repository.BlockStateRepository
import com.workoutapp.domain.usecase.BlockPeriodization
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BlockStateRepository {

    private val prefs by lazy {
        context.getSharedPreferences("block_state_prefs", Context.MODE_PRIVATE)
    }

    override suspend fun getState(gymId: Long): Pair<Date, Int> {
        val startKey = "block_start_date_$gymId"
        val numberKey = "block_number_$gymId"

        val storedStart = prefs.getLong(startKey, 0L)
        return if (storedStart == 0L) {
            val today = Date()
            prefs.edit()
                .putLong(startKey, today.time)
                .putInt(numberKey, 1)
                .apply()
            today to 1
        } else {
            Date(storedStart) to prefs.getInt(numberKey, 1)
        }
    }

    override suspend fun advanceBlock(gymId: Long) {
        val startKey = "block_start_date_$gymId"
        val numberKey = "block_number_$gymId"
        val newStart = BlockPeriodization.nextMondayAfter(Date())
        val currentNumber = prefs.getInt(numberKey, 1)
        prefs.edit()
            .putLong(startKey, newStart.time)
            .putInt(numberKey, currentNumber + 1)
            .apply()
    }
}
