package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.ExameDao
import com.katui.offline.data.local.dao.ExameWithResultados
import com.katui.offline.data.local.entities.Exame
import com.katui.offline.data.local.entities.ExameResultado
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExameRepository @Inject constructor(
    private val exameDao: ExameDao
) {
    fun getExames(usuarioId: Long): Flow<List<Exame>> =
        exameDao.getExamesByUsuario(usuarioId)

    suspend fun insertExame(exame: Exame): Long =
        exameDao.insert(exame)

    suspend fun deleteExame(exame: Exame) =
        exameDao.delete(exame)

    fun getExameComResultados(exameId: Long): Flow<ExameWithResultados> =
        exameDao.getExameComResultados(exameId)

    suspend fun insertResultado(resultado: ExameResultado) =
        exameDao.insertResultado(resultado)

    suspend fun deleteResultadosByExame(exameId: Long) =
        exameDao.deleteResultadosByExame(exameId)

    fun getHistoricoPorMetrica(metrica: String, usuarioId: Long): Flow<List<ExameResultado>> =
        exameDao.getHistoricoPorMetrica(metrica, usuarioId)

    fun getMetricasDisponiveis(usuarioId: Long): Flow<List<String>> =
        exameDao.getMetricasDisponiveis(usuarioId)

    fun getMetricasComDadosSuficientes(usuarioId: Long) =
        exameDao.getMetricasComDadosSuficientes(usuarioId)
}
