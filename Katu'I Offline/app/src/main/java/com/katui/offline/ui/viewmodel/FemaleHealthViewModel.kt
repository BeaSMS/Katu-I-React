package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.*
import com.katui.offline.data.repository.FemaleHealthRepository
import com.katui.offline.data.repository.MedicamentoRepository
import com.katui.offline.data.repository.AlarmeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FemaleHealthViewModel @Inject constructor(
    private val repository: FemaleHealthRepository,
    private val medicamentoRepository: MedicamentoRepository,
    private val alarmeRepository: AlarmeRepository,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _ciclos = MutableStateFlow<List<CicloMenstrual>>(emptyList())
    val ciclos: StateFlow<List<CicloMenstrual>> = _ciclos.asStateFlow()

    private val _sintomasDiarios = MutableStateFlow<List<SintomaDiarioFeminino>>(emptyList())
    val sintomasDiarios: StateFlow<List<SintomaDiarioFeminino>> = _sintomasDiarios.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    val anticoncepcional = sessionManager.userId.flatMapLatest { userId ->
        repository.getAnticoncepcionalAtivo(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val registrosAnti = anticoncepcional.flatMapLatest { anti ->
        anti?.let { repository.getRegistrosAnti(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaCiclo = _ciclos.map { lista ->
        if (lista.size < 2) 28 else {
            val intervalos = mutableListOf<Long>()
            for (i in 0 until lista.size - 1) {
                intervalos.add(ChronoUnit.DAYS.between(lista[i+1].dataInicio, lista[i].dataInicio))
            }
            intervalos.average().toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28)

    val mediaDuracao = _ciclos.map { lista ->
        val duracoes = lista.filter { it.dataFim != null }.map {
            ChronoUnit.DAYS.between(it.dataInicio, it.dataFim) + 1
        }
        if (duracoes.isEmpty()) 5 else duracoes.average().toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val proximaMenstruacao = combine(_ciclos, mediaCiclo) { lista, media ->
        lista.firstOrNull()?.dataInicio?.plusDays(media.toLong())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val diasFerteis = combine(_ciclos, mediaCiclo) { lista, media ->
        val ultimoInicio = lista.firstOrNull()?.dataInicio ?: return@combine emptyList<LocalDate>()
        val diaOvulacao = ultimoInicio.plusDays(media.toLong() - 14)
        // Janela fértil: 3 dias antes e 1 dia depois da ovulação
        (-3..1).map { diaOvulacao.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previsaoProximosDiasFerteis = combine(proximaMenstruacao, mediaCiclo) { proxima, media ->
        val inicio = proxima ?: return@combine emptyList<LocalDate>()
        val diaOvulacao = inicio.plusDays(media.toLong() - 14)
        (-3..1).map { diaOvulacao.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dias estimados de continuação do ciclo em aberto (a partir do 2º dia), usando a duração média
     *  saudável (histórico da usuária ou padrão de 5 dias). Mostrados em cor clara até o ciclo ser
     *  efetivamente encerrado, já que ainda não são dias confirmados. */
    val diasEstimadosCicloAberto = combine(_ciclos, mediaDuracao) { lista, duracao ->
        val cicloAberto = lista.firstOrNull { it.dataFim == null } ?: return@combine emptyList<LocalDate>()
        if (duracao <= 1) emptyList() else (1 until duracao).map { cicloAberto.dataInicio.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dias estimados do próximo período, a partir da previsão de início e da duração média saudável. */
    val previsaoProximoPeriodo = combine(proximaMenstruacao, mediaDuracao) { proxima, duracao ->
        if (proxima == null) emptyList() else (0 until duracao).map { proxima.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId.flatMapLatest { userId ->
            repository.getCiclos(userId)
        }.onEach { _ciclos.value = it }.launchIn(viewModelScope)

        sessionManager.userId.flatMapLatest { userId ->
            repository.getSintomasDiarios(userId)
        }.onEach { _sintomasDiarios.value = it }.launchIn(viewModelScope)

        sessionManager.userId.flatMapLatest { userId ->
            usuarioDao.getUsuarioPorId(userId)
        }.onEach { _usuario.value = it }.launchIn(viewModelScope)

        // Sincronização: garante que se houver um anticoncepcional ativo, ele exista como "espelho" nos remédios gerais
        anticoncepcional.onEach { anti ->
            if (anti != null && anti.ativo) {
                val userId = anti.usuarioId
                val existingMeds = medicamentoRepository.getMedicamentos(userId).first()
                val mirror = existingMeds.find { it.finalidade == "Anticoncepcional" }
                
                if (mirror == null) {
                    // Cria o espelho se não existir
                    val duration = if (anti.tipoCartela == 0) null else 30
                    val medId = medicamentoRepository.insertMedicamento(Medicamento(
                        usuarioId = userId,
                        nome = anti.nome,
                        horario = anti.horarioTomada,
                        tipoFrequencia = "INTERVALO_HORAS",
                        valorFrequencia = 24,
                        dias = duration,
                        finalidade = "Anticoncepcional",
                        dosagem = if (anti.tipoCartela == 0) "Uso Contínuo" else "Cartela ${anti.tipoCartela} dias",
                        dataInicio = anti.dataInicio,
                        dataFim = duration?.let { anti.dataInicio.plusDays(it.toLong()) },
                        observacoes = "Integrado com Saúde Feminina",
                        ativo = true
                    ))
                    val newMed = medicamentoRepository.getMedicamento(medId)
                    if (newMed != null) {
                        alarmeRepository.generateAlarmsForMedication(newMed)
                    }
                } else if (mirror.nome != anti.nome || mirror.horario != anti.horarioTomada) {
                    // Atualiza o espelho se mudou o nome ou horário na saúde feminina
                    val updatedMirror = mirror.copy(
                        nome = anti.nome,
                        horario = anti.horarioTomada,
                        dosagem = if (anti.tipoCartela == 0) "Uso Contínuo" else "Cartela ${anti.tipoCartela} dias"
                    )
                    medicamentoRepository.updateMedicamento(updatedMirror)
                    // Re-gera alarmes se o horário mudou
                    if (mirror.horario != anti.horarioTomada) {
                        alarmeRepository.deletePendentesDoMedicamento(mirror.id)
                        alarmeRepository.generateAlarmsForMedication(updatedMirror)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun registrarInicioCiclo(data: LocalDate) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.insertCiclo(CicloMenstrual(usuarioId = userId, dataInicio = data))
        }
    }

    fun registrarFimCiclo(ciclo: CicloMenstrual, data: LocalDate) {
        viewModelScope.launch {
            repository.updateCiclo(ciclo.copy(dataFim = data))
        }
    }

    /** Permite corrigir manualmente as datas de início e/ou fim de um ciclo já registrado
     *  (ex: usuária errou o dia ao registrar, ou quer lançar um período retroativo completo). */
    fun editarCiclo(ciclo: CicloMenstrual, novoInicio: LocalDate, novoFim: LocalDate?) {
        viewModelScope.launch {
            repository.updateCiclo(ciclo.copy(dataInicio = novoInicio, dataFim = novoFim))
        }
    }
    
    fun salvarSintomasDiarios(data: LocalDate, intensidade: IntensidadeFluxo, sintomas: String, humor: String) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            repository.insertOuUpdateSintomaDiario(SintomaDiarioFeminino(
                usuarioId = userId,
                data = data,
                intensidade = intensidade,
                sintomas = sintomas,
                humor = humor
            ))
        }
    }

    fun excluirCiclo(ciclo: CicloMenstrual) {
        viewModelScope.launch {
            repository.deleteCiclo(ciclo)
        }
    }

    // Anticoncepcional
    fun configurarAnticoncepcional(nome: String, tipo: Int, horario: String) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            
            // 1. Salva na tabela específica de Saúde Feminina
            repository.insertAnticoncepcional(Anticoncepcional(
                usuarioId = userId,
                nome = nome,
                tipoCartela = tipo,
                dataInicio = LocalDate.now(),
                horarioTomada = horario
            ))

            // 2. Integra aos Remédios gerais para aparecer no Dashboard e Alarmes
            // Primeiro, remove qualquer "espelho" anterior do anticoncepcional
            val existingMeds = medicamentoRepository.getMedicamentos(userId).first()
            existingMeds.find { it.finalidade == "Anticoncepcional" }?.let {
                medicamentoRepository.deleteMedicamento(it)
            }

            // Cria o novo medicamento espelho
            val duration = if (tipo == 0) null else 30 // Damos uma base de 30 dias se não for contínuo
            val medId = medicamentoRepository.insertMedicamento(Medicamento(
                usuarioId = userId,
                nome = nome,
                horario = horario,
                tipoFrequencia = "INTERVALO_HORAS",
                valorFrequencia = 24,
                dias = duration,
                finalidade = "Anticoncepcional",
                dosagem = if (tipo == 0) "Uso Contínuo" else "Cartela $tipo dias",
                dataInicio = LocalDate.now(),
                dataFim = duration?.let { LocalDate.now().plusDays(it.toLong()) },
                observacoes = "Integrado com Saúde Feminina",
                ativo = true
            ))

            // Gera os alarmes padrão
            val newMed = medicamentoRepository.getMedicamento(medId)
            if (newMed != null) {
                alarmeRepository.generateAlarmsForMedication(newMed)
            }
        }
    }

    fun pararAnticoncepcional() {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            val anti = anticoncepcional.value ?: return@launch
            
            // 1. Desativa na tabela de Saúde Feminina
            repository.updateAnticoncepcional(anti.copy(ativo = false))
            
            // 2. Remove o espelho nos remédios gerais
            val existingMeds = medicamentoRepository.getMedicamentos(userId).first()
            existingMeds.find { it.finalidade == "Anticoncepcional" }?.let {
                medicamentoRepository.deleteMedicamento(it)
            }
        }
    }

    fun registrarTomadaPilha(tomado: Boolean) {
        viewModelScope.launch {
            val anti = anticoncepcional.value ?: return@launch
            repository.registrarTomada(anti.id, LocalDate.now(), tomado)
        }
    }

    fun registrarDadosRetroativos(data: LocalDate, intensidade: IntensidadeFluxo, sintomas: String, humor: String) {
        salvarSintomasDiarios(data, intensidade, sintomas, humor)
    }
}
