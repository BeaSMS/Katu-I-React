package com.katui.offline.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.Alarme
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.data.local.entities.Vacina
import com.katui.offline.data.repository.AlarmeRepository
import com.katui.offline.data.repository.VacinaRepository
import com.katui.offline.util.VacinaScheduleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VaccinationViewModel @Inject constructor(
    private val repository: VacinaRepository,
    private val alarmeRepository: AlarmeRepository,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _vacinas = MutableStateFlow<List<Vacina>>(emptyList())
    val vacinas: StateFlow<List<Vacina>> = _vacinas.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userId.collect { userId ->
                if (repository.getContagemVacinas(userId) == 0) prepopularVacinas(userId)
            }
        }
        sessionManager.userId.flatMapLatest { repository.getVacinas(it) }
            .onEach { _vacinas.value = it }.launchIn(viewModelScope)
        sessionManager.userId.flatMapLatest { usuarioDao.getUsuarioPorId(it) }
            .onEach { _usuario.value = it }.launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ações públicas
    // ─────────────────────────────────────────────────────────────────────────

    fun registrarVacinaComData(vacina: Vacina, data: LocalDate) {
        viewModelScope.launch {
            val atualizada = vacina.copy(tomada = true, dataTomada = data)
            repository.atualizarVacina(atualizada)
            agendarAlarmeSePreciso(atualizada, data)
        }
    }

    fun toggleVacina(vacina: Vacina) {
        viewModelScope.launch {
            val hoje = LocalDate.now()
            val atualizada = if (!vacina.tomada) {
                vacina.copy(tomada = true, dataTomada = hoje)
            } else {
                vacina.copy(tomada = false, dataTomada = null)
            }
            repository.atualizarVacina(atualizada)
            if (atualizada.tomada) agendarAlarmeSePreciso(atualizada, hoje)
        }
    }

    fun deletarVacina(vacina: Vacina) {
        viewModelScope.launch { repository.deletarVacina(vacina) }
    }

    fun adicionarVacinaManual(nome: String, data: LocalDate, observacao: String?) {
        viewModelScope.launch {
            sessionManager.userId.firstOrNull()?.let { userId ->
                repository.inserirVacina(
                    Vacina(
                        usuarioId = userId, nome = nome, dose = "Manual",
                        categoria = "Outras", tomada = true, dataTomada = data,
                        observacao = observacao
                    )
                )
            }
        }
    }

    /**
     * Adiciona uma nova dose avulsa a um grupo de vacinas (ex: novo reforço de COVID).
     * Calcula automaticamente o próximo número de dose dentro do grupo.
     */
    fun adicionarDoseAoGrupo(grupoVacina: String, categoria: String, data: LocalDate?,
                              observacao: String?, recorrente: Boolean = false,
                              mesesParaReforco: Int? = null, mesCampanha: Int? = null) {
        viewModelScope.launch {
            sessionManager.userId.firstOrNull()?.let { userId ->
                val maiorOrdem = repository.getMaiorOrdemDose(userId, grupoVacina) ?: 0
                val novaOrdem = maiorOrdem + 1
                val labelDose = numeroDoseLabel(novaOrdem)
                repository.inserirVacina(
                    Vacina(
                        usuarioId = userId,
                        nome = grupoVacina,
                        dose = labelDose,
                        categoria = categoria,
                        tomada = data != null,
                        dataTomada = data,
                        observacao = observacao,
                        grupoVacina = grupoVacina,
                        ordemDose = novaOrdem,
                        recorrente = recorrente,
                        mesesParaReforco = mesesParaReforco,
                        mesCampanha = mesCampanha,
                        permiteNovasDoses = true
                    )
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers internos
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun agendarAlarmeSePreciso(vacina: Vacina, dataTomada: LocalDate) {
        val dataReforco: LocalDate = when {
            vacina.mesCampanha != null ->
                VacinaScheduleHelper.proximoCicloMesPublico(vacina.mesCampanha, dataTomada)
            vacina.mesesParaReforco != null ->
                dataTomada.plusMonths(vacina.mesesParaReforco.toLong())
            else -> return
        }
        alarmeRepository.insertAlarme(Alarme(
            usuarioId = vacina.usuarioId,
            medicamentoId = null,
            titulo = "Lembrete: Próxima dose de ${vacina.nome}",
            horario = LocalDateTime.of(dataReforco, LocalTime.of(9, 0)),
            tomado = false
        ))
    }

    private fun numeroDoseLabel(ordem: Int): String = when (ordem) {
        1 -> "1ª Dose"; 2 -> "2ª Dose"; 3 -> "3ª Dose"; 4 -> "4ª Dose"; 5 -> "5ª Dose"
        else -> "${ordem}ª Dose"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prepopulação — Calendário Nacional de Vacinação PNI / Ministério da Saúde
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun prepopularVacinas(userId: Long) {
        val lista = mutableListOf<Vacina>()

        // ── Criança ───────────────────────────────────────────────────────────
        lista += Vacina(usuarioId = userId, nome = "BCG", dose = "Dose Única",
            categoria = "Criança", observacao = "Tuberculose — ao nascer")
        lista += Vacina(usuarioId = userId, nome = "Hepatite B", dose = "Ao Nascer",
            categoria = "Criança", grupoVacina = "Hepatite B", ordemDose = 1)
        lista += Vacina(usuarioId = userId, nome = "Pentavalente", dose = "1ª Dose",
            categoria = "Criança", observacao = "Difteria, Tétano, Coqueluche, Hep.B, Hib",
            grupoVacina = "Pentavalente", ordemDose = 1, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Pentavalente", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Pentavalente", ordemDose = 2, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Pentavalente", dose = "3ª Dose",
            categoria = "Criança", grupoVacina = "Pentavalente", ordemDose = 3, mesesParaReforco = 9)
        lista += Vacina(usuarioId = userId, nome = "Poliomielite (VIP)", dose = "1ª Dose",
            categoria = "Criança", grupoVacina = "Poliomielite", ordemDose = 1, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Poliomielite (VIP)", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Poliomielite", ordemDose = 2, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Poliomielite (VIP)", dose = "3ª Dose",
            categoria = "Criança", grupoVacina = "Poliomielite", ordemDose = 3, mesesParaReforco = 9)
        lista += Vacina(usuarioId = userId, nome = "Poliomielite (VOP)", dose = "Reforço",
            categoria = "Criança", grupoVacina = "Poliomielite", ordemDose = 4, mesesParaReforco = 33)
        lista += Vacina(usuarioId = userId, nome = "Pneumocócica 10v", dose = "1ª Dose",
            categoria = "Criança", grupoVacina = "Pneumocócica 10v", ordemDose = 1, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Pneumocócica 10v", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Pneumocócica 10v", ordemDose = 2, mesesParaReforco = 8)
        lista += Vacina(usuarioId = userId, nome = "Meningocócica C", dose = "1ª Dose",
            categoria = "Criança", grupoVacina = "Meningocócica C", ordemDose = 1, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Meningocócica C", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Meningocócica C", ordemDose = 2, mesesParaReforco = 7)
        lista += Vacina(usuarioId = userId, nome = "Febre Amarela", dose = "1ª Dose",
            categoria = "Criança", mesesParaReforco = 39)
        lista += Vacina(usuarioId = userId, nome = "Tríplice Viral (SCR)", dose = "1ª Dose",
            categoria = "Criança", observacao = "Sarampo, Caxumba, Rubéola",
            grupoVacina = "Tríplice Viral", ordemDose = 1, mesesParaReforco = 3)
        lista += Vacina(usuarioId = userId, nome = "Tríplice Viral (SCR)", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Tríplice Viral", ordemDose = 2)
        lista += Vacina(usuarioId = userId, nome = "Hepatite A", dose = "Dose Única", categoria = "Criança")
        lista += Vacina(usuarioId = userId, nome = "Varicela", dose = "Dose Única",
            categoria = "Criança", observacao = "Catapora")
        lista += Vacina(usuarioId = userId, nome = "Rotavírus", dose = "1ª Dose",
            categoria = "Criança", grupoVacina = "Rotavírus", ordemDose = 1, mesesParaReforco = 2)
        lista += Vacina(usuarioId = userId, nome = "Rotavírus", dose = "2ª Dose",
            categoria = "Criança", grupoVacina = "Rotavírus", ordemDose = 2)

        // ── Adolescente ───────────────────────────────────────────────────────
        lista += Vacina(usuarioId = userId, nome = "HPV", dose = "Dose Única",
            categoria = "Adolescente", observacao = "9–14 anos (meninas) / 11–14 anos (meninos)")
        lista += Vacina(usuarioId = userId, nome = "Meningocócica ACWY", dose = "Reforço",
            categoria = "Adolescente", observacao = "11–12 anos")
        lista += Vacina(usuarioId = userId, nome = "dTpa", dose = "1ª Dose",
            categoria = "Adolescente", observacao = "Difteria, Tétano, Coqueluche acelular")

        // ── Adulto ────────────────────────────────────────────────────────────
        // Hepatite B adulto — esquema 0-1-6 meses
        // Fonte: PNI / Butantan — indicado para adultos não vacinados
        lista += Vacina(usuarioId = userId, nome = "Hepatite B", dose = "1ª Dose (adulto)",
            categoria = "Adulto", grupoVacina = "Hepatite B adulto", ordemDose = 1,
            observacao = "Esquema 0-1-6 meses para não vacinados", mesesParaReforco = 1,
            permiteNovasDoses = false)
        lista += Vacina(usuarioId = userId, nome = "Hepatite B", dose = "2ª Dose (adulto)",
            categoria = "Adulto", grupoVacina = "Hepatite B adulto", ordemDose = 2,
            observacao = "1 mês após 1ª dose", mesesParaReforco = 5)
        lista += Vacina(usuarioId = userId, nome = "Hepatite B", dose = "3ª Dose (adulto)",
            categoria = "Adulto", grupoVacina = "Hepatite B adulto", ordemDose = 3,
            observacao = "6 meses após 1ª dose")

        lista += Vacina(usuarioId = userId, nome = "Dupla Adulto (dT)", dose = "Reforço",
            categoria = "Adulto", observacao = "Difteria e Tétano — reforço a cada 10 anos",
            recorrente = true, mesesParaReforco = 120)
        lista += Vacina(usuarioId = userId, nome = "Febre Amarela", dose = "Dose Única (adulto)",
            categoria = "Adulto", observacao = "Dose única com proteção para toda a vida")

        // ── Idoso ─────────────────────────────────────────────────────────────
        lista += Vacina(usuarioId = userId, nome = "Pneumocócica 23v", dose = "1ª Dose",
            categoria = "Idoso", observacao = "Idosos institucionalizados/acamados",
            grupoVacina = "Pneumocócica 23v", ordemDose = 1, mesesParaReforco = 60)
        lista += Vacina(usuarioId = userId, nome = "Pneumocócica 23v", dose = "2ª Dose",
            categoria = "Idoso", grupoVacina = "Pneumocócica 23v", ordemDose = 2,
            observacao = "Reforço 5 anos após 1ª dose")
        lista += Vacina(usuarioId = userId, nome = "Dupla Adulto (dT)", dose = "Reforço (idoso)",
            categoria = "Idoso", observacao = "Reforço a cada 10 anos",
            recorrente = true, mesesParaReforco = 120)

        // ── COVID-19 ──────────────────────────────────────────────────────────
        // Esquema pandêmico histórico: até 5 doses (esquema primário + reforços)
        // O usuário registra quantas doses tomou. Doses extras podem ser adicionadas.
        // Reforços anuais: grupos prioritários 1x/ano; idosos/imunocomp./gestantes 2x/ano (6 meses).
        // Fonte: Ministério da Saúde, atualização dez/2024.
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "1ª Dose",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 1,
            observacao = "Esquema primário — 1ª dose", permiteNovasDoses = false)
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "2ª Dose",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 2,
            observacao = "Esquema primário — 2ª dose", permiteNovasDoses = false)
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "3ª Dose (1º Reforço)",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 3,
            observacao = "1º reforço", permiteNovasDoses = false)
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "4ª Dose (2º Reforço)",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 4,
            observacao = "2º reforço", permiteNovasDoses = false)
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "5ª Dose (3º Reforço)",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 5,
            observacao = "3º reforço", permiteNovasDoses = false)
        // Dose anual recorrente — grupos prioritários (intervalo mínimo 3 meses da última dose)
        // Para idosos/imunocomp./gestantes/puérperas: 2 doses/ano com 6 meses de intervalo
        lista += Vacina(usuarioId = userId, nome = "COVID-19", dose = "Dose Anual",
            categoria = "Anual", grupoVacina = "COVID-19", ordemDose = 6,
            observacao = "Grupos prioritários: 1 dose/ano. Idosos/imunocomp./gestantes: 2 doses/ano (intervalo mín. 6 meses)",
            recorrente = true, mesesParaReforco = 12, permiteNovasDoses = true)

        // ── Influenza ─────────────────────────────────────────────────────────
        // Campanha nacional todo ano em abril (S/SE/CO/NE); 2º semestre na Região Norte
        lista += Vacina(usuarioId = userId, nome = "Influenza (Gripe)", dose = "Anual",
            categoria = "Anual", observacao = "Campanha nacional: abril (S/SE/CO/NE) · 2º sem. (Norte)",
            recorrente = true, mesesParaReforco = 12, mesCampanha = 4)

        repository.inserirVacinas(lista)
    }
}
