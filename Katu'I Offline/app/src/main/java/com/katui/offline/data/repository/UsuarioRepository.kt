package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.Usuario
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val usuarioDao: UsuarioDao
) {
    suspend fun getUsuarioPorEmail(email: String) = usuarioDao.getUsuarioPorEmail(email)
    
    fun getUsuarioPorId(id: Long) = usuarioDao.getUsuarioPorId(id)

    suspend fun getUsuario(id: Long) = usuarioDao.getUsuario(id)

    suspend fun salvarUsuario(usuario: Usuario) = usuarioDao.inserirUsuario(usuario)

    fun getPacientes(cuidadorId: Long) = usuarioDao.getPacientesDoCuidador(cuidadorId)

    /**
     * Verifica se o usuario com o [id] informado realmente existe no banco.
     *
     * Usado para validar a consistencia entre o DataStore (onboarding_complete)
     * e o Room: se um backup/restauracao do sistema (Cloud Backup do Android,
     * transferencia de dispositivo, restauracao de OEM, etc.) trouxer de volta
     * a flag onboarding_complete=true sem o banco correspondente (ex: banco
     * vazio/recriado), essa checagem permite detectar a inconsistencia e
     * forcar o onboarding de novo, em vez de o app ficar "travado" esperando
     * por um usuario que nao existe.
     */
    suspend fun usuarioPrincipalExiste(id: Long = 1L): Boolean = usuarioDao.getUsuario(id) != null
}
