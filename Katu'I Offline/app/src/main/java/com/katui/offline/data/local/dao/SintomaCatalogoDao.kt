package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.EfeitoColateralCatalogo
import com.katui.offline.data.local.entities.SintomaCatalogo
import kotlinx.coroutines.flow.Flow

@Dao
interface SintomaCatalogoDao {
    @Query("SELECT * FROM sintoma_catalogo WHERE nome LIKE :query || '%' ORDER BY popular DESC, nome ASC LIMIT 20")
    fun searchSintomas(query: String): Flow<List<SintomaCatalogo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSintomas(items: List<SintomaCatalogo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEfeitos(items: List<EfeitoColateralCatalogo>)

    @Query("SELECT COUNT(*) FROM sintoma_catalogo")
    suspend fun getCount(): Int

    @Query("""
        SELECT sc.* FROM sintoma_catalogo sc
        INNER JOIN efeito_colateral_catalogo ecc ON sc.id = ecc.sintomaId
        WHERE ecc.medicamentoId IN (:medicamentoIds)
    """)
    suspend fun getEfeitosParaMedicamentos(medicamentoIds: List<Long>): List<SintomaCatalogo>

    // Dado um sintoma especifico (pelo catalogoId) e a lista de catalogoIds dos medicamentos
    // ativos do usuario, retorna quais desses medicamentos tem esse sintoma como efeito colateral
    // conhecido. Usado para alertar o usuario ao registrar um sintoma fisico.
    @Query("""
        SELECT mc.nome AS nomeMedicamento, ecc.frequencia AS frequencia
        FROM efeito_colateral_catalogo ecc
        INNER JOIN medicamento_catalogo mc ON mc.id = ecc.medicamentoId
        WHERE ecc.sintomaId = :sintomaCatalogoId AND ecc.medicamentoId IN (:medicamentoCatalogoIdsAtivos)
    """)
    suspend fun getMedicamentosQueCausamSintoma(
        sintomaCatalogoId: Long,
        medicamentoCatalogoIdsAtivos: List<Long>
    ): List<MedicamentoCausador>
}

data class MedicamentoCausador(
    val nomeMedicamento: String,
    val frequencia: String
)
