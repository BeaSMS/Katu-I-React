package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.CicloMenstrual
import kotlinx.coroutines.flow.Flow

@Dao
interface CicloMenstrualDao {
    @Query("SELECT * FROM ciclos_menstruais WHERE usuarioId = :userId ORDER BY dataInicio DESC")
    fun getCiclosPorUsuario(userId: Long): Flow<List<CicloMenstrual>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCiclo(ciclo: CicloMenstrual)

    @Update
    suspend fun updateCiclo(ciclo: CicloMenstrual)

    @Delete
    suspend fun deleteCiclo(ciclo: CicloMenstrual)
}
