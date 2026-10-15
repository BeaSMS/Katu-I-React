package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.SintomaCatalogoDao
import com.katui.offline.data.local.dao.SintomaDao
import com.katui.offline.data.local.dao.MedicamentoCausador
import com.katui.offline.data.local.entities.EfeitoColateralCatalogo
import com.katui.offline.data.local.entities.Sintoma
import com.katui.offline.data.local.entities.SintomaCatalogo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SintomaRepository @Inject constructor(
    private val sintomaDao: SintomaDao,
    private val catalogoDao: SintomaCatalogoDao
) {
    fun getSintomas(usuarioId: Long): Flow<List<Sintoma>> =
        sintomaDao.getSintomasByUsuario(usuarioId)

    fun searchCatalogo(query: String): Flow<List<SintomaCatalogo>> =
        catalogoDao.searchSintomas(query)

    suspend fun getCatalogoCount(): Int = catalogoDao.getCount()

    suspend fun insertCatalogoItems(items: List<SintomaCatalogo>) =
        catalogoDao.insertSintomas(items)

    suspend fun insertEfeitoMapeamento(items: List<EfeitoColateralCatalogo>) =
        catalogoDao.insertEfeitos(items)

    suspend fun getEfeitosParaMedicamentos(medIds: List<Long>) =
        catalogoDao.getEfeitosParaMedicamentos(medIds)

    // Verifica se algum dos medicamentos ativos do usuario (identificados pelo catalogoId
    // do MedicamentoCatalogo) tem o sintoma escolhido como efeito colateral conhecido.
    suspend fun getMedicamentosQueCausamSintoma(
        sintomaCatalogoId: Long,
        medicamentoCatalogoIdsAtivos: List<Long>
    ): List<MedicamentoCausador> {
        if (medicamentoCatalogoIdsAtivos.isEmpty()) return emptyList()
        return catalogoDao.getMedicamentosQueCausamSintoma(sintomaCatalogoId, medicamentoCatalogoIdsAtivos)
    }

    suspend fun insertSintoma(sintoma: Sintoma) =
        sintomaDao.insert(sintoma)

    suspend fun updateSintoma(sintoma: Sintoma) =
        sintomaDao.update(sintoma)

    suspend fun deleteSintoma(sintoma: Sintoma) =
        sintomaDao.delete(sintoma)
}
