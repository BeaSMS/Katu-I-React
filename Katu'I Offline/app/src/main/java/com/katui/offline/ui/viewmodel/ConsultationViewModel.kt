package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Consulta
import com.katui.offline.data.local.entities.ConsultaLembrete
import com.katui.offline.data.local.entities.MetodoNotificacao
import com.katui.offline.data.local.entities.TipoLembreteConsulta
import com.katui.offline.data.repository.ConsultaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ConsultationViewModel @Inject constructor(
    private val repository: ConsultaRepository,
    private val usuarioDao: com.katui.offline.data.local.dao.UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _consultas = MutableStateFlow<List<Consulta>>(emptyList())
    val consultas: StateFlow<List<Consulta>> = _consultas.asStateFlow()

    private val _usuario = MutableStateFlow<com.katui.offline.data.local.entities.Usuario?>(null)
    val usuario: StateFlow<com.katui.offline.data.local.entities.Usuario?> = _usuario.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val filteredConsultas = combine(_consultas, _selectedDate) { list, date ->
        list.filter { it.dataHora.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingConsultas = _consultas.map { list ->
        val today = LocalDate.now()
        val nextWeek = today.plusDays(7)
        list.filter { 
            val date = it.dataHora.toLocalDate()
            !date.isBefore(today) && !date.isAfter(nextWeek)
        }.sortedBy { it.dataHora }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getConsultas(userId) }
            .onEach { _consultas.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getUsuarioPorId(userId) }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Retorna a lista de lembretes padrão para uma nova consulta.
     * O de 3 horas antes agora vem como ALARME por padrão, os outros como NOTIFICACAO.
     */
    fun getDefaultReminders(consultaDate: LocalDate): List<ConsultaLembrete> {
        return listOf(
            ConsultaLembrete(
                consultaId = 0, 
                tipo = TipoLembreteConsulta.RELATIVO, 
                metodo = MetodoNotificacao.NOTIFICACAO,
                minutosAntes = 0, 
                rotulo = "Horário da consulta"
            ),
            ConsultaLembrete(
                consultaId = 0, 
                tipo = TipoLembreteConsulta.RELATIVO, 
                metodo = MetodoNotificacao.ALARME, // Padrão antigo era alarme pra 3h
                minutosAntes = 180, 
                rotulo = "3 horas antes"
            ),
            ConsultaLembrete(
                consultaId = 0,
                tipo = TipoLembreteConsulta.ABSOLUTO, 
                metodo = MetodoNotificacao.NOTIFICACAO,
                horarioAbsoluto = consultaDate.minusDays(1).atTime(LocalTime.of(12, 0)),
                rotulo = "Véspera ao meio-dia"
            )
        )
    }

    /**
     * Busca lembretes de uma consulta específica.
     */
    fun getLembretesForConsulta(consultaId: Long): Flow<List<ConsultaLembrete>> {
        return repository.getLembretesForConsulta(consultaId)
    }

    fun addConsulta(consulta: Consulta, lembretes: List<ConsultaLembrete>) {
        viewModelScope.launch {
            val user = _usuario.value
            val isPrincipal = user?.id == 1L
            repository.insertConsulta(
                consulta = consulta.copy(usuarioId = user?.id ?: 0L),
                lembretes = lembretes,
                userName = if (!isPrincipal) user?.nome else null,
                userSexo = if (!isPrincipal) user?.sexo?.name else null,
                userId = user?.id
            )
        }
    }

    fun updateConsulta(consulta: Consulta, lembretes: List<ConsultaLembrete>) {
        viewModelScope.launch {
            val user = _usuario.value
            val isPrincipal = user?.id == 1L
            repository.insertConsulta(
                consulta = consulta,
                lembretes = lembretes,
                userName = if (!isPrincipal) user?.nome else null,
                userSexo = if (!isPrincipal) user?.sexo?.name else null,
                userId = user?.id
            )
        }
    }

    fun deleteConsulta(consulta: Consulta) {
        viewModelScope.launch {
            repository.deleteConsulta(consulta)
        }
    }
}
