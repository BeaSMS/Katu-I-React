package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.GestacaoDao
import com.katui.offline.data.local.entities.Gestacao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestacaoRepository @Inject constructor(
    private val gestacaoDao: GestacaoDao
) {
    fun getGestacaoAtiva(usuarioId: Long): Flow<Gestacao?> =
        gestacaoDao.getGestacaoAtiva(usuarioId)

    suspend fun iniciarGestacao(gestacao: Gestacao) {
        gestacaoDao.encerrarGestacoes(gestacao.usuarioId)
        gestacaoDao.inserirGestacao(gestacao)
    }

    suspend fun atualizarGestacao(gestacao: Gestacao) {
        gestacaoDao.atualizarGestacao(gestacao)
    }

    suspend fun encerrarGestacao(usuarioId: Long) {
        gestacaoDao.encerrarGestacoes(usuarioId)
    }
}
