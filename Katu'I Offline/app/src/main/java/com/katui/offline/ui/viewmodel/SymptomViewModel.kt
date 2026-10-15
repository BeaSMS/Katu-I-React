package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.dao.MedicamentoCausador
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.Sintoma
import com.katui.offline.data.local.entities.SintomaCatalogo
import com.katui.offline.data.repository.MedicamentoRepository
import com.katui.offline.data.repository.SintomaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SymptomViewModel @Inject constructor(
    private val repository: SintomaRepository,
    private val medicamentoRepository: MedicamentoRepository,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _sintomas = MutableStateFlow<List<Sintoma>>(emptyList())
    val sintomas: StateFlow<List<Sintoma>> = _sintomas.asStateFlow()

    private val _usuario = MutableStateFlow<com.katui.offline.data.local.entities.Usuario?>(null)
    val usuario: StateFlow<com.katui.offline.data.local.entities.Usuario?> = _usuario.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) {
                repository.searchCatalogo(query)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Lista de medicamentos ativos hoje que podem causar o sintoma selecionado no autocomplete.
    // Fica vazia quando nao ha correspondencia ou quando nenhum sintoma do catalogo foi escolhido.
    private val _alertaEfeitoColateral = MutableStateFlow<List<MedicamentoCausador>>(emptyList())
    val alertaEfeitoColateral: StateFlow<List<MedicamentoCausador>> = _alertaEfeitoColateral.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredSintomas = combine(_sintomas, _selectedDate) { list, date ->
        list.filter { it.dataHoraRegistro.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getSintomas(userId) }
            .onEach { _sintomas.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getUsuarioPorId(userId) }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // Chamado quando o usuario seleciona um sintoma do catalogo (autocomplete). Cruza com os
    // medicamentos ativos hoje para avisar se aquele sintoma e um efeito colateral conhecido.
    fun verificarEfeitoColateral(sintomaCatalogoId: Long?) {
        if (sintomaCatalogoId == null) {
            _alertaEfeitoColateral.value = emptyList()
            return
        }
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            val hoje = LocalDate.now()
            val medicamentosAtivosCatalogoIds = medicamentoRepository.getMedicamentos(userId)
                .first()
                .filter { med ->
                    med.ativo &&
                        (med.dataInicio == null || !hoje.isBefore(med.dataInicio)) &&
                        (med.dataFim == null || !hoje.isAfter(med.dataFim))
                }
                .mapNotNull { it.catalogoId }
                .distinct()

            _alertaEfeitoColateral.value = repository.getMedicamentosQueCausamSintoma(
                sintomaCatalogoId,
                medicamentosAtivosCatalogoIds
            )
        }
    }

    fun limparAlertaEfeitoColateral() {
        _alertaEfeitoColateral.value = emptyList()
    }

    fun addSintoma(sintoma: Sintoma) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.insertSintoma(sintoma.copy(usuarioId = userId))
        }
    }

    fun updateSintoma(sintoma: Sintoma) {
        viewModelScope.launch {
            repository.updateSintoma(sintoma)
        }
    }

    fun deleteSintoma(sintoma: Sintoma) {
        viewModelScope.launch {
            repository.deleteSintoma(sintoma)
        }
    }
}
