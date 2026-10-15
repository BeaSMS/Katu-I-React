package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Receita
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceitaDao {
    @Query("SELECT * FROM receitas WHERE usuarioId = :usuarioId ORDER BY dataRegistro DESC")
    fun getReceitasByUsuario(usuarioId: Long): Flow<List<Receita>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receita: Receita): Long

    @Delete
    suspend fun delete(receita: Receita)
}
