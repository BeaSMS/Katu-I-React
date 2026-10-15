package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Gestacao
import kotlinx.coroutines.flow.Flow

@Dao
interface GestacaoDao {
    @Query("SELECT * FROM gestacoes WHERE usuarioId = :usuarioId AND ativa = 1 LIMIT 1")
    fun getGestacaoAtiva(usuarioId: Long): Flow<Gestacao?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirGestacao(gestacao: Gestacao): Long

    @Update
    suspend fun atualizarGestacao(gestacao: Gestacao)

    @Query("UPDATE gestacoes SET ativa = 0 WHERE usuarioId = :usuarioId")
    suspend fun encerrarGestacoes(usuarioId: Long)
}
