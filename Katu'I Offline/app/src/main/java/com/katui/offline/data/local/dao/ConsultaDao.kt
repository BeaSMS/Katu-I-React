package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Consulta
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultaDao {
    @Query("SELECT * FROM consultas WHERE usuarioId = :usuarioId ORDER BY dataHora ASC")
    fun getConsultasByUsuario(usuarioId: Long): Flow<List<Consulta>>

    @Query("SELECT * FROM consultas WHERE id = :id")
    suspend fun getConsultaById(id: Long): Consulta?

    @Query("SELECT * FROM consultas WHERE dataHora > :now")
    suspend fun getConsultasPendentes(now: java.time.LocalDateTime): List<Consulta>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consulta: Consulta): Long

    @Delete
    suspend fun delete(consulta: Consulta)
}
