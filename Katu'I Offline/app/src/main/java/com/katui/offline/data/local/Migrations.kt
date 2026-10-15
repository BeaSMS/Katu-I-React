package com.katui.offline.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * O banco de dados do Katu'i foi reiniciado para version = 1 em 22/06/2026,
 * já que o app ainda estava em fase de testes internos e nunca
 * teve uma versão publicada com usuários reais. As migrações antigas
 * (15 → 21) existiam apenas para builds de teste e foram removidas daqui.
 *
 * Quem já tinha um banco físico de uma build de teste anterior (gravado
 * com PRAGMA user_version entre 15 e 21) não perde os dados: o
 * DatabaseModule detecta esse caso e renumera a versão do arquivo para 1
 * ANTES do Room abri-lo (ver DatabaseModule.normalizeLegacyTestDatabaseVersion).
 * Isso é seguro porque o schema de tabelas da v1 atual é idêntico ao
 * schema real que essas builds de teste já tinham na v21 — nenhuma
 * entidade mudou, só o número da versão.
 *
 * A partir de agora (v1 em diante), qualquer mudança de schema real DEVE
 * ganhar uma Migration de verdade aqui, seguindo o padrão do Room, e um
 * teste correspondente em MigrationTest.kt usando o schema JSON real
 * exportado em app/schemas (exportSchema = true já está ativo).
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Verifica se a coluna 'categoria' já existe na tabela 'exames'. 
        // Isso é necessário porque builds de teste antigas (v21 legada) podem 
        // ter sido renumeradas para v1 pelo DatabaseModule, e nelas a 
        // coluna já existe fisicamente.
        val cursorExames = db.query("PRAGMA table_info(exames)")
        var hasCategoria = false
        while (cursorExames.moveToNext()) {
            val name = cursorExames.getString(cursorExames.getColumnIndexOrThrow("name"))
            if (name == "categoria") {
                hasCategoria = true
                break
            }
        }
        cursorExames.close()

        if (!hasCategoria) {
            db.execSQL("ALTER TABLE exames ADD COLUMN categoria TEXT NOT NULL DEFAULT 'Outros'")
        }

        // Verifica se a coluna 'corHex' já existe na tabela 'usuarios'.
        val cursorUsuarios = db.query("PRAGMA table_info(usuarios)")
        var hasCorHex = false
        while (cursorUsuarios.moveToNext()) {
            val name = cursorUsuarios.getString(cursorUsuarios.getColumnIndexOrThrow("name"))
            if (name == "corHex") {
                hasCorHex = true
                break
            }
        }
        cursorUsuarios.close()

        if (!hasCorHex) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN corHex TEXT")
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Tabela exame_resultados e seu índice
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `exame_resultados` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `exameId` INTEGER NOT NULL, 
                `metrica` TEXT NOT NULL, 
                `valor` REAL NOT NULL, 
                `unidade` TEXT, 
                `referencia` TEXT, 
                FOREIGN KEY(`exameId`) REFERENCES `exames`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exame_resultados_exameId` ON `exame_resultados` (`exameId`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `gestacoes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `usuarioId` INTEGER NOT NULL, 
                `dataUltimaMenstruacao` TEXT, 
                `dataPartoEstimada` TEXT,
                `nomeBebe` TEXT, 
                `ativa` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `consulta_lembretes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `consultaId` INTEGER NOT NULL, 
                `tipo` TEXT NOT NULL, 
                `metodo` TEXT NOT NULL DEFAULT 'NOTIFICACAO',
                `minutosAntes` INTEGER, 
                `horarioAbsoluto` TEXT, 
                `rotulo` TEXT, 
                FOREIGN KEY(`consultaId`) REFERENCES `consultas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consulta_lembretes_consultaId` ON `consulta_lembretes` (`consultaId`)")
    }
}
