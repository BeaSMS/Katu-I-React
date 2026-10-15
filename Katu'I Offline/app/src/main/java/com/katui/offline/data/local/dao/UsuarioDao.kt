package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getUsuarioPorEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun getUsuarioPorId(id: Long): Flow<Usuario?>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuario(id: Long): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirUsuario(usuario: Usuario): Long

    @Update
    suspend fun atualizarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE cuidadorId = :cuidadorId")
    fun getDependentes(cuidadorId: Long): Flow<List<Usuario>>

    @Query("SELECT p.* FROM usuarios p INNER JOIN cuidador_paciente cp ON p.id = cp.pacienteId WHERE cp.cuidadorId = :cuidadorId")
    fun getPacientesDoCuidador(cuidadorId: Long): Flow<List<Usuario>>

    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun deletarUsuario(id: Long)
}
