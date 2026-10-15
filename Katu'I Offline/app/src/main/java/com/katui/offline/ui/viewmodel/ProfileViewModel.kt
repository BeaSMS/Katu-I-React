package com.katui.offline.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.katui.offline.data.SecureStorage
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.KatuiDatabase
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import com.katui.offline.util.BackupManager
import com.katui.offline.util.DependentDataSummary
import com.katui.offline.util.DependentDeletionManager
import com.katui.offline.util.GoogleDriveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager,
    private val saudeCatalogoRepository: com.katui.offline.data.repository.SaudeCatalogoRepository,
    private val database: KatuiDatabase,
    private val secureStorage: SecureStorage,
    private val googleDriveManager: GoogleDriveManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val backupManager = BackupManager(context, database)
    private val dependentDeletionManager = DependentDeletionManager(database)
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    // ID do usuário sendo VISUALIZADO/EDITADO nesta tela. Por padrão segue o usuário
    // ATIVO da sessão (sessionManager.userId), mas pode ser sobrescrito para abrir o
    // perfil de um dependente sem trocar quem está ativo no resto do app (ex: alarmes,
    // medicamentos do dashboard continuam sendo do usuário ativo, não do visualizado).
    private val _viewingUserId = MutableStateFlow<Long?>(null)

    // true quando está olhando o próprio perfil ativo; false quando está "espiando"
    // o perfil de um dependente sem ter trocado para ele.
    val isViewingOwnProfile: StateFlow<Boolean> = combine(
        sessionManager.userId, _viewingUserId
    ) { activeId, viewingId -> viewingId == null || viewingId == activeId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** Chamar com um ID específico para abrir o perfil de um dependente sem trocar de sessão. */
    fun setViewingUser(userId: Long?) {
        _viewingUserId.value = userId
    }

    private val _geminiKey = MutableStateFlow(secureStorage.getGeminiKey() ?: "")
    val geminiKey: StateFlow<String> = _geminiKey.asStateFlow()

    private val _openRouterKey = MutableStateFlow(secureStorage.getOpenRouterKey() ?: "")
    val openRouterKey: StateFlow<String> = _openRouterKey.asStateFlow()

    private val _ocrProvider = MutableStateFlow(secureStorage.getOcrProvider())
    val ocrProvider: StateFlow<String> = _ocrProvider.asStateFlow()

    private val _openRouterModel = MutableStateFlow(secureStorage.getOpenRouterModel())
    val openRouterModel: StateFlow<String> = _openRouterModel.asStateFlow()

    private val _mistralKey = MutableStateFlow(secureStorage.getMistralKey() ?: "")
    val mistralKey: StateFlow<String> = _mistralKey.asStateFlow()

    val darkMode = sessionManager.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val fontSize = sessionManager.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Normal")
    val dyslexiaFont = sessionManager.dyslexiaFont.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val alarmSound = sessionManager.alarmSound.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default_alarm")

    private val _dependentes = MutableStateFlow<List<Usuario>>(emptyList())
    val dependentes: StateFlow<List<Usuario>> = _dependentes.asStateFlow()

    // Usuário ATIVO da sessão (independente do que está sendo visualizado nesta tela).
    // Usado para saber se o perfil ativo é um dependente (tem cuidadorId), e nesse caso
    // mostrar um botão para voltar ao perfil principal.
    private val _activeUsuario = MutableStateFlow<Usuario?>(null)
    val isActiveProfileDependent: StateFlow<Boolean> = _activeUsuario
        .map { it?.cuidadorId != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Troca de volta para o cuidador principal (raiz) a partir do perfil ativo. */
    fun switchToPrimaryProfile() {
        viewModelScope.launch {
            val rootId = _activeUsuario.value?.cuidadorId ?: return@launch
            sessionManager.saveUserId(rootId)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val saudeSearchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) {
                saudeCatalogoRepository.search(query)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saudeCatalogo = saudeCatalogoRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSaudeSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        // _usuario segue o usuário VISUALIZADO (viewingUserId), que por padrão (null)
        // cai no usuário ATIVO da sessão. Isso permite abrir/editar o perfil de um
        // dependente sem trocar quem está ativo no resto do app.
        combine(sessionManager.userId, _viewingUserId) { activeId, viewingId ->
            viewingId ?: activeId
        }
            .flatMapLatest { effectiveId ->
                usuarioDao.getUsuarioPorId(effectiveId)
                    .onEach { user ->
                        if (user == null && effectiveId == 1L) {
                            usuarioDao.inserirUsuario(Usuario(id = 1, nome = "Cuidador Principal", tipo = TipoUsuario.CUIDADOR))
                        }
                    }
            }
            .onEach { _usuario.value = it }
            .launchIn(viewModelScope)

        // A lista de dependentes é sempre relativa ao usuário ATIVO da sessão (não ao
        // visualizado), para não "perder" a lista ao abrir o perfil de um dependente.
        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getDependentes(userId) }
            .onEach { _dependentes.value = it }
            .launchIn(viewModelScope)

        // Acompanha sempre o usuário ATIVO da sessão, independente de visualização,
        // para saber se ele é um dependente (cuidadorId != null) e mostrar o botão de
        // "voltar ao perfil principal".
        sessionManager.userId
            .flatMapLatest { userId -> usuarioDao.getUsuarioPorId(userId) }
            .onEach { _activeUsuario.value = it }
            .launchIn(viewModelScope)
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { sessionManager.setDarkMode(enabled) }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch { sessionManager.setFontSize(size) }
    }

    fun setDyslexiaFont(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setDyslexiaFont(enabled) }
    }

    fun setAlarmSound(uri: String) {
        viewModelScope.launch { sessionManager.setAlarmSound(uri) }
    }

    fun saveGeminiKey(key: String) {
        secureStorage.saveGeminiKey(key)
        _geminiKey.value = key
    }

    fun saveOpenRouterKey(key: String) {
        secureStorage.saveOpenRouterKey(key)
        _openRouterKey.value = key
    }

    fun saveOcrProvider(provider: String) {
        secureStorage.saveOcrProvider(provider)
        _ocrProvider.value = provider
    }

    fun saveOpenRouterModel(model: String) {
        secureStorage.saveOpenRouterModel(model)
        _openRouterModel.value = model
    }

    fun saveMistralKey(key: String) {
        secureStorage.saveMistralKey(key)
        _mistralKey.value = key
    }

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    fun testKey(provider: String, key: String, model: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isValidating.value = true
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = when (provider) {
                        "gemini" -> URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key")
                        "openrouter" -> URL("https://openrouter.ai/api/v1/auth/key")
                        "mistral" -> URL("https://api.mistral.ai/v1/models")
                        else -> return@withContext false to "Provedor desconhecido"
                    }
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = if (provider == "gemini") "POST" else "GET"
                    if (provider == "openrouter" || provider == "mistral") {
                        conn.setRequestProperty("Authorization", "Bearer $key")
                    }
                    if (provider == "gemini") {
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        conn.outputStream.use { it.write("{\"contents\":[{\"parts\":[{\"text\":\"hi\"}]}]}".toByteArray()) }
                    }
                    
                    val code = conn.responseCode
                    if (code in 200..299) {
                        true to "Chave válida!"
                    } else {
                        val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                        false to "Inválida ($code): ${if (error.contains("invalid", true)) "Chave incorreta" else "Erro no servidor"}"
                    }
                } catch (e: Exception) {
                    false to "Erro de conexão: ${e.localizedMessage}"
                }
            }
            _isValidating.value = false
            onResult(result.first, result.second)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                backupManager.exportBackup(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importBackup(uri: Uri, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                backupManager.importBackup(uri)
                onResult(Result.success(true))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // Google Drive Backup
    fun backupToGoogleDrive(account: GoogleSignInAccount, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                val intent = Intent(context, com.katui.offline.service.SyncService::class.java).apply {
                    action = com.katui.offline.service.SyncService.ACTION_BACKUP
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                onResult(Result.success(true)) // Indica que o serviço iniciou
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun restoreFromGoogleDrive(account: GoogleSignInAccount, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                val intent = Intent(context, com.katui.offline.service.SyncService::class.java).apply {
                    action = com.katui.offline.service.SyncService.ACTION_RESTORE
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                onResult(Result.success(true)) // Indica que o serviço iniciou
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun getGoogleSignInIntent() = googleDriveManager.getSignInIntent()

    fun updateUsuario(usuario: Usuario) {
        viewModelScope.launch {
            usuarioDao.atualizarUsuario(usuario)
        }
    }

    fun addDependente(usuario: Usuario) {
        viewModelScope.launch {
            val primaryId = sessionManager.userId.first()
            usuarioDao.inserirUsuario(usuario.copy(
                tipo = TipoUsuario.PACIENTE,
                cuidadorId = if (primaryId == 1L) 1L else usuario.cuidadorId ?: 1L
            ))
        }
    }

    fun switchProfile(userId: Long) {
        viewModelScope.launch {
            sessionManager.saveUserId(userId)
        }
    }

    // --- Exclusão de Dependente ---

    private val _dependentSummary = MutableStateFlow<DependentDataSummary?>(null)
    val dependentSummary: StateFlow<DependentDataSummary?> = _dependentSummary.asStateFlow()

    private val _isDeletingDependent = MutableStateFlow(false)
    val isDeletingDependent: StateFlow<Boolean> = _isDeletingDependent.asStateFlow()

    private val _deleteDependentResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteDependentResult: StateFlow<Result<Unit>?> = _deleteDependentResult.asStateFlow()

    /** Carrega a contagem de dados associados, para mostrar antes de confirmar a exclusão. */
    fun loadDependentSummary(usuarioId: Long) {
        viewModelScope.launch {
            _dependentSummary.value = dependentDeletionManager.contarDados(usuarioId)
        }
    }

    fun clearDependentSummary() {
        _dependentSummary.value = null
    }

    /**
     * Exclui definitivamente o dependente e todos os dados associados.
     * Chamar apenas após o usuário confirmar explicitamente (ex: digitando o nome).
     */
    fun excluirDependente(usuarioId: Long) {
        viewModelScope.launch {
            _isDeletingDependent.value = true
            try {
                dependentDeletionManager.excluirDependenteCompleto(usuarioId)
                _deleteDependentResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _deleteDependentResult.value = Result.failure(e)
            } finally {
                _isDeletingDependent.value = false
                _dependentSummary.value = null
            }
        }
    }

    fun resetDeleteDependentResult() {
        _deleteDependentResult.value = null
    }
}
