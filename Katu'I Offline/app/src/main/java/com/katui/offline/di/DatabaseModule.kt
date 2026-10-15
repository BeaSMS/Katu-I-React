package com.katui.offline.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import com.katui.offline.data.local.KatuiDatabase
import com.katui.offline.data.local.MIGRATION_1_2
import com.katui.offline.data.local.MIGRATION_2_3
import com.katui.offline.data.local.MIGRATION_3_4
import com.katui.offline.data.local.MIGRATION_4_5
import com.katui.offline.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * O banco foi reiniciado para version = 1 (ver KatuiDatabase.kt e
     * Migrations.kt para o motivo: o app ainda estava em fase de testes,
     * nunca teve usuários reais em produção). O schema de tabelas em si NÃO
     * mudou em relação à última build de teste (que estava em version = 21)
     * — só o número da versão foi reiniciado. Por isso, builds de teste
     * antigas instaladas no dispositivo (PRAGMA user_version entre 15 e 21)
     * não perdem dados: normalizeLegacyTestDatabaseVersion() apenas regrava
     * o número de versão do arquivo SQLite para 1 antes do Room abri-lo,
     * sem tocar em nenhuma tabela ou linha.
     */
    private val LEGACY_TEST_VERSIONS = 2..21

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): KatuiDatabase {
        Log.d("KATUI_DEBUG", "Iniciando provisionamento do banco de dados (Sem Criptografia)...")
        val dbName = "katui_db"

        normalizeLegacyTestDatabaseVersion(context, dbName)

        return openOrRecreateDatabase(context, dbName)
    }

    /**
     * Se existir um arquivo de banco físico de uma build de teste anterior
     * (gravado com PRAGMA user_version entre 15 e 21, de quando o schema
     * ainda contava versões antigas), regrava esse número para 1 — a nova
     * versão única que o Room passa a esperar. Isso NÃO altera nenhuma
     * tabela nem dado: é só o cabeçalho de versão do arquivo SQLite.
     *
     * Importante: isso só é seguro porque o schema de tabelas é idêntico
     * entre a antiga v21 de teste e a nova v1. Se algum dia o schema da v1
     * divergir do schema real da v21 antiga, essa função PRECISA ser
     * substituída por uma migração de dados de verdade (ex: copiar linha a
     * linha para um banco novo), e não apenas uma renumeração.
     */
    private fun normalizeLegacyTestDatabaseVersion(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        try {
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            try {
                // Garante que qualquer escrita pendente no -wal (de uma sessão
                // anterior do Room em WAL mode) esteja de fato gravada no
                // arquivo principal antes de tocarmos no cabeçalho de versão.
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (checkpointError: Exception) {
                    Log.w("KATUI_DEBUG", "Checkpoint WAL falhou (banco pode não estar em WAL mode, ok ignorar)", checkpointError)
                }

                val currentVersion = db.version
                if (currentVersion in LEGACY_TEST_VERSIONS) {
                    Log.w(
                        "KATUI_DEBUG",
                        "Banco de teste antigo detectado (user_version=$currentVersion). " +
                            "Renumerando para version=1 sem alterar dados."
                    )
                    db.version = 1
                }
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            // Se não conseguirmos nem abrir o arquivo para checar a versão
            // (ex: arquivo corrompido), deixamos para o openOrRecreateDatabase
            // tratar isso normalmente — não interrompemos o fluxo aqui.
            Log.e("KATUI_DEBUG", "Falha ao checar/normalizar a versão legada do banco", e)
        }
    }

    /**
     * Room.databaseBuilder(...).build() é "lazy" — ele NÃO abre a conexão real
     * com o SQLite imediatamente, apenas monta os metadados. A conexão de fato
     * só é estabelecida na primeira query, que no Room moderno acontece dentro
     * do InvalidationTracker, geralmente alguns segundos DEPOIS da Activity já
     * estar na tela. Isso significa que um arquivo de banco corrompido (ou um
     * resíduo do antigo formato criptografado SQLCipher, de versões anteriores
     * do app) não falha no .build(), e sim minutos/segundos depois, em uma
     * coroutine de background do Room, podendo matar o processo sem tratamento.
     *
     * A correção força a abertura real da conexão AQUI, ainda dentro do
     * provideDatabase. IMPORTANTE: só tratamos como "arquivo inválido" (e
     * portanto fazemos backup + recriação) os erros que comprovadamente indicam
     * isso — ver isInvalidDatabaseFileError(). Qualquer outro erro (migração
     * ausente/incorreta, disco sem espaço, I/O, etc.) é relançado para travar o
     * app: é preferível um crash visível e corrigível via atualização do que
     * apagar silenciosamente os dados do usuário por uma falha transitória ou
     * um bug de migração que essa heurística não soube identificar.
     */
    private fun isInvalidDatabaseFileError(e: Throwable): Boolean {
        val message = e.message ?: ""
        return message.contains("file is not a database", ignoreCase = true) ||
            message.contains("not a database", ignoreCase = true) ||
            e is android.database.sqlite.SQLiteDatabaseCorruptException
    }

    private fun openOrRecreateDatabase(
        context: Context,
        dbName: String
    ): KatuiDatabase {
        fun build(): KatuiDatabase {
            return Room.databaseBuilder(context, KatuiDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .enableMultiInstanceInvalidation()
                .build()
        }

        val db = build()

        return try {
            db.openHelper.writableDatabase
            Log.d("KATUI_DEBUG", "Conexão com o banco validada com sucesso")
            db
        } catch (e: Exception) {
            // SÓ tratamos como "arquivo inválido" (resíduo de formato antigo/corrompido)
            // quando a exceção comprovadamente indica isso. Qualquer outro motivo (erro
            // de migração, disco sem espaço, I/O transitório, etc.) é relançado: é
            // preferível o app travar com um crash rastreável do que apagar os dados
            // do usuário por engano.
            if (!isInvalidDatabaseFileError(e)) {
                Log.e("KATUI_DEBUG", "Erro ao abrir o banco que NÃO é de arquivo inválido. Interrompendo para evitar perda de dados.", e)
                db.close()
                throw e
            }

            Log.e("KATUI_DEBUG", "Arquivo de banco inválido/corrompido detectado (ex: resíduo de formato antigo)", e)
            db.close()

            // Em vez de deletar, movemos para um backup para possibilitar 
            // recuperação manual se necessário.
            backupDatabaseFiles(context, dbName)

            Log.w("KATUI_DEBUG", "Tentando recriar o banco do zero...")
            val recreated = build()
            try {
                recreated.openHelper.writableDatabase
                Log.d("KATUI_DEBUG", "Novo banco criado com sucesso")
                recreated
            } catch (recreateError: Exception) {
                Log.e("KATUI_DEBUG", "Erro fatal: não foi possível nem criar um banco novo", recreateError)
                throw recreateError
            }
        }
    }

    @Provides
    fun provideUsuarioDao(db: KatuiDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideMedicamentoDao(db: KatuiDatabase): MedicamentoDao = db.medicamentoDao()

    @Provides
    fun provideSintomaDao(db: KatuiDatabase): SintomaDao = db.sintomaDao()

    @Provides
    fun provideConsultaDao(db: KatuiDatabase): ConsultaDao = db.consultaDao()

    @Provides
    fun provideAlarmeDao(db: KatuiDatabase): AlarmeDao = db.alarmeDao()

    @Provides
    fun provideExameDao(db: KatuiDatabase): ExameDao = db.exameDao()

    @Provides
    fun provideReceitaDao(db: KatuiDatabase): ReceitaDao = db.receitaDao()

    @Provides
    fun provideVacinaDao(db: KatuiDatabase): VacinaDao = db.vacinaDao()

    @Provides
    fun provideCicloMenstrualDao(db: KatuiDatabase): CicloMenstrualDao = db.cicloMenstrualDao()

    @Provides
    fun provideAnticoncepcionalDao(db: KatuiDatabase): AnticoncepcionalDao = db.anticoncepcionalDao()

    @Provides
    fun provideSintomaDiarioFemininoDao(db: KatuiDatabase): SintomaDiarioFemininoDao = db.sintomaDiarioFemininoDao()

    @Provides
    fun provideVitalsDao(db: KatuiDatabase): VitalsDao = db.vitalsDao()

    @Provides
    fun provideGestacaoDao(db: KatuiDatabase): GestacaoDao = db.gestacaoDao()

    @Provides
    fun provideConsultaLembreteDao(db: KatuiDatabase): ConsultaLembreteDao = db.consultaLembreteDao()

    @Provides
    fun provideMedicamentoCatalogoDao(db: KatuiDatabase): MedicamentoCatalogoDao = db.medicamentoCatalogoDao()

    @Provides
    fun provideSintomaCatalogoDao(db: KatuiDatabase): SintomaCatalogoDao = db.sintomaCatalogoDao()

    @Provides
    fun provideSaudeItemCatalogoDao(db: KatuiDatabase): SaudeItemCatalogoDao = db.saudeItemCatalogoDao()

    private fun backupDatabaseFiles(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        val timestamp = System.currentTimeMillis()
        val backupDir = File(context.filesDir, "database_backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        fun moveFile(name: String) {
            val file = context.getDatabasePath(name)
            if (file.exists()) {
                val dest = File(backupDir, "${name}_$timestamp.bak")
                if (file.renameTo(dest)) {
                    Log.w("KATUI_DEBUG", "Arquivo $name movido para backup: ${dest.absolutePath}")
                } else {
                    Log.e("KATUI_DEBUG", "Falha ao mover $name para backup, deletando...")
                    file.delete()
                }
            }
        }

        moveFile(dbName)
        moveFile("$dbName-shm")
        moveFile("$dbName-wal")
    }
}
