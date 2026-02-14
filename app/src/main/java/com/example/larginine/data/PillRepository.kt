package com.example.larginine.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

class PillRepository(private val pillDao: PillDao) {
    val allPills: Flow<List<Pill>> = pillDao.getAllPills()
    val allPillsLiveData: LiveData<List<Pill>> = pillDao.getAllPillsLiveData()

    suspend fun getPillById(id: Long): Pill? {
        return pillDao.getPillById(id)
    }

    suspend fun insertPill(pill: Pill): Long {
        return pillDao.insertPill(pill)
    }

    suspend fun updatePill(pill: Pill) {
        pillDao.updatePill(pill)
    }

    suspend fun deletePill(pill: Pill) {
        pillDao.deletePill(pill)
    }

    suspend fun deletePillById(id: Long) {
        pillDao.deletePillById(id)
    }
}
