package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.ConsultaLembrete
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultaLembreteDao {
    @Query("SELECT * FROM consulta_lembretes WHERE consultaId = :consultaId ORDER BY id ASC")
    fun getLembretesByConsulta(consultaId: Long): Flow<List<ConsultaLembrete>>

    @Query("SELECT * FROM consulta_lembretes WHERE consultaId = :consultaId ORDER BY id ASC")
    suspend fun getLembretesByConsultaSync(consultaId: Long): List<ConsultaLembrete>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lembrete: ConsultaLembrete): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lembretes: List<ConsultaLembrete>)

    @Delete
    suspend fun delete(lembrete: ConsultaLembrete)

    // Usado ao reagendar uma consulta: limpa os lembretes antigos antes de
    // inserir a lista atualizada vinda da tela de edição.
    @Query("DELETE FROM consulta_lembretes WHERE consultaId = :consultaId")
    suspend fun deleteAllForConsulta(consultaId: Long)
}
