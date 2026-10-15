package com.katui.offline.data.repository

import android.content.Context
import com.katui.offline.data.local.dao.AnticoncepcionalDao
import com.katui.offline.data.local.dao.CicloMenstrualDao
import com.katui.offline.data.local.dao.SintomaDiarioFemininoDao
import com.katui.offline.data.local.entities.Anticoncepcional
import com.katui.offline.data.local.entities.CicloMenstrual
import com.katui.offline.data.local.entities.RegistroAnticoncepcional
import com.katui.offline.data.local.entities.SintomaDiarioFeminino
import com.katui.offline.util.FemaleHealthReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FemaleHealthRepository @Inject constructor(
    private val cicloMenstrualDao: CicloMenstrualDao,
    private val sintomaDiarioFemininoDao: SintomaDiarioFemininoDao,
    private val anticoncepcionalDao: AnticoncepcionalDao,
    @ApplicationContext private val context: Context
) {
    fun getCiclos(userId: Long): Flow<List<CicloMenstrual>> = 
        cicloMenstrualDao.getCiclosPorUsuario(userId)

    suspend fun triggerScheduling(userId: Long) {
        val ciclos = cicloMenstrualDao.getCiclosPorUsuario(userId).first()
        val anti = anticoncepcionalDao.getAnticoncepcionalAtivo(userId).first()
        FemaleHealthReminderScheduler.scheduleReminders(context, ciclos, anti)
    }

    suspend fun insertCiclo(ciclo: CicloMenstrual) {
        cicloMenstrualDao.insertCiclo(ciclo)
        triggerScheduling(ciclo.usuarioId)
    }

    suspend fun updateCiclo(ciclo: CicloMenstrual) {
        cicloMenstrualDao.updateCiclo(ciclo)
        triggerScheduling(ciclo.usuarioId)
    }

    suspend fun deleteCiclo(ciclo: CicloMenstrual) {
        cicloMenstrualDao.deleteCiclo(ciclo)
        triggerScheduling(ciclo.usuarioId)
    }

    // Sintomas Diários
    fun getSintomasDiarios(userId: Long): Flow<List<SintomaDiarioFeminino>> =
        sintomaDiarioFemininoDao.getSintomasPorUsuario(userId)

    suspend fun getSintomaDiarioPorData(userId: Long, data: LocalDate): SintomaDiarioFeminino? =
        sintomaDiarioFemininoDao.getSintomaPorData(userId, data)

    suspend fun insertOuUpdateSintomaDiario(sintoma: SintomaDiarioFeminino) {
        val existente = sintomaDiarioFemininoDao.getSintomaPorData(sintoma.usuarioId, sintoma.data)
        if (existente != null) {
            sintomaDiarioFemininoDao.updateSintoma(sintoma.copy(id = existente.id))
        } else {
            sintomaDiarioFemininoDao.insertSintoma(sintoma)
        }
    }

    // Anticoncepcional
    fun getAnticoncepcionalAtivo(userId: Long) = anticoncepcionalDao.getAnticoncepcionalAtivo(userId)
    
    suspend fun insertAnticoncepcional(anti: Anticoncepcional) {
        anticoncepcionalDao.insertAnticoncepcional(anti)
        triggerScheduling(anti.usuarioId)
    }

    suspend fun updateAnticoncepcional(anti: Anticoncepcional) {
        anticoncepcionalDao.updateAnticoncepcional(anti)
        triggerScheduling(anti.usuarioId)
    }
    
    suspend fun registrarTomada(antiId: Long, data: LocalDate, tomado: Boolean) {
        val registro = RegistroAnticoncepcional(anticoncepcionalId = antiId, data = data, tomado = tomado)
        anticoncepcionalDao.insertRegistro(registro)
    }

    fun getRegistrosAnti(antiId: Long) = anticoncepcionalDao.getRegistros(antiId)
}
