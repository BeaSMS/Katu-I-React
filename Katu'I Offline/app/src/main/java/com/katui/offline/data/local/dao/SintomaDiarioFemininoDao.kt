package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.SintomaDiarioFeminino
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SintomaDiarioFemininoDao {
    @Query("SELECT * FROM sintomas_diarios_femininos WHERE usuarioId = :userId ORDER BY data DESC")
    fun getSintomasPorUsuario(userId: Long): Flow<List<SintomaDiarioFeminino>>

    @Query("SELECT * FROM sintomas_diarios_femininos WHERE usuarioId = :userId AND data = :data LIMIT 1")
    suspend fun getSintomaPorData(userId: Long, data: LocalDate): SintomaDiarioFeminino?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSintoma(sintoma: SintomaDiarioFeminino)

    @Update
    suspend fun updateSintoma(sintoma: SintomaDiarioFeminino)

    @Delete
    suspend fun deleteSintoma(sintoma: SintomaDiarioFeminino)
}
