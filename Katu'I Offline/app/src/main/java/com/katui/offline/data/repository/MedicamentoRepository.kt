package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.MedicamentoCatalogoDao
import com.katui.offline.data.local.dao.MedicamentoDao
import com.katui.offline.data.local.dao.ReceitaDao
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.local.entities.MedicamentoCatalogo
import com.katui.offline.data.local.entities.Receita
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicamentoRepository @Inject constructor(
    private val medicamentoDao: MedicamentoDao,
    private val catalogoDao: MedicamentoCatalogoDao,
    private val receitaDao: ReceitaDao,
    private val usuarioDao: com.katui.offline.data.local.dao.UsuarioDao
) {
    fun getMedicamentos(usuarioId: Long): Flow<List<Medicamento>> =
        medicamentoDao.getMedicamentosByUsuario(usuarioId)

    fun searchCatalogo(query: String): Flow<List<MedicamentoCatalogo>> =
        catalogoDao.searchMedicamentos(query)

    suspend fun getCatalogoCount(): Int = catalogoDao.getCount()

    suspend fun insertCatalogoItems(items: List<MedicamentoCatalogo>) =
        catalogoDao.insertAll(items)

    suspend fun getMedicamento(id: Long): Medicamento? =
        medicamentoDao.getMedicamento(id)

    suspend fun ajustarEstoque(id: Long, delta: Double) {
        // delta negativo = desconta (tomou); delta positivo = devolve (desmarcou)
        val med = medicamentoDao.getMedicamento(id) ?: return
        val novoEstoque = (med.estoqueAtual + delta).coerceAtLeast(0.0)
        medicamentoDao.update(med.copy(estoqueAtual = novoEstoque))
    }

    suspend fun insertMedicamento(medicamento: Medicamento) =
        medicamentoDao.insert(medicamento)

    suspend fun getMedicamentoByName(usuarioId: Long, nome: String) =
        medicamentoDao.getMedicamentoByName(usuarioId, nome)

    suspend fun updateMedicamento(medicamento: Medicamento) =
        medicamentoDao.update(medicamento)

    suspend fun deleteMedicamento(medicamento: Medicamento) =
        medicamentoDao.delete(medicamento)

    suspend fun insertReceita(receita: Receita) =
        receitaDao.insert(receita)

    fun getUsuario(id: Long): Flow<com.katui.offline.data.local.entities.Usuario?> =
        usuarioDao.getUsuarioPorId(id)
}
