package com.katui.offline.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katui.offline.data.SessionManager
import com.katui.offline.data.local.entities.Receita
import com.katui.offline.data.repository.ReceitaRepository
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
class PrescriptionViewModel @Inject constructor(
    private val repository: ReceitaRepository,
    private val usuarioDao: com.katui.offline.data.local.dao.UsuarioDao,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _receitas = MutableStateFlow<List<Receita>>(emptyList())
    val receitas: StateFlow<List<Receita>> = _receitas.asStateFlow()

    private val _usuario = MutableStateFlow<com.katui.offline.data.local.entities.Usuario?>(null)
    val usuario: StateFlow<com.katui.offline.data.local.entities.Usuario?> = _usuario.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _timeScope = MutableStateFlow(TimeScope.DAY)
    val timeScope: StateFlow<TimeScope> = _timeScope.asStateFlow()

    val filteredReceitas = combine(_receitas, _selectedDate, _timeScope) { list, date, scope ->
        list.filter { receita ->
            when (scope) {
                TimeScope.ALL -> true
                TimeScope.MONTH -> {
                    val rDate = receita.dataRegistro.toLocalDate()
                    rDate.year == date.year && rDate.month == date.month
                }
                TimeScope.DAY -> receita.dataRegistro.toLocalDate() == date
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sessionManager.userId
            .flatMapLatest { userId -> repository.getReceitasByUsuario(userId) }
            .onEach { _receitas.value = it }
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

    /** Receita a partir de imagem. */
    fun addReceita(imagemUriStr: String?, observacao: String?) {
        viewModelScope.launch {
            val userId = sessionManager.userId.first()
            
            val finalPath = imagemUriStr?.let { uriStr ->
                try {
                    val uri = Uri.parse(uriStr)
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        withContext(Dispatchers.IO) {
                            PdfManager.copyImageToInternal(context, uri)?.absolutePath
                        }
                    } else uriStr
                } catch (e: Exception) {
                    uriStr
                }
            }

            repository.insert(
                Receita(
                    usuarioId = userId,
                    imagemPath = finalPath,
                    observacao = observacao,
                    isPdf = false
                )
            )
        }
    }

    /**
     * Receita a partir de um PDF selecionado pelo usuário.
     * O arquivo é copiado para o armazenamento interno.
     */
    fun addReceitaFromPdf(context: Context, pdfUri: Uri, observacao: String?) {
        viewModelScope.launch {
            val savedFile = withContext(Dispatchers.IO) {
                PdfManager.copyPdfToInternal(context, pdfUri)
            }
            val userId = sessionManager.userId.first()
            repository.insert(
                Receita(
                    usuarioId = userId,
                    imagemPath = savedFile?.absolutePath,
                    observacao = observacao,
                    isPdf = true
                )
            )
        }
    }

    fun deleteReceita(receita: Receita) {
        viewModelScope.launch {
            repository.delete(receita)
        }
    }
}
