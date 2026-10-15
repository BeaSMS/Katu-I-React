package com.katui.offline.data.repository

import android.content.Context
import com.katui.offline.data.local.dao.ConsultaDao
import com.katui.offline.data.local.dao.ConsultaLembreteDao
import com.katui.offline.data.local.entities.Consulta
import com.katui.offline.data.local.entities.ConsultaLembrete
import com.katui.offline.util.ConsultationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsultaRepository @Inject constructor(
    private val consultaDao: ConsultaDao,
    private val lembreteDao: ConsultaLembreteDao,
    @ApplicationContext private val context: Context
) {
    fun getConsultas(usuarioId: Long): Flow<List<Consulta>> =
        consultaDao.getConsultasByUsuario(usuarioId)

    suspend fun getConsultasPendentes(now: java.time.LocalDateTime): List<Consulta> =
        consultaDao.getConsultasPendentes(now)

    fun getLembretesForConsulta(consultaId: Long): Flow<List<ConsultaLembrete>> =
        lembreteDao.getLembretesByConsulta(consultaId)

    suspend fun insertConsulta(
        consulta: Consulta,
        lembretes: List<ConsultaLembrete>,
        userName: String? = null,
        userSexo: String? = null,
        userId: Long? = null
    ): Long {
        // 1. Salva ou atualiza a consulta
        val id = if (consulta.id == 0L) {
            consultaDao.insert(consulta)
        } else {
            // Se já existe, cancelamos os agendamentos anteriores baseados nos IDs antigos
            val antigos = lembreteDao.getLembretesByConsultaSync(consulta.id)
            antigos.forEach { ConsultationScheduler.cancelSpecific(context, consulta.id, it.id) }
            // Também cancelamos os 3 tipos fixos antigos por segurança (migração)
            ConsultationScheduler.cancel(context, consulta.id)
            
            consultaDao.insert(consulta)
            consulta.id
        }
        
        // 2. Atualiza os lembretes no banco
        lembreteDao.deleteAllForConsulta(id)
        val lembretesComId = lembretes.map { it.copy(id = 0, consultaId = id) }
        lembreteDao.insertAll(lembretesComId)
        
        // 3. Busca os lembretes recém-inseridos para ter os IDs reais gerados pelo banco
        val lembretesPersistidos = lembreteDao.getLembretesByConsultaSync(id)
        
        // 4. Agenda as notificações
        ConsultationScheduler.schedule(
            context,
            id,
            consulta.medico,
            consulta.especialidade,
            consulta.dataHora,
            lembretesPersistidos,
            userName = userName,
            userSexo = userSexo,
            userId = userId
        )
        
        return id
    }

    suspend fun deleteConsulta(consulta: Consulta) {
        // 1. Busca os lembretes para cancelar os agendamentos
        val lembretes = lembreteDao.getLembretesByConsultaSync(consulta.id)
        lembretes.forEach { ConsultationScheduler.cancelSpecific(context, consulta.id, it.id) }
        
        // 2. Cancela os 3 tipos fixos antigos (retrocompatibilidade)
        ConsultationScheduler.cancel(context, consulta.id)
        
        // 3. Deleta do banco (lembretes morrem via CASCADE)
        consultaDao.delete(consulta)
    }
}
