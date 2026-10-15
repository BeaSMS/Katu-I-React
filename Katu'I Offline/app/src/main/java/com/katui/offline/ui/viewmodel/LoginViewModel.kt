package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.Usuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    fun login(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            var usuario = usuarioDao.getUsuarioPorEmail(email)
            if (usuario == null) {
                val id = usuarioDao.inserirUsuario(Usuario(nome = "Usuário Teste", senha = "123"))
                usuario = usuarioDao.getUsuario(id)
            }
            
            if (usuario != null) {
                sessionManager.saveUserId(usuario.id)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}
