package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Vacina
import kotlinx.coroutines.flow.Flow

@Dao
interface VacinaDao {
    @Query("SELECT * FROM vacinas WHERE usuarioId = :usuarioId ORDER BY categoria, grupoVacina, ordemDose, nome")
    fun getVacinasPorUsuario(usuarioId: Long): Flow<List<Vacina>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVacina(vacina: Vacina)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVacinas(vacinas: List<Vacina>)

    @Update
    suspend fun atualizarVacina(vacina: Vacina)

    @Delete
    suspend fun deletarVacina(vacina: Vacina)

    @Query("SELECT COUNT(*) FROM vacinas WHERE usuarioId = :usuarioId")
    suspend fun getContagemVacinas(usuarioId: Long): Int

    /** Retorna o maior ordemDose dentro de um grupo para saber qual número atribuir à próxima dose. */
    @Query("SELECT MAX(ordemDose) FROM vacinas WHERE usuarioId = :usuarioId AND grupoVacina = :grupo")
    suspend fun getMaiorOrdemDose(usuarioId: Long, grupo: String): Int?
}
