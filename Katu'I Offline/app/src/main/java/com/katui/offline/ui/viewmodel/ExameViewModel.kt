package com.katui.offline.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Exame
import com.katui.offline.data.local.entities.ExameResultado
import com.katui.offline.data.repository.ExameRepository
import com.katui.offline.util.ExamCategorizer
import com.katui.offline.util.OCRManager
import com.katui.offline.util.PdfManager
import com.katui.offline.util.TimeScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExameViewModel @Inject constructor(
    private val repository: ExameRepository,
    private val usuarioDao: com.katui.offline.data.local.dao.UsuarioDao,
    private val sessionManager: SessionManager,
    private val ocrManager: OCRManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _exames = MutableStateFlow<List<Exame>>(emptyList())
    val exames: StateFlow<List<Exame>> = _exames.asStateFlow()

    private val _usuario = MutableStateFlow<com.katui.offline.data.local.entities.Usuario?>(null)
    val usuario: StateFlow<com.katui.offline.data.local.entities.Usuario?> = _usuario.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _timeScope = MutableStateFlow(TimeScope.DAY)
    val timeScope: StateFlow<TimeScope> = _timeScope.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Resultados de um exame específico
    private val _selectedExameResultados = MutableStateFlow<List<ExameResultado>>(emptyList())
    val selectedExameResultados: StateFlow<List<ExameResultado>> = _selectedExameResultados.asStateFlow()

    // Métricas distintas já registradas pelo usuário atual (para abas do gráfico de tendências)
    private val _metricasDisponiveis = MutableStateFlow<List<String>>(emptyList())
    val metricasDisponiveis: StateFlow<List<String>> = _metricasDisponiveis.asStateFlow()

    private val _metricasComDadosSuficientes = MutableStateFlow<Set<String>>(emptySet())
    val metricasComDadosSuficientes: StateFlow<Set<String>> = _metricasComDadosSuficientes.asStateFlow()

    // Estado de loading para operações de PDF (pode demorar)
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Resultados extraídos pela IA
    private val _extractedResults = MutableStateFlow<List<ExameResultado>>(emptyList())
    val extractedResults: StateFlow<List<ExameResultado>> = _extractedResults.asStateFlow()

    // Mensagem de erro da última tentativa de extração por IA (null = sem erro pendente)
    private val _extractionError = MutableStateFlow<String?>(null)
    val extractionError: StateFlow<String?> = _extractionError.asStateFlow()

    val filteredExames = combine(_exames, _selectedDate, _selectedCategory, _timeScope) { list, date, category, scope ->
        list.filter { exame ->
            val matchesScope = when (scope) {
                TimeScope.ALL -> true
                TimeScope.MONTH -> {
                    val eDate = exame.dataRegistro.toLocalDate()
                    eDate.year == date.year && eDate.month == date.month
                }
                TimeScope.DAY -> exame.dataRegistro.toLocalDate() == date
            }
            matchesScope && (category == null || exame.categoria == category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getExames(userId) }
            .onEach { _exames.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> repository.getMetricasDisponiveis(userId) }
            .onEach { _metricasDisponiveis.value = it }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> repository.getMetricasComDadosSuficientes(userId) }
            .onEach { list -> _metricasComDadosSuficientes.value = list.map { it.metrica }.toSet() }
            .launchIn(viewModelScope)

        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getUsuarioPorId(userId) }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectTimeScope(scope: TimeScope) {
        _timeScope.value = scope
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun extractMetrics(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _extractionError.value = null
            try {
                val results = ocrManager.extractExamMetrics(context, uri)
                _extractedResults.value = results
                if (results.isEmpty()) {
                    _extractionError.value = "A IA não encontrou nenhum valor reconhecível nesta imagem. Tente outra foto ou adicione os valores manualmente."
                }
            } catch (e: Exception) {
                Log.e("KATUI_DEBUG", "Falha na extração de métricas", e)
                _extractionError.value = e.message ?: "Erro inesperado ao extrair os valores do exame."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearExtractionError() {
        _extractionError.value = null
    }

    fun clearExtractedResults() {
        _extractedResults.value = emptyList()
    }

    fun loadResultados(exameId: Long) {
        viewModelScope.launch {
            repository.getExameComResultados(exameId).collect {
                _selectedExameResultados.value = it.resultados
            }
        }
    }

    fun getHistoricoMetrica(metrica: String): Flow<List<ExameResultado>> {
        return sessionManager.userId.flatMapLatest { userId ->
            repository.getHistoricoPorMetrica(metrica, userId).map { resultados ->
                resultados.map { res ->
                    val normalizedValue = com.katui.offline.util.MedicalUnitConverter.normalize(
                        res.valor,
                        res.unidade,
                        res.metrica
                    )
                    res.copy(
                        valor = normalizedValue,
                        unidade = com.katui.offline.util.MedicalUnitConverter.STANDARD_UNITS[res.metrica] ?: res.unidade
                    )
                }
            }
        }
    }

    /** Adiciona exame com resultados opcionais. */
    fun addExame(nome: String, categoria: String, observacao: String?, arquivoUriStr: String? = null, resultados: List<ExameResultado> = emptyList()) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            
            // Copia imagem para armazenamento interno se for um URI
            val finalPath = arquivoUriStr?.let { uriStr ->
                try {
                    val uri = Uri.parse(uriStr)
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        withContext(Dispatchers.IO) {
                            PdfManager.copyImageToInternal(context, uri)?.absolutePath
                        }
                    } else uriStr
                } catch (e: Exception) {
                    arquivoUriStr
                }
            }

            val exameId = repository.insertExame(
                Exame(
                    usuarioId = userId,
                    nome = nome,
                    categoria = categoria,
                    observacao = observacao,
                    arquivoPath = finalPath,
                    isPdf = false
                )
            )
            resultados.forEach {
                repository.insertResultado(it.copy(exameId = exameId))
            }
        }
    }

    /** Adiciona exame a partir de PDF com resultados opcionais. */
    fun addExameFromPdf(context: Context, nome: String, categoria: String, observacao: String?, pdfUri: Uri, resultados: List<ExameResultado> = emptyList()) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val savedFile = withContext(Dispatchers.IO) {
                    PdfManager.copyPdfToInternal(context, pdfUri)
                }
                val userId = sessionManager.userId.first()
                val exameId = repository.insertExame(
                    Exame(
                        usuarioId = userId,
                        nome = nome,
                        categoria = categoria,
                        observacao = observacao,
                        arquivoPath = savedFile?.absolutePath,
                        isPdf = true
                    )
                )
                resultados.forEach {
                    repository.insertResultado(it.copy(exameId = exameId))
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Converte múltiplas imagens em PDF e adiciona com resultados opcionais. */
    fun addExameFromImages(context: Context, nome: String, categoria: String, observacao: String?, imageUris: List<Uri>, resultados: List<ExameResultado> = emptyList()) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfManager.imagesToPdf(context, imageUris)
                }
                val userId = sessionManager.userId.first()
                val exameId = repository.insertExame(
                    Exame(
                        usuarioId = userId,
                        nome = nome,
                        categoria = categoria,
                        observacao = observacao,
                        arquivoPath = pdfFile?.absolutePath,
                        isPdf = true
                    )
                )
                resultados.forEach {
                    repository.insertResultado(it.copy(exameId = exameId))
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // --- Modo Consulta Médica ---

    private val _consultaPdfPath = MutableStateFlow<String?>(null)
    val consultaPdfPath: StateFlow<String?> = _consultaPdfPath.asStateFlow()

    fun clearConsultaPdf() { _consultaPdfPath.value = null }

    /** Sugere exames pré-selecionados com base na especialidade do médico. */
    fun sugerirExamesParaConsulta(especialidade: String): List<Exame> {
        val categoriasRel = ExamCategorizer.categoriasPorEspecialidade(especialidade).toSet()
        val todos = _exames.value
        // Pega os 2 mais recentes de cada categoria relevante
        return categoriasRel.flatMap { cat ->
            todos.filter { it.categoria == cat }.take(2)
        }.distinctBy { it.id }
    }

    /** Gera o PDF consolidado e salva o caminho em [consultaPdfPath]. */
    fun gerarPdfConsulta(
        context: Context,
        nomesPaciente: String,
        especialidade: String,
        examesSelecionados: List<Exame>
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Busca resultados de cada exame selecionado
                val resultadosPorExame = mutableMapOf<Long, List<ExameResultado>>()
                examesSelecionados.forEach { exame ->
                    repository.getExameComResultados(exame.id).first().let {
                        resultadosPorExame[exame.id] = it.resultados
                    }
                }
                val file = withContext(Dispatchers.IO) {
                    PdfManager.gerarPdfConsulta(
                        context = context,
                        nomesPaciente = nomesPaciente,
                        especialidade = especialidade,
                        exames = examesSelecionados,
                        resultadosPorExame = resultadosPorExame
                    )
                }
                _consultaPdfPath.value = file?.absolutePath
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteExame(exame: Exame) {
        viewModelScope.launch {
            repository.deleteExame(exame)
        }
    }
}
