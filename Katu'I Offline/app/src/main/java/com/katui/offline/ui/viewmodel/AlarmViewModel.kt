package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Alarme
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.repository.AlarmeRepository
import com.katui.offline.data.repository.MedicamentoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class EstoqueBaixoAlerta(
    val nomeMedicamento: String,
    val estoqueRestante: Double,
    val unidade: String
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmeRepository,
    private val medicamentoRepository: MedicamentoRepository,
    private val usuarioDao: com.katui.offline.data.local.dao.UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _alarmes = MutableStateFlow<List<Alarme>>(emptyList())
    val alarmes: StateFlow<List<Alarme>> = _alarmes.asStateFlow()

    private val _usuario = MutableStateFlow<com.katui.offline.data.local.entities.Usuario?>(null)
    val usuario: StateFlow<com.katui.offline.data.local.entities.Usuario?> = _usuario.asStateFlow()

    private val _medicamentos = MutableStateFlow<List<Medicamento>>(emptyList())
    val medicamentos: StateFlow<List<Medicamento>> = _medicamentos.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _estoqueBaixoAlerta = MutableStateFlow<EstoqueBaixoAlerta?>(null)
    val estoqueBaixoAlerta: StateFlow<EstoqueBaixoAlerta?> = _estoqueBaixoAlerta.asStateFlow()

    fun dismissEstoqueAlerta() { _estoqueBaixoAlerta.value = null }

    val filteredAlarmes = combine(_alarmes, _selectedDate) { list, date ->
        list.filter { it.horario.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getAlarmes(userId) }
            .onEach { _alarmes.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> medicamentoRepository.getMedicamentos(userId) }
            .onEach { _medicamentos.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getUsuarioPorId(userId) }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleTomado(alarme: Alarme) {
        viewModelScope.launch {
            val newStatus = !alarme.tomado
            repository.updateStatus(alarme.id, newStatus)

            // Ao marcar como tomado, desconta a dose do estoque do medicamento.
            // Ao desmarcar, devolve a dose (caso o usuário tenha marcado por engano).
            alarme.medicamentoId?.let { medId ->
                val med = medicamentoRepository.getMedicamento(medId) ?: return@let
                val delta = if (newStatus) -med.dosePorTomada else med.dosePorTomada
                medicamentoRepository.ajustarEstoque(medId, delta)

                // Verifica se cruzou o limite mínimo ao marcar como tomado
                if (newStatus) {
                    val estoqueAtualizado = med.estoqueAtual - med.dosePorTomada
                    if (estoqueAtualizado <= med.estoqueMinimo && med.estoqueMinimo > 0) {
                        _estoqueBaixoAlerta.value = EstoqueBaixoAlerta(
                            nomeMedicamento = med.nome,
                            estoqueRestante = estoqueAtualizado,
                            unidade = med.unidadeMedida
                        )
                    }
                }
            }
        }
    }

    fun addAlarme(titulo: String, horario: LocalDateTime, medicamentoId: Long? = null) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.insertAlarme(Alarme(
                usuarioId = userId, 
                titulo = titulo, 
                horario = horario, 
                medicamentoId = medicamentoId
            ))
        }
    }

    fun deleteAlarme(alarme: Alarme) {
        viewModelScope.launch {
            repository.deleteAlarme(alarme)
        }
    }

    // Usado quando o usuário escolhe "todos os futuros" no diálogo de exclusão,
    // em vez de excluir só a ocorráncia daquele dia.
    fun deleteAlarmeETodosFuturos(alarme: Alarme) {
        viewModelScope.launch {
            val medId = alarme.medicamentoId
            if (medId != null) {
                repository.deleteFuturosDoMedicamento(medId)
            } else {
                repository.deleteAlarme(alarme)
            }
        }
    }

    fun updateSingleAlarme(id: Long, titulo: String, horario: LocalDateTime) {
        viewModelScope.launch {
            val currentAlarm = repository.alarmeDao.getAlarmeById(id) ?: return@launch
            val updatedAlarm = currentAlarm.copy(
                titulo = titulo,
                horario = horario
            )
            repository.insertAlarme(updatedAlarm) // O insert com REPLACE cuida de reagendar
        }
    }
}
