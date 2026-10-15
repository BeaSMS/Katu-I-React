package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.VacinaDao
import com.katui.offline.data.local.entities.Vacina
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VacinaRepository @Inject constructor(
    private val vacinaDao: VacinaDao
) {
    fun getVacinas(usuarioId: Long): Flow<List<Vacina>> =
        vacinaDao.getVacinasPorUsuario(usuarioId)

    suspend fun inserirVacinas(vacinas: List<Vacina>) =
        vacinaDao.inserirVacinas(vacinas)

    suspend fun inserirVacina(vacina: Vacina) =
        vacinaDao.inserirVacina(vacina)

    suspend fun atualizarVacina(vacina: Vacina) =
        vacinaDao.atualizarVacina(vacina)

    suspend fun deletarVacina(vacina: Vacina) =
        vacinaDao.deletarVacina(vacina)

    suspend fun getContagemVacinas(usuarioId: Long): Int =
        vacinaDao.getContagemVacinas(usuarioId)

    suspend fun getMaiorOrdemDose(usuarioId: Long, grupo: String): Int? =
        vacinaDao.getMaiorOrdemDose(usuarioId, grupo)
}
