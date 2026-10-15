package com.katui.offline.util

import android.content.Context
import android.net.Uri
import com.katui.offline.data.local.KatuiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context, private val database: KatuiDatabase) {

    suspend fun exportBackup(outputUri: Uri) = withContext(Dispatchers.IO) {
        // Forçar checkpoint para garantir que o arquivo .db está atualizado (Room usa WAL)
        database.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }

        val dbPath = context.getDatabasePath("katui_db")
        val dbShm = File(dbPath.absolutePath + "-shm")
        val dbWal = File(dbPath.absolutePath + "-wal")

        context.contentResolver.openOutputStream(outputUri)?.use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Adicionar metadados
                val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                val metadata = """
                    {
                        "db_version": 1,
                        "app_version": "$appVersion",
                        "timestamp": ${System.currentTimeMillis()}
                    }
                """.trimIndent()
                
                val metadataEntry = ZipEntry("metadata.json")
                zos.putNextEntry(metadataEntry)
                zos.write(metadata.toByteArray())
                zos.closeEntry()

                addToZip(zos, dbPath, "katui_db")
                if (dbShm.exists()) addToZip(zos, dbShm, "katui_db-shm")
                if (dbWal.exists()) addToZip(zos, dbWal, "katui_db-wal")
                
                // Backup de arquivos de mídia se existirem
                val mediaDir = context.getExternalFilesDir(null)
                mediaDir?.listFiles()?.forEach { file ->
                    if (file.isFile) addToZip(zos, file, "media/${file.name}")
                }
            }
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    suspend fun importBackup(inputUri: Uri) = withContext(Dispatchers.IO) {
        val dbPath = context.getDatabasePath("katui_db")
        val dbDir = dbPath.parentFile
        val currentDbVersion = 1

        context.contentResolver.openInputStream(inputUri)?.use { fis ->
            // Primeira passada: Verificar metadados
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "metadata.json") {
                        val content = zis.bufferedReader().readText()
                        val dbVersionMatch = "\"db_version\":\\s*(\\d+)".toRegex().find(content)
                        val backupDbVersion = dbVersionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        
                        if (backupDbVersion > currentDbVersion) {
                            throw Exception("VERSAO_NOVA:$backupDbVersion")
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        }

        // Se chegou aqui, a versão é compatível ou o arquivo é antigo (sem metadados)
        database.close()

        context.contentResolver.openInputStream(inputUri)?.use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(dbDir, entry.name)
                    if (entry.name.startsWith("media/")) {
                        val mediaFile = File(context.getExternalFilesDir(null), entry.name.removePrefix("media/"))
                        mediaFile.parentFile?.mkdirs()
                        mediaFile.outputStream().use { zis.copyTo(it) }
                    } else if (entry.name != "metadata.json") {
                        file.outputStream().use { zis.copyTo(it) }
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }
}
