package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Exame
import com.katui.offline.data.local.entities.ExameResultado
import kotlinx.coroutines.flow.Flow

@Dao
interface ExameDao {
    @Query("SELECT * FROM exames WHERE usuarioId = :usuarioId")
    fun getExamesByUsuario(usuarioId: Long): Flow<List<Exame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exame: Exame): Long

    @Delete
    suspend fun delete(exame: Exame)

    @Transaction
    @Query("SELECT * FROM exames WHERE id = :exameId")
    fun getExameComResultados(exameId: Long): Flow<ExameWithResultados>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResultado(resultado: ExameResultado): Long

    @Query("DELETE FROM exame_resultados WHERE exameId = :exameId")
    suspend fun deleteResultadosByExame(exameId: Long)

    // FIX: faltava filtrar por usuarioId — antes misturava o histórico de TODOS os
    // pacientes cadastrados no mesmo banco (app suporta múltiplos perfis via
    // CuidadorPacienteCrossRef). Também trocado ORDER BY id (ordem de inserção, não
    // necessariamente cronológica) por ORDER BY e.dataRegistro, a data real do exame.
    @Query("""
        SELECT er.* FROM exame_resultados er
        INNER JOIN exames e ON er.exameId = e.id
        WHERE er.metrica = :metrica AND e.usuarioId = :usuarioId
        ORDER BY e.dataRegistro ASC
    """)
    fun getHistoricoPorMetrica(metrica: String, usuarioId: Long): Flow<List<ExameResultado>>

    // FIX: lista de métricas disponíveis para o gráfico de tendências era fixa
    // ("Glicose", "Colesterol LDL", "TSH", "Hemoglobina"), ignorando o que o usuário
    // realmente registrou (ex: Creatinina, TGO, Cortisol). Agora reflete os dados reais.
    @Query("""
        SELECT DISTINCT er.metrica FROM exame_resultados er
        INNER JOIN exames e ON er.exameId = e.id
        WHERE e.usuarioId = :usuarioId
        ORDER BY er.metrica ASC
    """)
    fun getMetricasDisponiveis(usuarioId: Long): Flow<List<String>>

    @Query("""
        SELECT er.metrica, COUNT(*) as count FROM exame_resultados er
        INNER JOIN exames e ON er.exameId = e.id
        WHERE e.usuarioId = :usuarioId
        GROUP BY er.metrica
        HAVING count >= 2
        ORDER BY er.metrica ASC
    """)
    fun getMetricasComDadosSuficientes(usuarioId: Long): Flow<List<MetricaComContagem>>
}

data class MetricaComContagem(val metrica: String, val count: Int)

data class ExameWithResultados(
    @Embedded val exame: Exame,
    @Relation(
        parentColumn = "id",
        entityColumn = "exameId"
    )
    val resultados: List<ExameResultado>
)
