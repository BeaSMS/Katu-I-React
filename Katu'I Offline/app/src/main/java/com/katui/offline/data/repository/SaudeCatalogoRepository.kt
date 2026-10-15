package com.katui.offline.data.repository

import com.katui.offline.data.local.dao.SaudeItemCatalogoDao
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaudeCatalogoRepository @Inject constructor(
    private val saudeItemCatalogoDao: SaudeItemCatalogoDao
) {
    fun getAll(): Flow<List<SaudeItemCatalogo>> = saudeItemCatalogoDao.getAll()
    
    fun getByTipo(tipo: TipoSaudeItem): Flow<List<SaudeItemCatalogo>> = saudeItemCatalogoDao.getByTipo(tipo)
    
    fun search(query: String): Flow<List<SaudeItemCatalogo>> = saudeItemCatalogoDao.search(query)
    
    suspend fun getCount(): Int = saudeItemCatalogoDao.getCount()
    
    suspend fun insertAll(items: List<SaudeItemCatalogo>) = saudeItemCatalogoDao.insertAll(items)
}
