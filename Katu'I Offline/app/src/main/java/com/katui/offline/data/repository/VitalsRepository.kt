package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.VitalsDao
import com.katui.offline.data.local.entities.Vitals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalsRepository @Inject constructor(private val vitalsDao: VitalsDao) {
    fun getVitals(usuarioId: Long): Flow<List<Vitals>> = vitalsDao.getVitalsPorUsuario(usuarioId)

    fun getVitalsPorPeriodo(usuarioId: Long, inicio: LocalDateTime, fim: LocalDateTime): Flow<List<Vitals>> = 
        vitalsDao.getVitalsPorPeriodo(usuarioId, inicio, fim)

    suspend fun insertVitals(vitals: Vitals) = vitalsDao.insertVitals(vitals)

    suspend fun updateVitals(vitals: Vitals) = vitalsDao.updateVitals(vitals)

    suspend fun deleteVitals(vitals: Vitals) = vitalsDao.deleteVitals(vitals)
}
