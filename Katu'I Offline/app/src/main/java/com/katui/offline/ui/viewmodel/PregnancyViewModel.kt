package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Gestacao
import com.katui.offline.data.repository.GestacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PregnancyViewModel @Inject constructor(
    private val repository: GestacaoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _gestacao = MutableStateFlow<Gestacao?>(null)
    val gestacao: StateFlow<Gestacao?> = _gestacao.asStateFlow()

    val pregnancyHidden = sessionManager.pregnancyHidden.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getGestacaoAtiva(userId) }
            .onEach { _gestacao.value = it }
            .launchIn(viewModelScope)
    }

    fun iniciarGestacao(dum: LocalDate, nomeBebe: String? = null) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            val edd = dum.plusDays(280)
            repository.iniciarGestacao(Gestacao(
                usuarioId = userId,
                dataUltimaMenstruacao = dum,
                dataPartoEstimada = edd,
                nomeBebe = nomeBebe
            ))
        }
    }

    fun atualizarGestacao(gestacao: Gestacao) {
        viewModelScope.launch {
            repository.atualizarGestacao(gestacao)
        }
    }

    fun encerrarGestacao() {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.encerrarGestacao(userId)
        }
    }

    fun setPregnancyHidden(hidden: Boolean) {
        viewModelScope.launch {
            sessionManager.setPregnancyHidden(hidden)
        }
    }
}
