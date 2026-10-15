package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Medicamento
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {
    @Query("SELECT * FROM medicamentos WHERE usuarioId = :usuarioId")
    fun getMedicamentosByUsuario(usuarioId: Long): Flow<List<Medicamento>>

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun getMedicamento(id: Long): Medicamento?

    @Query("SELECT * FROM medicamentos WHERE usuarioId = :usuarioId AND UPPER(nome) = UPPER(:nome) LIMIT 1")
    suspend fun getMedicamentoByName(usuarioId: Long, nome: String): Medicamento?

    @Query("UPDATE medicamentos SET estoqueAtual = estoqueAtual - :dose WHERE id = :id")
    suspend fun subtrairEstoque(id: Long, dose: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicamento: Medicamento): Long

    @Update
    suspend fun update(medicamento: Medicamento)

    @Delete
    suspend fun delete(medicamento: Medicamento)
}
