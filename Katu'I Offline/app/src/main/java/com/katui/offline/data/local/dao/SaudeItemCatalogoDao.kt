package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaudeItemCatalogoDao {
    @Query("SELECT * FROM saude_catalogo ORDER BY categoria ASC, nome ASC")
    fun getAll(): Flow<List<SaudeItemCatalogo>>

    @Query("SELECT * FROM saude_catalogo WHERE tipo = :tipo ORDER BY categoria ASC, nome ASC")
    fun getByTipo(tipo: TipoSaudeItem): Flow<List<SaudeItemCatalogo>>

    @Query("SELECT * FROM saude_catalogo WHERE nome LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<SaudeItemCatalogo>>

    @Query("SELECT COUNT(*) FROM saude_catalogo")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaudeItemCatalogo>)
}
