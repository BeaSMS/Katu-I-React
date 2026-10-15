package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Sintoma
import kotlinx.coroutines.flow.Flow

@Dao
interface SintomaDao {
    @Query("SELECT * FROM sintomas WHERE usuarioId = :usuarioId ORDER BY dataHoraRegistro DESC")
    fun getSintomasByUsuario(usuarioId: Long): Flow<List<Sintoma>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sintoma: Sintoma): Long

    @Update
    suspend fun update(sintoma: Sintoma)

    @Delete
    suspend fun delete(sintoma: Sintoma)
}
