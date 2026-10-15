package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Anticoncepcional
import com.katui.offline.data.local.entities.RegistroAnticoncepcional
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AnticoncepcionalDao {
    @Query("SELECT * FROM anticoncepcionais WHERE usuarioId = :userId AND ativo = 1 LIMIT 1")
    fun getAnticoncepcionalAtivo(userId: Long): Flow<Anticoncepcional?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnticoncepcional(anti: Anticoncepcional)

    @Update
    suspend fun updateAnticoncepcional(anti: Anticoncepcional)

    @Query("SELECT * FROM registro_anticoncepcional WHERE anticoncepcionalId = :antiId AND data = :data")
    suspend fun getRegistroPorData(antiId: Long, data: LocalDate): RegistroAnticoncepcional?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroAnticoncepcional)

    @Query("SELECT * FROM registro_anticoncepcional WHERE anticoncepcionalId = :antiId ORDER BY data DESC")
    fun getRegistros(antiId: Long): Flow<List<RegistroAnticoncepcional>>
}
