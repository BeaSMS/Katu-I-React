package com.katui.offline.util

import com.katui.offline.data.local.KatuiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Contagem de registros associados a um usuário, usada para avisar o cuidador
 * antes de excluir um dependente (ver DependentsSection / DependentDeletionManager).
 */
data class DependentDataSummary(
    val medicamentos: Int,
    val alarmes: Int,
    val consultas: Int,
    val exames: Int,
    val receitas: Int,
    val sintomas: Int,
    val vacinas: Int,
    val vitals: Int,
    val ciclosMenstruais: Int,
    val anticoncepcionais: Int
) {
    val total: Int get() = medicamentos + alarmes + consultas + exames + receitas +
        sintomas + vacinas + vitals + ciclosMenstruais + anticoncepcionais
}

/**
 * Centraliza a contagem e exclusão em cascata dos dados de um dependente.
 *
 * Por que isso existe: a entidade Usuario não tem nenhuma @ForeignKey declarada
 * a partir das tabelas de dados (medicamentos, alarmes, exames, etc.) com
 * onDelete = CASCADE. Excluir um Usuario direto pelo Room deixaria todos esses
 * registros órfãos no banco (referenciando um usuarioId que não existe mais).
 * Em vez de adicionar onDelete = CASCADE em 11 entidades diferentes (mudança de
 * schema arriscada em produção), esta classe apaga explicitamente, numa única
 * transação, tudo o que pertence ao usuarioId antes de apagar o próprio Usuario.
 */
class DependentDeletionManager(private val database: KatuiDatabase) {

    suspend fun contarDados(usuarioId: Long): DependentDataSummary = withContext(Dispatchers.IO) {
        DependentDataSummary(
            medicamentos = contar("medicamentos", usuarioId),
            alarmes = contar("alarmes", usuarioId),
            consultas = contar("consultas", usuarioId),
            exames = contar("exames", usuarioId),
            receitas = contar("receitas", usuarioId),
            sintomas = contar("sintomas", usuarioId),
            vacinas = contar("vacinas", usuarioId),
            vitals = contar("vitals", usuarioId),
            ciclosMenstruais = contar("ciclos_menstruais", usuarioId),
            anticoncepcionais = contar("anticoncepcionais", usuarioId)
        )
    }

    private fun contar(tabela: String, usuarioId: Long): Int {
        database.query("SELECT COUNT(*) FROM $tabela WHERE usuarioId = ?", arrayOf(usuarioId)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /**
     * Apaga definitivamente o dependente e TODOS os dados associados a ele.
     * Roda numa única transação: se qualquer passo falhar, nada é apagado.
     *
     * NÃO chamar para o usuário ativo da sessão (cuidador principal) — esta
     * função assume que usuarioId é um dependente sendo removido pelo cuidador.
     */
    suspend fun excluirDependenteCompleto(usuarioId: Long) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val db = database.openHelper.writableDatabase

            // Registro de anticoncepcional referencia anticoncepcionalId, não
            // usuarioId direto — precisa apagar via subquery antes do pai.
            db.execSQL(
                "DELETE FROM registro_anticoncepcional WHERE anticoncepcionalId IN " +
                    "(SELECT id FROM anticoncepcionais WHERE usuarioId = ?)",
                arrayOf(usuarioId)
            )

            // exame_resultados já tem onDelete = CASCADE a partir de exames,
            // então apagar exames já leva os resultados junto.
            val tabelasComUsuarioId = listOf(
                "medicamentos", "alarmes", "consultas", "exames", "receitas",
                "sintomas", "vacinas", "vitals", "ciclos_menstruais",
                "sintomas_diarios_femininos", "anticoncepcionais"
            )
            tabelasComUsuarioId.forEach { tabela ->
                db.execSQL("DELETE FROM $tabela WHERE usuarioId = ?", arrayOf(usuarioId))
            }

            // Relação cuidador-paciente (tabela auxiliar, se usada)
            db.execSQL("DELETE FROM cuidador_paciente WHERE pacienteId = ?", arrayOf(usuarioId))

            // Por fim, o próprio usuário
            db.execSQL("DELETE FROM usuarios WHERE id = ?", arrayOf(usuarioId))
        }
    }
}
