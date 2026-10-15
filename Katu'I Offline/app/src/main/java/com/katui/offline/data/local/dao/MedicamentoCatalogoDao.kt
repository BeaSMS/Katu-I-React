package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.MedicamentoCatalogo
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoCatalogoDao {
    @Query("SELECT * FROM medicamento_catalogo WHERE nome LIKE :query || '%' OR principioAtivo LIKE :query || '%' LIMIT 20")
    fun searchMedicamentos(query: String): Flow<List<MedicamentoCatalogo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicamentos: List<MedicamentoCatalogo>)

    @Query("SELECT COUNT(*) FROM medicamento_catalogo")
    suspend fun getCount(): Int
}
