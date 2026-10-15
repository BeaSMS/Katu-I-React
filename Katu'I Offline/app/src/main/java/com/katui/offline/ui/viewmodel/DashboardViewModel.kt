package com.katui.offline.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Alarme
import com.katui.offline.data.local.entities.Consulta
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.local.entities.Sintoma
import com.katui.offline.data.local.entities.Exame
import com.katui.offline.data.local.entities.Receita
import com.katui.offline.data.local.KatuiDatabase
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.repository.AlarmeRepository
import com.katui.offline.data.repository.ConsultaRepository
import com.katui.offline.data.repository.MedicamentoRepository
import com.katui.offline.data.repository.SintomaRepository
import com.katui.offline.data.repository.ExameRepository
import com.katui.offline.data.repository.FemaleHealthRepository
import com.katui.offline.data.repository.VitalsRepository
import com.katui.offline.data.local.entities.CicloMenstrual
import com.katui.offline.data.local.entities.Anticoncepcional
import com.katui.offline.data.local.entities.Vitals
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import com.katui.offline.data.local.entities.MedicamentoCatalogo
import com.katui.offline.data.local.entities.SintomaCatalogo
import com.katui.offline.data.local.entities.EfeitoColateralCatalogo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val medicamentoRepository: MedicamentoRepository,
    private val alarmeRepository: AlarmeRepository,
    private val consultaRepository: ConsultaRepository,
    private val sintomaRepository: SintomaRepository,
    private val exameRepository: ExameRepository,
    private val femaleHealthRepository: FemaleHealthRepository,
    private val vitalsRepository: VitalsRepository,
    private val receitaRepository: com.katui.offline.data.repository.ReceitaRepository,
    private val saudeCatalogoRepository: com.katui.offline.data.repository.SaudeCatalogoRepository,
    private val usuarioDao: UsuarioDao,
    private val database: KatuiDatabase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _proximosAlarmes = MutableStateFlow<List<Alarme>>(emptyList())
    val proximosAlarmes: StateFlow<List<Alarme>> = _proximosAlarmes.asStateFlow()

    private val _proximasConsultas = MutableStateFlow<List<Consulta>>(emptyList())
    val proximasConsultas: StateFlow<List<Consulta>> = _proximasConsultas.asStateFlow()

    private val _todasConsultas = MutableStateFlow<List<Consulta>>(emptyList())

    private val _proximosCompromissos = MutableStateFlow<List<Compromisso>>(emptyList())
    val proximosCompromissos: StateFlow<List<Compromisso>> = _proximosCompromissos.asStateFlow()

    private val _consultasSemana = MutableStateFlow<List<Consulta>>(emptyList())
    val consultasSemana: StateFlow<List<Consulta>> = _consultasSemana.asStateFlow()

    private val _sintomas = MutableStateFlow<List<Sintoma>>(emptyList())
    val sintomas: StateFlow<List<Sintoma>> = _sintomas.asStateFlow()

    private val _exames = MutableStateFlow<List<Exame>>(emptyList())
    val exames: StateFlow<List<Exame>> = _exames.asStateFlow()

    private val _receitas = MutableStateFlow<List<Receita>>(emptyList())
    val receitas: StateFlow<List<Receita>> = _receitas.asStateFlow()

    private val _vitals = MutableStateFlow<List<Vitals>>(emptyList())
    val vitals: StateFlow<List<Vitals>> = _vitals.asStateFlow()

    private val _dependentes = MutableStateFlow<List<Usuario>>(emptyList())
    val dependentes: StateFlow<List<Usuario>> = _dependentes.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _todosAlarmes = MutableStateFlow<List<Alarme>>(emptyList())
    private val _todosCiclos = MutableStateFlow<List<CicloMenstrual>>(emptyList())
    private val _anticoncepcional = MutableStateFlow<Anticoncepcional?>(null)

    // Exposto para o Dashboard poder colorir o calendário com dados femininos
    val ciclosMenstruais: StateFlow<List<CicloMenstrual>> get() = _todosCiclos.asStateFlow()
    val anticoncepcionalAtivo: StateFlow<Anticoncepcional?> get() = _anticoncepcional.asStateFlow()

    init {
        Log.d("KATUI_DEBUG", "DashboardViewModel: init iniciado")
        
        sessionManager.userId
            .flatMapLatest { userId ->
                combine(
                    usuarioDao.getUsuarioPorId(userId),
                    alarmeRepository.getAlarmes(userId),
                    consultaRepository.getConsultas(userId),
                    femaleHealthRepository.getCiclos(userId),
                    femaleHealthRepository.getAnticoncepcionalAtivo(userId),
                    sintomaRepository.getSintomas(userId),
                    exameRepository.getExames(userId),
                    receitaRepository.getReceitasByUsuario(userId),
                    vitalsRepository.getVitals(userId)
                ) { values ->
                    val user = values[0] as Usuario?
                    val alarmes = values[1] as List<Alarme>
                    val consultas = values[2] as List<Consulta>
                    val ciclos = values[3] as List<CicloMenstrual>
                    val anticoncepcional = values[4] as Anticoncepcional?
                    val sintomas = values[5] as List<Sintoma>
                    val exames = values[6] as List<Exame>
                    val receitas = values[7] as List<Receita>
                    val vitals = values[8] as List<Vitals>

                    _usuario.value = user
                    _todosAlarmes.value = alarmes
                    _proximosAlarmes.value = alarmes.filter { !it.tomado && it.horario.isAfter(LocalDateTime.now()) }.take(3)
                    _todasConsultas.value = consultas
                    _proximasConsultas.value = consultas.filter { it.dataHora.isAfter(LocalDateTime.now()) }.take(3)
                    
                    val hoje = LocalDate.now()
                    val proximaSemana = hoje.plusDays(7)
                    _consultasSemana.value = consultas.filter { 
                        val data = it.dataHora.toLocalDate()
                        !data.isBefore(hoje) && !data.isAfter(proximaSemana)
                    }.sortedBy { it.dataHora }

                    _todosCiclos.value = ciclos
                    _anticoncepcional.value = anticoncepcional
                    _sintomas.value = sintomas
                    _exames.value = exames
                    _receitas.value = receitas
                    _vitals.value = vitals

                    atualizarCompromissos()
                    
                    if (user != null) {
                        viewModelScope.launch { 
                            femaleHealthRepository.triggerScheduling(user.id)
                            
                            val profiles = mutableListOf<Usuario>()
                            // Busca a "família" completa para o menu de troca
                            val rootId = user.cuidadorId ?: user.id
                            
                            // 1. Busca o Cuidador Principal
                            usuarioDao.getUsuario(rootId)?.let { profiles.add(it) }
                            
                            // 2. Busca todos os dependentes desse cuidador usando a coluna direta (correto)
                            val deps = usuarioDao.getDependentes(rootId).first()
                            profiles.addAll(deps)

                            _dependentes.value = profiles.distinctBy { it.id }
                        }
                    }
                    Unit
                }
            }
            .launchIn(viewModelScope)

        inicializarCatalogoSeNecessario()
        inicializarSintomasSeNecessario()
        inicializarSaudeCatalogoSeNecessario()
    }

    private fun inicializarSaudeCatalogoSeNecessario() {
        viewModelScope.launch {
            if (saudeCatalogoRepository.getCount() == 0) {
                Log.d("KATUI_DEBUG", "Populando catálogo de alergias e condições...")
                val items = mutableListOf<SaudeItemCatalogo>()
                
                // Alergias
                val alergiasComuns = listOf(
                    "Dipirona" to "Medicamentos",
                    "Paracetamol" to "Medicamentos",
                    "Penicilina" to "Medicamentos",
                    "AAS (Aspirina)" to "Medicamentos",
                    "Anti-inflamatórios (AINEs)" to "Medicamentos",
                    "Iodo" to "Contraste/Outros",
                    "Látex" to "Materiais",
                    "Amendoim" to "Alimentos",
                    "Frutos do Mar" to "Alimentos",
                    "Leite (Lactose)" to "Alimentos",
                    "Glúten" to "Alimentos",
                    "Ovo" to "Alimentos",
                    "Poeira/Ácaro" to "Ambiental",
                    "Pólen" to "Ambiental",
                    "Picada de Abelha" to "Insetos"
                )
                
                alergiasComuns.forEach { (nome, cat) ->
                    items.add(SaudeItemCatalogo(nome = nome, categoria = cat, tipo = TipoSaudeItem.ALERGIA))
                }
                
                // Condições Crônicas (DCNT)
                val condicoes = listOf(
                    "Hipertensão (Pressão Alta)" to "Cardiovascular",
                    "Diabetes Tipo 1" to "Endócrino",
                    "Diabetes Tipo 2" to "Endócrino",
                    "Colesterol Alto (Dislipidemia)" to "Metabólico",
                    "Asma" to "Respiratório",
                    "Bronquite Crônica / DPOC" to "Respiratório",
                    "Hipotireoidismo" to "Endócrino",
                    "Hipertireoidismo" to "Endócrino",
                    "Artrite / Artrose" to "Musculoesquelético",
                    "Osteoporose" to "Musculoesquelético",
                    "Insuficiência Renal" to "Renal",
                    "Gastrite / Refluxo" to "Digestivo",
                    "Depressão" to "Saúde Mental",
                    "Ansiedade" to "Saúde Mental",
                    "Alzheimer / Demência" to "Neurológico",
                    "Parkinson" to "Neurológico",
                    "Epilepsia" to "Neurológico",
                    "Glaucoma" to "Visão"
                )
                
                condicoes.forEach { (nome, cat) ->
                    items.add(SaudeItemCatalogo(nome = nome, categoria = cat, tipo = TipoSaudeItem.CONDICAO))
                }
                
                saudeCatalogoRepository.insertAll(items)
            }
        }
    }

    private fun inicializarSintomasSeNecessario() {
        viewModelScope.launch {
            if (sintomaRepository.getCatalogoCount() == 0) {
                Log.d("KATUI_DEBUG", "Populando catálogo de sintomas e mapeamento de efeitos...")
                
                // --- Sintomas Comuns ---
                val sintomas = listOf(
                    SintomaCatalogo(id = 1, nome = "Náusea", categoria = "Digestivo", popular = true),
                    SintomaCatalogo(id = 2, nome = "Dor de Cabeça", categoria = "Neurológico", popular = true),
                    SintomaCatalogo(id = 3, nome = "Tosse Seca", categoria = "Respiratório", popular = true),
                    SintomaCatalogo(id = 4, nome = "Tontura", categoria = "Neurológico", popular = true),
                    SintomaCatalogo(id = 5, nome = "Diarreia", categoria = "Digestivo", popular = true),
                    SintomaCatalogo(id = 6, nome = "Sonolência", categoria = "Neurológico", popular = true),
                    SintomaCatalogo(id = 7, nome = "Insônia", categoria = "Neurológico", popular = true),
                    SintomaCatalogo(id = 8, nome = "Cólica Abdominal", categoria = "Digestivo", popular = true),
                    SintomaCatalogo(id = 9, nome = "Boca Seca", categoria = "Outros", popular = false),
                    SintomaCatalogo(id = 10, nome = "Inchaço (Edema)", categoria = "Cardiovascular", popular = false),
                    SintomaCatalogo(id = 11, nome = "Palpitação", categoria = "Cardiovascular", popular = false),
                    SintomaCatalogo(id = 12, nome = "Cansaço Excessivo", categoria = "Geral", popular = true),
                    SintomaCatalogo(id = 13, nome = "Visão Turva", categoria = "Neurológico", popular = false),
                    SintomaCatalogo(id = 14, nome = "Erupção Cutânea", categoria = "Pele", popular = false),
                    SintomaCatalogo(id = 15, nome = "Dor Muscular", categoria = "Musculoesquelético", popular = true),
                    // --- Novos sintomas, necessários para cobrir o mapeamento expandido ---
                    SintomaCatalogo(id = 16, nome = "Constipação", categoria = "Digestivo", popular = true),
                    SintomaCatalogo(id = 17, nome = "Hipotensão Postural", categoria = "Cardiovascular", popular = false),
                    SintomaCatalogo(id = 18, nome = "Sangramento ou Hematoma Fácil", categoria = "Outros", popular = false),
                    SintomaCatalogo(id = 19, nome = "Disfunção Sexual", categoria = "Outros", popular = false),
                    SintomaCatalogo(id = 20, nome = "Ganho de Peso", categoria = "Geral", popular = false),
                    SintomaCatalogo(id = 21, nome = "Fotossensibilidade", categoria = "Pele", popular = false),
                    SintomaCatalogo(id = 22, nome = "Falta de Ar", categoria = "Respiratório", popular = false),
                    SintomaCatalogo(id = 23, nome = "Aumento do Apetite", categoria = "Geral", popular = false),
                    SintomaCatalogo(id = 24, nome = "Perda de Apetite", categoria = "Geral", popular = false),
                    SintomaCatalogo(id = 25, nome = "Azia ou Queimação", categoria = "Digestivo", popular = true),
                    SintomaCatalogo(id = 26, nome = "Tremores", categoria = "Neurológico", popular = false),
                    SintomaCatalogo(id = 27, nome = "Suor Excessivo", categoria = "Outros", popular = false),
                    SintomaCatalogo(id = 28, nome = "Zumbido no Ouvido", categoria = "Neurológico", popular = false),
                    SintomaCatalogo(id = 29, nome = "Confusão Mental", categoria = "Neurológico", popular = false),
                    SintomaCatalogo(id = 30, nome = "Câibras", categoria = "Musculoesquelético", popular = false)
                )
                sintomaRepository.insertCatalogoItems(sintomas)

                // --- Mapeamento de Efeitos Colaterais por Medicamento ---
                // IDs do MedicamentoCatalogo agora são explícitos (definidos em inicializarCatalogoSeNecessario),
                // entao essas referencias sao estaveis e nao dependem mais da ordem de insercao.
                // Mapeamento construido por classe terapeutica / principio ativo, restrito aos medicamentos
                // para os quais ha confianca clinica razoavel (efeitos colaterais bem documentados em bula).
                // Sintomas: 1=Nausea 2=DorCabeca 3=TosseSeca 4=Tontura 5=Diarreia 6=Sonolencia 7=Insonia
                // 8=ColicaAbdominal 9=BocaSeca 10=Inchaco 11=Palpitacao 12=Cansaco 13=VisaoTurva 14=Erupcao
                // 15=DorMuscular 16=Constipacao 17=HipotensaoPostural 18=Sangramento 19=DisfuncaoSexual
                // 20=GanhoPeso 21=Fotossensibilidade 22=FaltaDeAr 23=AumentoApetite 24=PerdaApetite
                // 25=Azia 26=Tremores 27=SuorExcessivo 28=Zumbido 29=ConfusaoMental 30=Caibras
                val efeitos = mutableListOf<EfeitoColateralCatalogo>()

                fun add(medicamentoId: Long, sintomaId: Long, frequencia: String = "Comum") {
                    efeitos.add(EfeitoColateralCatalogo(medicamentoId, sintomaId, frequencia))
                }

                // --- Analgesicos / Antitermicos ---
                add(1, 14, "Rara")        // Dipirona -> Erupcao Cutanea
                add(2, 25, "Rara")        // Paracetamol -> Azia (raro, em doses altas)
                add(5, 6, "Comum")        // Dorflex -> Sonolencia (orfenadrina)
                add(5, 9, "Comum")        // Dorflex -> Boca Seca
                add(9, 8, "Comum")        // Ibuprofeno -> Colica Abdominal
                add(9, 25, "Comum")       // Ibuprofeno -> Azia
                add(15, 8, "Comum")       // Nimesulida -> Colica Abdominal
                add(15, 25, "Comum")      // Nimesulida -> Azia

                // --- Anti-hipertensivos / Cardiovasculares ---
                add(3, 2, "Comum")        // Losartana -> Dor de Cabeca
                add(3, 4, "Comum")        // Losartana -> Tontura
                add(11, 6, "Comum")       // Atenolol -> Sonolencia
                add(11, 17, "Comum")      // Atenolol -> Hipotensao Postural
                add(21, 3, "Muito Comum") // Enalapril -> Tosse Seca (efeito classico de IECA)
                add(21, 4, "Comum")       // Enalapril -> Tontura
                add(22, 4, "Comum")       // Furosemida -> Tontura (deplecao volemica)
                add(22, 11, "Comum")      // Furosemida -> Palpitacao
                add(22, 17, "Comum")      // Furosemida -> Hipotensao Postural
                add(23, 4, "Comum")       // Hidroclorotiazida -> Tontura
                add(23, 17, "Comum")      // Hidroclorotiazida -> Hipotensao Postural
                add(24, 6, "Comum")       // Metoprolol -> Sonolencia
                add(24, 17, "Comum")      // Metoprolol -> Hipotensao Postural
                add(25, 10, "Muito Comum") // Anlodipino -> Inchaco (Edema), efeito classico
                add(25, 2, "Comum")       // Anlodipino -> Dor de Cabeca
                add(27, 18, "Comum")      // Aspirina Prevent -> Sangramento/Hematoma Facil
                add(27, 8, "Comum")       // Aspirina Prevent -> Colica Abdominal
                add(28, 18, "Muito Comum") // Xarelto -> Sangramento/Hematoma Facil
                add(29, 18, "Muito Comum") // Eliquis -> Sangramento/Hematoma Facil
                add(30, 18, "Muito Comum") // Marevan (Varfarina) -> Sangramento/Hematoma Facil

                // --- Estatinas / Colesterol ---
                add(8, 15, "Comum")       // Simvastatina -> Dor Muscular
                add(20, 15, "Comum")      // Atorvastatina -> Dor Muscular
                add(26, 15, "Comum")      // Rosuvastatina -> Dor Muscular

                // --- Antidiabeticos ---
                add(4, 1, "Muito Comum")  // Metformina -> Nausea
                add(4, 5, "Muito Comum")  // Metformina -> Diarreia
                add(4, 8, "Comum")        // Metformina -> Colica Abdominal
                add(6, 1, "Comum")        // Ozempic (Semaglutida) -> Nausea
                add(6, 5, "Comum")        // Ozempic -> Diarreia
                add(6, 24, "Comum")       // Ozempic -> Perda de Apetite
                add(19, 1, "Comum")       // Forxiga (Dapagliflozina) -> Nausea
                add(31, 1, "Comum")       // Jardiance (Empagliflozina) -> Nausea
                add(34, 8, "Comum")       // Diamicron (Gliclazida) -> Colica Abdominal
                add(34, 23, "Comum")      // Diamicron -> Aumento do Apetite
                add(35, 23, "Comum")      // Glibenclamida -> Aumento do Apetite
                add(35, 20, "Comum")      // Glibenclamida -> Ganho de Peso

                // --- Saude Mental / Neurologico ---
                add(12, 6, "Comum")       // Rivotril (Clonazepam) -> Sonolencia
                add(12, 29, "Comum")      // Rivotril -> Confusao Mental
                add(17, 7, "Comum")       // Sertralina -> Insonia
                add(17, 1, "Comum")       // Sertralina -> Nausea
                add(17, 19, "Comum")      // Sertralina -> Disfuncao Sexual
                add(39, 7, "Comum")       // Fluoxetina (id=39) -> Insonia
                add(39, 1, "Comum")       // Fluoxetina -> Nausea
                add(39, 6, "Comum")       // Fluoxetina -> Sonolencia
                add(39, 19, "Comum")      // Fluoxetina -> Disfuncao Sexual
                add(40, 1, "Comum")       // Escitalopram (id=40) -> Nausea
                add(40, 7, "Comum")       // Escitalopram -> Insonia
                add(40, 19, "Comum")      // Escitalopram -> Disfuncao Sexual
                add(41, 9, "Comum")       // Desvenlafaxina (id=41) -> Boca Seca
                add(41, 27, "Comum")      // Desvenlafaxina -> Suor Excessivo
                add(42, 9, "Muito Comum") // Amitriptilina (id=42) -> Boca Seca
                add(42, 6, "Muito Comum") // Amitriptilina -> Sonolencia
                add(42, 20, "Comum")      // Amitriptilina -> Ganho de Peso
                add(42, 16, "Comum")      // Amitriptilina -> Constipacao
                add(43, 6, "Muito Comum") // Quetiapina (id=43) -> Sonolencia
                add(43, 20, "Comum")      // Quetiapina -> Ganho de Peso
                add(44, 6, "Comum")       // Risperidona (id=44) -> Sonolencia
                add(44, 20, "Comum")      // Risperidona -> Ganho de Peso
                add(46, 6, "Comum")       // Carbamazepina (id=46) -> Sonolencia
                add(46, 4, "Comum")       // Carbamazepina -> Tontura
                add(46, 13, "Comum")      // Carbamazepina -> Visao Turva

                // --- Antibioticos ---
                add(14, 1, "Comum")       // Amoxicilina -> Nausea
                add(14, 5, "Comum")       // Amoxicilina -> Diarreia
                add(14, 14, "Rara")       // Amoxicilina -> Erupcao Cutanea
                add(48, 1, "Comum")       // Azitromicina (id=48) -> Nausea
                add(48, 5, "Comum")       // Azitromicina -> Diarreia
                add(48, 8, "Comum")       // Azitromicina -> Colica Abdominal
                add(49, 1, "Comum")       // Ciprofloxacino (id=49) -> Nausea
                add(49, 5, "Comum")       // Ciprofloxacino -> Diarreia
                add(50, 1, "Comum")       // Cefalexina (id=50) -> Nausea
                add(50, 5, "Comum")       // Cefalexina -> Diarreia

                // --- Anti-inflamatorios / Corticoides ---
                add(51, 8, "Comum")       // Diclofenaco (id=51) -> Colica Abdominal
                add(51, 25, "Comum")      // Diclofenaco -> Azia
                add(52, 8, "Comum")       // Meloxicam (id=52) -> Colica Abdominal
                add(53, 23, "Comum")      // Prednisona (id=53) -> Aumento do Apetite
                add(53, 7, "Comum")       // Prednisona -> Insonia
                add(53, 20, "Comum")      // Prednisona -> Ganho de Peso
                add(54, 23, "Comum")      // Dexametasona (id=54) -> Aumento do Apetite
                add(54, 7, "Comum")       // Dexametasona -> Insonia

                // --- Gastro ---
                add(10, 2, "Comum")       // Omeprazol -> Dor de Cabeca
                add(10, 8, "Comum")       // Omeprazol -> Colica Abdominal
                add(55, 2, "Comum")       // Pantoprazol (id=55) -> Dor de Cabeca
                add(55, 5, "Comum")       // Pantoprazol -> Diarreia

                // --- Antialergicos ---
                add(59, 6, "Comum")       // Loratadina (id=59) -> Sonolencia (leve)
                add(59, 9, "Comum")       // Loratadina -> Boca Seca
                add(60, 6, "Muito Comum") // Polaramine (id=60) -> Sonolencia
                add(60, 9, "Comum")       // Polaramine -> Boca Seca

                // --- Hormonio Tireoidiano ---
                add(7, 11, "Comum")       // Puran T4 -> Palpitacao (em caso de dose excessiva)
                add(7, 7, "Comum")        // Puran T4 -> Insonia
                add(7, 26, "Comum")       // Puran T4 -> Tremores

                // --- Disfuncao eretil ---
                add(16, 2, "Comum")       // Tadalafila -> Dor de Cabeca
                add(16, 4, "Comum")       // Tadalafila -> Tontura

                sintomaRepository.insertEfeitoMapeamento(efeitos)
            }
        }
    }

    private fun inicializarCatalogoSeNecessario() {
        viewModelScope.launch {
            if (medicamentoRepository.getCatalogoCount() == 0) {
                Log.d("KATUI_DEBUG", "Populando catálogo inicial expandido de medicamentos...")
                val starterKit = mutableListOf<MedicamentoCatalogo>()
                
                // --- Top 1-50 (Líderes de Venda e Genéricos Massivos) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 1, nome = "Dipirona Monoidratada", principioAtivo = "Dipirona", laboratorio = "EMS", registroMS = "102350434", classeTerapeutica = "Analgésico e Antitérmico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 2, nome = "Paracetamol", principioAtivo = "Paracetamol", laboratorio = "Medley", registroMS = "101810332", classeTerapeutica = "Analgésico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 3, nome = "Losartana Potássica", principioAtivo = "Losartana", laboratorio = "Eurofarma", registroMS = "100430932", classeTerapeutica = "Anti-hipertensivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 4, nome = "Metformina (Glifage XR)", principioAtivo = "Cloridrato de Metformina", laboratorio = "Merck", registroMS = "100890250", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 5, nome = "Dorflex", principioAtivo = "Dipirona + Citrato de Orfenadrina + Cafeína", laboratorio = "Sanofi", registroMS = "113000183", classeTerapeutica = "Analgésico/Relaxante Muscular", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 6, nome = "Ozempic", principioAtivo = "Semaglutida", laboratorio = "Novo Nordisk", registroMS = "117660036", classeTerapeutica = "Antidiabético / Obesidade", formaFarmaceutica = "Solução Injetável"),
                    MedicamentoCatalogo(id = 7, nome = "Puran T4", principioAtivo = "Levotiroxina Sódica", laboratorio = "Sanofi", registroMS = "113000181", classeTerapeutica = "Hormônio Tireoidiano", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 8, nome = "Simvastatina", principioAtivo = "Simvastatina", laboratorio = "Sanofi", registroMS = "113001051", classeTerapeutica = "Redutor de Colesterol", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 9, nome = "Ibuprofeno (Advil)", principioAtivo = "Ibuprofeno", laboratorio = "GSK", registroMS = "101430048", classeTerapeutica = "Anti-inflamatório", formaFarmaceutica = "Cápsula"),
                    MedicamentoCatalogo(id = 10, nome = "Omeprazol", principioAtivo = "Omeprazol", laboratorio = "Neo Química", registroMS = "155840333", classeTerapeutica = "Protetor Gástrico", formaFarmaceutica = "Cápsula"),
                    MedicamentoCatalogo(id = 11, nome = "Atenolol", principioAtivo = "Atenolol", laboratorio = "EMS", registroMS = "102350481", classeTerapeutica = "Anti-hipertensivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 12, nome = "Rivotril", principioAtivo = "Clonazepam", laboratorio = "Roche", registroMS = "101000072", classeTerapeutica = "Ansiolítico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 13, nome = "Venvanse", principioAtivo = "Dimesilato de Lisdexanfetamina", laboratorio = "Takeda", registroMS = "106390264", classeTerapeutica = "TDAH", formaFarmaceutica = "Cápsula"),
                    MedicamentoCatalogo(id = 14, nome = "Amoxicilina", principioAtivo = "Amoxicilina", laboratorio = "Aché", registroMS = "105730311", classeTerapeutica = "Antibiótico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 15, nome = "Nimesulida", principioAtivo = "Nimesulida", laboratorio = "Cimed", registroMS = "143810105", classeTerapeutica = "Anti-inflamatório", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 16, nome = "Tadalafila", principioAtivo = "Tadalafila", laboratorio = "Eurofarma", registroMS = "100431057", classeTerapeutica = "Disfunção Erétil", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 17, nome = "Sertralina (Zoloft)", principioAtivo = "Cloridrato de Sertralina", laboratorio = "Viatris", registroMS = "100100150", classeTerapeutica = "Antidepressivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 18, nome = "Buscopan Composto", principioAtivo = "Butilbrometo de Escopolamina + Dipirona", laboratorio = "Boehringer", registroMS = "103670004", classeTerapeutica = "Antiespasmódico", formaFarmaceutica = "Drágea"),
                    MedicamentoCatalogo(id = 19, nome = "Forxiga", principioAtivo = "Dapagliflozina", laboratorio = "AstraZeneca", registroMS = "116180255", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 20, nome = "Atorvastatina", principioAtivo = "Atorvastatina Cálcica", laboratorio = "Pfizer", registroMS = "102160147", classeTerapeutica = "Redutor de Colesterol", formaFarmaceutica = "Comprimido")
                ))

                // --- Top 21-30 (Saúde Crônica e Cardiovascular) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 21, nome = "Enaprol (Enalapril)", principioAtivo = "Maleato de Enalapril", laboratorio = "Sandoz", registroMS = "100470375", classeTerapeutica = "Anti-hipertensivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 22, nome = "Furosemida (Lasix)", principioAtivo = "Furosemida", laboratorio = "Sanofi", registroMS = "113000213", classeTerapeutica = "Diurético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 23, nome = "Hidroclorotiazida", principioAtivo = "Hidroclorotiazida", laboratorio = "Teuto", registroMS = "103700057", classeTerapeutica = "Diurético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 24, nome = "Selozok (Metoprolol)", principioAtivo = "Succinato de Metoprolol", laboratorio = "AstraZeneca", registroMS = "116180227", classeTerapeutica = "Betabloqueador", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 25, nome = "Anlodipino", principioAtivo = "Besilato de Anlodipino", laboratorio = "Medley", registroMS = "101810452", classeTerapeutica = "Anti-hipertensivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 26, nome = "Rosuvastatina (Crestor)", principioAtivo = "Rosuvastatina Cálcica", laboratorio = "AstraZeneca", registroMS = "116180242", classeTerapeutica = "Hipolipemiante", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 27, nome = "Aspirina Prevent", principioAtivo = "Ácido Acetilsalicílico", laboratorio = "Bayer", registroMS = "170560014", classeTerapeutica = "Antiagregante Plaquetário", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 28, nome = "Xarelto", principioAtivo = "Rivaroxabana", laboratorio = "Bayer", registroMS = "170560048", classeTerapeutica = "Anticoagulante", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 29, nome = "Eliquis", principioAtivo = "Apixabana", laboratorio = "Pfizer", registroMS = "102160241", classeTerapeutica = "Anticoagulante", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 30, nome = "Marevan", principioAtivo = "Varfarina Sódica", laboratorio = "Takeda", registroMS = "106390141", classeTerapeutica = "Anticoagulante", formaFarmaceutica = "Comprimido")
                ))

                // --- Top 31-38 (Diabetes e Metabolismo) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 31, nome = "Jardiance", principioAtivo = "Empagliflozina", laboratorio = "Boehringer", registroMS = "103670172", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 32, nome = "Trayenta", principioAtivo = "Linagliptina", laboratorio = "Boehringer", registroMS = "103670165", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 33, nome = "Galvus", principioAtivo = "Vildagliptina", laboratorio = "Novartis", registroMS = "100681084", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 34, nome = "Diamicron MR", principioAtivo = "Gliclazida", laboratorio = "Servier", registroMS = "112780003", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 35, nome = "Glibenclamida", principioAtivo = "Glibenclamida", laboratorio = "Prati-Donaduzzi", registroMS = "125680076", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 36, nome = "Lantus", principioAtivo = "Insulina Glargina", laboratorio = "Sanofi", registroMS = "113000271", classeTerapeutica = "Insulina", formaFarmaceutica = "Solução Injetável"),
                    MedicamentoCatalogo(id = 37, nome = "Novorapid", principioAtivo = "Insulina Asparte", laboratorio = "Novo Nordisk", registroMS = "117660015", classeTerapeutica = "Insulina", formaFarmaceutica = "Solução Injetável"),
                    MedicamentoCatalogo(id = 38, nome = "Januvia", principioAtivo = "Fosfato de Sitagliptina", laboratorio = "MSD", registroMS = "100290184", classeTerapeutica = "Antidiabético", formaFarmaceutica = "Comprimido")
                ))

                // --- Top 39-47 (Saúde Mental e Neurológica) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 39, nome = "Fluoxetina (Daforin)", principioAtivo = "Cloridrato de Fluoxetina", laboratorio = "Neo Química", registroMS = "155840251", classeTerapeutica = "Antidepressivo", formaFarmaceutica = "Cápsula"),
                    MedicamentoCatalogo(id = 40, nome = "Escitalopram (Lexapro)", principioAtivo = "Oxalato de Escitalopram", laboratorio = "Lundbeck", registroMS = "104750044", classeTerapeutica = "Antidepressivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 41, nome = "Desvenlafaxina (Pristiq)", principioAtivo = "Succinato de Desvenlafaxina Monoidratado", laboratorio = "Pfizer", registroMS = "102160228", classeTerapeutica = "Antidepressivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 42, nome = "Amitriptilina", principioAtivo = "Cloridrato de Amitriptilina", laboratorio = "EMS", registroMS = "102350646", classeTerapeutica = "Antidepressivo", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 43, nome = "Quetiapina", principioAtivo = "Hemifumarato de Quetiapina", laboratorio = "Eurofarma", registroMS = "100431011", classeTerapeutica = "Antipsicótico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 44, nome = "Risperidona", principioAtivo = "Risperidona", laboratorio = "Legrand", registroMS = "105830606", classeTerapeutica = "Antipsicótico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 45, nome = "Valproato de Sódio (Depakene)", principioAtivo = "Valproato de Sódio", laboratorio = "Abbott", registroMS = "105530203", classeTerapeutica = "Anticonvulsivante", formaFarmaceutica = "Xarope"),
                    MedicamentoCatalogo(id = 46, nome = "Carbamazepina (Tegretol)", principioAtivo = "Carbamazepina", laboratorio = "Novartis", registroMS = "100680088", classeTerapeutica = "Anticonvulsivante", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 47, nome = "Fenobarbital (Gardenal)", principioAtivo = "Fenobarbital", laboratorio = "Sanofi", registroMS = "113000204", classeTerapeutica = "Anticonvulsivante", formaFarmaceutica = "Comprimido")
                ))

                // --- Top 48-54 (Antibióticos e Anti-inflamatórios) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 48, nome = "Azitromicina", principioAtivo = "Azitromicina", laboratorio = "Germed", registroMS = "105830501", classeTerapeutica = "Antibiótico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 49, nome = "Ciprofloxacino", principioAtivo = "Cloridrato de Ciprofloxacino", laboratorio = "Bayer", registroMS = "170560032", classeTerapeutica = "Antibiótico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 50, nome = "Cefalexina (Keflex)", principioAtivo = "Cefalexina", laboratorio = "Takeda", registroMS = "106390117", classeTerapeutica = "Antibiótico", formaFarmaceutica = "Drágea"),
                    MedicamentoCatalogo(id = 51, nome = "Diclofenaco (Voltaren)", principioAtivo = "Diclofenaco Sódico", laboratorio = "Novartis", registroMS = "100680062", classeTerapeutica = "Anti-inflamatório", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 52, nome = "Meloxicam", principioAtivo = "Meloxicam", laboratorio = "Medley", registroMS = "101810488", classeTerapeutica = "Anti-inflamatório", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 53, nome = "Prednisona (Meticorten)", principioAtivo = "Prednisona", laboratorio = "Mantorp", registroMS = "100930018", classeTerapeutica = "Corticoide", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 54, nome = "Dexametasona (Decadron)", principioAtivo = "Dexametasona", laboratorio = "Aché", registroMS = "105730312", classeTerapeutica = "Corticoide", formaFarmaceutica = "Elixir")
                ))

                // --- Top 55-64 (Gastro e Diversos) ---
                starterKit.addAll(listOf(
                    MedicamentoCatalogo(id = 55, nome = "Pantoprazol", principioAtivo = "Pantoprazol Magnésico", laboratorio = "Takeda", registroMS = "106390216", classeTerapeutica = "Protetor Gástrico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 56, nome = "Domperidona (Motilium)", principioAtivo = "Domperidona", laboratorio = "Janssen", registroMS = "112360014", classeTerapeutica = "Antiemético", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 57, nome = "Luftal (Simeticona)", principioAtivo = "Simeticona", laboratorio = "Reckitt", registroMS = "100300001", classeTerapeutica = "Antiflatulento", formaFarmaceutica = "Gotas"),
                    MedicamentoCatalogo(id = 58, nome = "Allegra (Fexofenadina)", principioAtivo = "Cloridrato de Fexofenadina", laboratorio = "Sanofi", registroMS = "113000258", classeTerapeutica = "Antialérgico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 59, nome = "Loratadina", principioAtivo = "Loratadina", laboratorio = "Neo Química", registroMS = "155840331", classeTerapeutica = "Antialérgico", formaFarmaceutica = "Xarope"),
                    MedicamentoCatalogo(id = 60, nome = "Polaramine", principioAtivo = "Maleato de Dexclorfeniramina", laboratorio = "Mantorp", registroMS = "100930011", classeTerapeutica = "Antialérgico", formaFarmaceutica = "Drágea"),
                    MedicamentoCatalogo(id = 61, nome = "Aerolin (Salbutamol)", principioAtivo = "Sulfato de Salbutamol", laboratorio = "GSK", registroMS = "101070220", classeTerapeutica = "Broncodilatador", formaFarmaceutica = "Spray Aerosol"),
                    MedicamentoCatalogo(id = 62, nome = "Vick Pyrena", principioAtivo = "Paracetamol", laboratorio = "P&G", registroMS = "121420001", classeTerapeutica = "Analgésico", formaFarmaceutica = "Pó para solução"),
                    MedicamentoCatalogo(id = 63, nome = "Engov", principioAtivo = "Maleato de Mepiramina + Hidróxido de Alumínio + AAS + Cafeína", laboratorio = "Hypera", registroMS = "100930005", classeTerapeutica = "Antácido/Analgésico", formaFarmaceutica = "Comprimido"),
                    MedicamentoCatalogo(id = 64, nome = "Eno", principioAtivo = "Bicarbonato de Sódio + Carbonato de Sódio + Ácido Cítrico", laboratorio = "GSK", registroMS = "101070104", classeTerapeutica = "Antácido", formaFarmaceutica = "Pó Efervescente")
                ))

                // Adicionando mais 50 genéricos variados para chegar perto dos 300
                val genericos = listOf(
                    "Alopurinol", "Ambroxol", "Amiodarona", "Baclofeno", "Beclometasona", "Benzilpenicilina", "Betametasona",
                    "Bisoprolol", "Bromoprida", "Captopril", "Carvedilol", "Cinarizina", "Clortalidona", "Clotrimazol",
                    "Desonida", "Digoxina", "Diltiazem", "Doxazosina", "Espirolactona", "Etilefrina", "Fenoximetilpenicilina",
                    "Finasterida", "Fluconazol", "Gliclazida", "Glimepirida", "Guaifenesina", "Haloperidol", "Hidantal",
                    "Hidrocortisona", "Indapamida", "Ipatrópio", "Isossorbida", "Itraconazol", "Ketoconazol", "Levodopa",
                    "Levofloxacino", "Lidocaína", "Loperamida", "Mebendazol", "Metildopa", "Metoclopramida", "Metronidazol",
                    "Mupirocina", "Naproxeno", "Nifedipino", "Nitrofurantoína", "Nistatina", "Norfloxacino", "Oxibutinina",
                    "Pantoprazol", "Piroxicam", "Prednisolona", "Prometazina", "Propafenona", "Propranolol", "Ranitidina",
                    "Sinvastatina", "Spironolactona", "Sulfametoxazol", "Sulfato Ferroso", "Terazosina", "Terbutalina",
                    "Tiamina", "Timolol", "Tizanidina", "Tramadol", "Trazodona", "Triancinolona", "Verapamil", "Varfarina"
                )
                
                genericos.forEach { nomeG ->
                    starterKit.add(MedicamentoCatalogo(nome = nomeG, principioAtivo = nomeG, laboratorio = "Genérico", registroMS = null, classeTerapeutica = "Variada", formaFarmaceutica = "Comprimido"))
                }

                medicamentoRepository.insertCatalogoItems(starterKit)
            }
        }
    }

    fun switchProfile(userId: Long) {
        viewModelScope.launch {
            sessionManager.saveUserId(userId)
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun getSintomasForDate(date: LocalDate) = _sintomas.value.filter { it.dataHoraRegistro.toLocalDate() == date }
    fun getAlarmesForDate(date: LocalDate) = _todosAlarmes.value.filter { it.horario.toLocalDate() == date }.sortedBy { it.horario }
    fun getConsultasForDate(date: LocalDate) = _todasConsultas.value.filter { it.dataHora.toLocalDate() == date }
    fun getReceitasForDate(date: LocalDate) = _receitas.value.filter { it.dataRegistro.toLocalDate() == date }
    fun getExamesForDate(date: LocalDate) = _exames.value.filter { 
        it.dataRegistro.toLocalDate() == date
    }

    fun getVitalsForDate(date: LocalDate) = _vitals.value.filter { it.dataHora.toLocalDate() == date }

    fun isDiaMenstruacao(date: LocalDate): Boolean =
        _todosCiclos.value.any { ciclo ->
            val fim = ciclo.dataFim ?: ciclo.dataInicio
            !date.isBefore(ciclo.dataInicio) && !date.isAfter(fim)
        }

    private fun calcularMediaCiclo(): Long {
        val ciclos = _todosCiclos.value
        return if (ciclos.size < 2) 28L else {
            val intervalos = (0 until ciclos.size - 1).map {
                ChronoUnit.DAYS.between(ciclos[it + 1].dataInicio, ciclos[it].dataInicio)
            }
            intervalos.average().toLong()
        }
    }

    private fun calcularMediaDuracao(): Long {
        val duracoes = _todosCiclos.value.filter { it.dataFim != null }.map {
            ChronoUnit.DAYS.between(it.dataInicio, it.dataFim) + 1
        }
        return if (duracoes.isEmpty()) 5L else duracoes.average().toLong()
    }

    fun isDiaPrevisaoMenstruacao(date: LocalDate): Boolean {
        val ciclos = _todosCiclos.value
        if (ciclos.isEmpty()) return false
        val mediaDuracao = calcularMediaDuracao()
        val mediaCiclo = calcularMediaCiclo()

        val emCicloAberto = ciclos.firstOrNull { it.dataFim == null }?.let { ciclo ->
            val fimEstimado = ciclo.dataInicio.plusDays(mediaDuracao - 1)
            date.isAfter(ciclo.dataInicio) && !date.isAfter(fimEstimado)
        } ?: false

        val proximaData = ciclos.first().dataInicio.plusDays(mediaCiclo)
        val fimPrevisao = proximaData.plusDays(mediaDuracao - 1)
        val emPrevisaoProximoPeriodo = !date.isBefore(proximaData) && !date.isAfter(fimPrevisao)

        return emCicloAberto || emPrevisaoProximoPeriodo
    }

    fun isDiaFertil(date: LocalDate): Boolean {
        val ciclos = _todosCiclos.value
        if (ciclos.isEmpty()) return false
        val mediaCiclo = calcularMediaCiclo()
        return ciclos.any { ciclo ->
            val ovulacao = ciclo.dataInicio.plusDays(mediaCiclo - 14)
            val inicioFertil = ovulacao.minusDays(5)
            val fimFertil = ovulacao.plusDays(1)
            !date.isBefore(inicioFertil) && !date.isAfter(fimFertil)
        }
    }

    fun isDiaAnticoncepcional(date: LocalDate): Boolean = _anticoncepcional.value != null

    fun getDayIntensity(date: LocalDate): Int {
        val sintomasDoDia = getSintomasForDate(date)
        return if (sintomasDoDia.isEmpty()) 0 else sintomasDoDia.maxOf { it.intensidadeEscala ?: 0 }
    }

    private fun atualizarCompromissos() {
        val hoje = LocalDate.now()
        val amanha = hoje.plusDays(1)
        val compromissos = mutableListOf<Compromisso>()

        _todosAlarmes.value.filter { 
            val data = it.horario.toLocalDate()
            // Mostra se for hoje ou amanhã, independentemente de já ter passado (para manter o histórico do dia no Dashboard)
            (data == hoje || data == amanha)
        }.forEach {
            compromissos.add(Compromisso(it.horario, "Medicamento: ${it.titulo}", TipoCompromisso.MEDICAMENTO, alarmeReferencia = it))
        }

        _todasConsultas.value.filter {
            val data = it.dataHora.toLocalDate()
            data == hoje || data == amanha
        }.forEach {
            compromissos.add(Compromisso(it.dataHora, it.medico, TipoCompromisso.CONSULTA, extraInfo = it.especialidade ?: "Geral"))
        }

        val ciclos = _todosCiclos.value
        if (ciclos.isNotEmpty()) {
            val mediaCiclo = calcularMediaCiclo()
            val proximaData = ciclos.first().dataInicio.plusDays(mediaCiclo)
            if (proximaData == hoje || proximaData == amanha) {
                compromissos.add(Compromisso(proximaData.atTime(LocalTime.MIDNIGHT), "Previsão de Menstruação", TipoCompromisso.SAUDE_FEMININA))
            }
        }

        _anticoncepcional.value?.let { anti ->
            val horario = try { LocalTime.parse(anti.horarioTomada) } catch (e: Exception) { LocalTime.of(8, 0) }
            compromissos.add(Compromisso(hoje.atTime(horario), "Tomar ${anti.nome}", TipoCompromisso.SAUDE_FEMININA))
            compromissos.add(Compromisso(amanha.atTime(horario), "Tomar ${anti.nome}", TipoCompromisso.SAUDE_FEMININA))
        }

        _proximosCompromissos.value = compromissos.filter { it.dataHora.isAfter(LocalDateTime.now()) }.sortedBy { it.dataHora }
    }
}

data class Compromisso(
    val dataHora: LocalDateTime,
    val descricao: String,
    val tipo: TipoCompromisso,
    val alarmeReferencia: Alarme? = null,
    val extraInfo: String? = null
)

enum class TipoCompromisso {
    MEDICAMENTO, CONSULTA, SAUDE_FEMININA
}
