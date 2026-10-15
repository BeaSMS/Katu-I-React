package com.katui.offline.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Alarme
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.local.entities.MedicamentoCatalogo
import com.katui.offline.data.repository.AlarmeRepository
import com.katui.offline.data.repository.MedicamentoRepository
import com.katui.offline.data.repository.FemaleHealthRepository
import com.katui.offline.util.OCRManager
import com.katui.offline.util.PdfManager
import com.katui.offline.util.PdfUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val repository: MedicamentoRepository,
    private val femaleHealthRepository: FemaleHealthRepository,
    private val alarmeRepository: AlarmeRepository,
    private val sessionManager: SessionManager,
    private val ocrManager: OCRManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<Medicamento>>(emptyList())
    val medicamentos: StateFlow<List<Medicamento>> = _medicamentos.asStateFlow()

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

    private val _extractedMeds = MutableStateFlow<List<Medicamento>>(emptyList())
    val extractedMeds: StateFlow<List<Medicamento>> = _extractedMeds.asStateFlow()

    fun setExtractedMeds(meds: List<Medicamento>) {
        _extractedMeds.value = meds
    }

    fun clearExtractedMeds() {
        _extractedMeds.value = emptyList()
    }

    fun updateExtractedMed(index: Int, med: Medicamento) {
        val current = _extractedMeds.value.toMutableList()
        if (index in current.indices) {
            current[index] = med
            _extractedMeds.value = current
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredMedicamentos = combine(_medicamentos, _selectedDate) { list, date ->
        list.filter { med ->
            val start = med.dataInicio
            val end = med.dataFim
            (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getMedicamentos(userId) }
            .onEach { _medicamentos.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> repository.getUsuario(userId) }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private val _saveStatus = MutableStateFlow<Result<Unit>?>(null)
    val saveStatus: StateFlow<Result<Unit>?> = _saveStatus.asStateFlow()

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun saveImportedMedications(
        meds: List<Medicamento>,
        generateAlarms: Boolean,
        prescriptionUri: Uri?,
        prescriptionObs: String
    ) {
        viewModelScope.launch {
            try {
                _saveStatus.value = null
                val userId = sessionManager.userId.first()
                
                android.util.Log.d("KATUI_SAVE", "Iniciando salvamento em lote de ${meds.size} medicamentos")

                meds.forEach { med ->
                    val existingMed = repository.getMedicamentoByName(userId, med.nome)
                    if (existingMed != null) {
                        android.util.Log.d("KATUI_SAVE", "Atualizando medicamento existente: ${med.nome} (Fim: ${med.dataFim})")
                        val updatedMed = existingMed.copy(
                            horario = med.horario,
                            tipoFrequencia = med.tipoFrequencia,
                            valorFrequencia = med.valorFrequencia,
                            dias = med.dias,
                            finalidade = med.finalidade ?: existingMed.finalidade,
                            dosagem = med.dosagem ?: existingMed.dosagem,
                            dataInicio = med.dataInicio ?: existingMed.dataInicio,
                            dataFim = med.dataFim ?: existingMed.dataFim,
                            observacoes = med.observacoes ?: existingMed.observacoes,
                            estoqueAtual = existingMed.estoqueAtual + med.estoqueAtual,
                            estoqueMinimo = med.estoqueMinimo,
                            dosePorTomada = med.dosePorTomada,
                            unidadeMedida = med.unidadeMedida,
                            ativo = true
                        )
                        repository.updateMedicamento(updatedMed)
                        if (generateAlarms) {
                            alarmeRepository.deletePendentesDoMedicamento(updatedMed.id)
                            alarmeRepository.generateAlarmsForMedication(updatedMed)
                        }
                    } else {
                        android.util.Log.d("KATUI_SAVE", "Inserindo novo medicamento: ${med.nome} (Fim: ${med.dataFim})")
                        val medWithUserId = med.copy(usuarioId = userId)
                        val medId = repository.insertMedicamento(medWithUserId)
                        if (generateAlarms) {
                            alarmeRepository.generateAlarmsForMedication(medWithUserId.copy(id = medId))
                        }
                    }
                }

                // Log final de verificação
                val totalNoBanco = repository.getMedicamentos(userId).first().size
                android.util.Log.d("KATUI_SAVE", "Total de medicamentos no banco para usuário $userId: $totalNoBanco")

                prescriptionUri?.let { uri ->
                    android.util.Log.d("KATUI_SAVE", "Copiando e salvando imagem da receita: $uri")
                    
                    val isPdf = PdfUtils.isPdf(context, uri)
                    val savedFile = withContext(Dispatchers.IO) {
                        if (isPdf) PdfManager.copyPdfToInternal(context, uri)
                        else PdfManager.copyImageToInternal(context, uri)
                    }

                    repository.insertReceita(
                        com.katui.offline.data.local.entities.Receita(
                            usuarioId = userId,
                            imagemPath = savedFile?.absolutePath,
                            observacao = prescriptionObs,
                            isPdf = isPdf
                        )
                    )
                }

                android.util.Log.d("KATUI_SAVE", "Salvamento concluído com sucesso")
                _saveStatus.value = Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("KATUI_SAVE", "Erro ao salvar medicamentos/receita", e)
                _saveStatus.value = Result.failure(e)
            }
        }
    }

    fun addMedicamento(medicamento: Medicamento, generateAlarms: Boolean) {
        viewModelScope.launch {
            try {
                android.util.Log.d("KATUI_SAVE", "Adicionando medicamento manual: ${medicamento.nome}")
                val userId = sessionManager.userId.first()
                val medWithUserId = medicamento.copy(usuarioId = userId)
                val medId = repository.insertMedicamento(medWithUserId)
                if (generateAlarms) {
                    alarmeRepository.generateAlarmsForMedication(medWithUserId.copy(id = medId))
                }
            } catch (e: Exception) {
                android.util.Log.e("KATUI_SAVE", "Erro ao adicionar medicamento manual", e)
            }
        }
    }

    fun updateMedicamento(medicamento: Medicamento, regenerateAlarms: Boolean) {
        viewModelScope.launch {
            try {
                android.util.Log.d("KATUI_SAVE", "Atualizando medicamento manual: ${medicamento.nome}")
                repository.updateMedicamento(medicamento)
                if (regenerateAlarms) {
                    alarmeRepository.deletePendentesDoMedicamento(medicamento.id)
                    alarmeRepository.generateAlarmsForMedication(medicamento)
                }
            } catch (e: Exception) {
                android.util.Log.e("KATUI_SAVE", "Erro ao atualizar medicamento manual", e)
            }
        }
    }

    fun deleteMedicamento(medicamento: Medicamento) {
        viewModelScope.launch {
            alarmeRepository.deletePendentesDoMedicamento(medicamento.id)
            repository.deleteMedicamento(medicamento)
        }
    }

    suspend fun extractMedications(context: Context, uri: Uri): List<Medicamento> {
        val userId = sessionManager.userId.first()
        return ocrManager.extractMedicationsFromImage(context, uri, userId)
    }

    fun savePrescriptionImage(uri: Uri, observation: String) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            val isPdf = PdfUtils.isPdf(context, uri)
            val savedFile = withContext(Dispatchers.IO) {
                if (isPdf) PdfManager.copyPdfToInternal(context, uri)
                else PdfManager.copyImageToInternal(context, uri)
            }
            
            repository.insertReceita(
                com.katui.offline.data.local.entities.Receita(
                    usuarioId = userId,
                    imagemPath = savedFile?.absolutePath,
                    observacao = observation,
                    isPdf = isPdf
                )
            )
        }
    }
}
