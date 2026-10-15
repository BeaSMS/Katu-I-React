package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Vitals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface VitalsDao {
    @Query("SELECT * FROM vitals WHERE usuarioId = :usuarioId ORDER BY dataHora DESC")
    fun getVitalsPorUsuario(usuarioId: Long): Flow<List<Vitals>>

    @Query("SELECT * FROM vitals WHERE usuarioId = :usuarioId AND dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    fun getVitalsPorPeriodo(usuarioId: Long, inicio: LocalDateTime, fim: LocalDateTime): Flow<List<Vitals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitals(vitals: Vitals)

    @Update
    suspend fun updateVitals(vitals: Vitals)

    @Delete
    suspend fun deleteVitals(vitals: Vitals)
}
