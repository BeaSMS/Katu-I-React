package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Vitals
import com.katui.offline.data.repository.UsuarioRepository
import com.katui.offline.data.repository.VitalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class TimeRange { DAY, WEEK, MONTH }

// Tipos de sinal filtráveis na lista histórica
enum class VitalTipo(val label: String) {
    TODOS("Todos"),
    PRESSAO("Pressão"),
    GLICOSE("Glicose"),
    PESO("Peso"),
    ALTURA("Altura"),
    BATIMENTOS("Batimentos"),
    SATURACAO("Saturação"),
    TEMPERATURA("Temperatura")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val repository: VitalsRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.DAY)
    val selectedTimeRange = _selectedTimeRange.asStateFlow()

    // Filtro de calendário para o histórico
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    // Filtro de tipo de sinal para o histórico
    private val _selectedTipo = MutableStateFlow(VitalTipo.TODOS)
    val selectedTipo = _selectedTipo.asStateFlow()

    val usuario = sessionManager.userId
        .flatMapLatest { usuarioRepository.getUsuarioPorId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vitals = sessionManager.userId
        .flatMapLatest { repository.getVitals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vitals filtrados para o GRÁFICO (por TimeRange)
    val filteredVitals = combine(vitals, _selectedTimeRange) { list, range ->
        val now = LocalDateTime.now()
        val start = when (range) {
            TimeRange.DAY -> now.minusDays(1)
            TimeRange.WEEK -> now.minusWeeks(1)
            TimeRange.MONTH -> now.minusMonths(1)
        }
        list.filter { it.dataHora.isAfter(start) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vitals filtrados para o HISTÓRICO (por data do calendário + tipo de sinal)
    val historicVitals = combine(vitals, _selectedDate, _selectedTipo) { list, date, tipo ->
        list
            .filter { it.dataHora.toLocalDate() == date }
            .filter { v ->
                when (tipo) {
                    VitalTipo.TODOS -> true
                    VitalTipo.PRESSAO -> v.sistolica != null
                    VitalTipo.GLICOSE -> v.glicose != null
                    VitalTipo.PESO -> v.peso != null
                    VitalTipo.ALTURA -> v.alturaCm != null
                    VitalTipo.BATIMENTOS -> v.frequenciaCardiaca != null
                    VitalTipo.SATURACAO -> v.saturacaoOxigenio != null
                    VitalTipo.TEMPERATURA -> v.temperatura != null
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeRange(range: TimeRange) { _selectedTimeRange.value = range }
    fun setSelectedDate(date: LocalDate) { _selectedDate.value = date }
    fun setSelectedTipo(tipo: VitalTipo) { _selectedTipo.value = tipo }

    fun addVitals(
        peso: Double? = null, alturaCm: Int? = null, sistolica: Int? = null,
        diastolica: Int? = null, frequenciaCardiaca: Int? = null, glicose: Int? = null,
        saturacao: Int? = null, temperatura: Double? = null, obs: String? = null,
        dataHora: LocalDateTime = LocalDateTime.now()
    ) {
        // Conversão automática para pressão arterial (ex: 12/8 -> 120/80)
        // Se o valor for menor que 40, assumimos que o usuário está usando a notação "por" (ex: 12 por 8)
        val sFix = if (sistolica != null && sistolica > 0 && sistolica < 40) sistolica * 10 else sistolica
        val dFix = if (diastolica != null && diastolica > 0 && diastolica < 40) diastolica * 10 else diastolica

        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.insertVitals(Vitals(
                usuarioId = userId, peso = peso, alturaCm = alturaCm,
                sistolica = sFix, diastolica = dFix,
                frequenciaCardiaca = frequenciaCardiaca, glicose = glicose,
                saturacaoOxigenio = saturacao, temperatura = temperatura, observacoes = obs,
                dataHora = dataHora
            ))
        }
    }

    fun deleteVitals(vitals: Vitals) {
        viewModelScope.launch { repository.deleteVitals(vitals) }
    }
}
