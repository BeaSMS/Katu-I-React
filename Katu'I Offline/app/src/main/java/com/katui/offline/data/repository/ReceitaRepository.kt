package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.ReceitaDao
import com.katui.offline.data.local.entities.Receita
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceitaRepository @Inject constructor(
    private val receitaDao: ReceitaDao
) {
    fun getReceitasByUsuario(usuarioId: Long): Flow<List<Receita>> =
        receitaDao.getReceitasByUsuario(usuarioId)

    suspend fun insert(receita: Receita) = receitaDao.insert(receita)

    suspend fun delete(receita: Receita) = receitaDao.delete(receita)
}
