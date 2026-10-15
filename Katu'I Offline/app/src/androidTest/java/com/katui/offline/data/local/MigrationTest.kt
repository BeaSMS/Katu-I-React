package com.katui.offline.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de migração do banco de dados do Katu'i.
 *
 * O banco foi reiniciado para version = 1 em 22/06/2026 (ver KatuiDatabase.kt
 * e Migrations.kt): o app ainda estava em fase de testes internos, sem
 * usuários reais em produção, então não fazia sentido carregar 21 versões
 * de histórico — boa parte delas sem schema JSON real exportado, já que
 * `exportSchema = true` só foi ativado depois.
 *
 * Por isso este teste não cobre mais uma cadeia de migrações antigas
 * (o teste anterior, que cobria 15→21, ficou obsoleto e foi removido junto
 * com Migrations.kt). O que resta validar hoje é que o Room consegue criar
 * o banco do zero exatamente como as entidades atuais declaram.
 *
 * A partir de agora (v1 em diante), toda nova Migration real adicionada em
 * Migrations.kt deve ganhar seu próprio teste aqui, usando o schema JSON
 * exportado em app/schemas (gerado automaticamente a cada build, graças a
 * `exportSchema = true` + `room.schemaLocation` no build.gradle.kts do
 * módulo app) — não SQL reconstruído manualmente, como foi necessário fazer
 * uma única vez no histórico antigo por falta desses arquivos.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KatuiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Verifica que o Room consegue criar o banco do zero na v1 (schema
     * único e atual) sem precisar de nenhuma migração, e que esse schema
     * bate com o que as entidades declaram. É exatamente o que roda na
     * primeira instalação do app por um usuário novo.
     */
    @Test
    fun abrirBancoNovoNaVersaoAtual_naoLancaExcecao() {
        helper.createDatabase(testDbName, 1).close()
        // Reabre validando contra o schema declarado nas entidades atuais (sem migrações a aplicar).
        helper.runMigrationsAndValidate(testDbName, 1, true)
    }

    @Test
    fun testMigration1to3() {
        // Cria banco na v1
        helper.createDatabase(testDbName, 1).close()

        // Roda migrações 1->2 e 2->3
        helper.runMigrationsAndValidate(testDbName, 3, true, MIGRATION_1_2, MIGRATION_2_3)
    }

    @Test
    fun testMigration1to2_comColunaJaExistente_naoLancaExcecao() {
        // Simula o cenário do crash: banco v1 que JÁ TEM a coluna (ex: legado v21 renumerado)
        val db = helper.createDatabase(testDbName, 1)
        db.execSQL("ALTER TABLE exames ADD COLUMN categoria TEXT NOT NULL DEFAULT 'Legado'")
        db.close()

        // Tenta rodar a migração 1->2. O fix idempotente deve evitar o crash.
        helper.runMigrationsAndValidate(testDbName, 2, true, MIGRATION_1_2)
    }
}
