package com.katui.offline.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.katui.offline.data.local.KatuiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: KatuiDatabase
) {
    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(context.getString(com.katui.offline.R.string.google_web_client_id))
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_APPDATA))
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Katu'I").build()
    }

    suspend fun backupToDrive(account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        Log.d("KATUI_DRIVE", "Starting backup process...")
        val service = getDriveService(account)
        
        // Force checkpoint for Room WAL
        Log.d("KATUI_DRIVE", "Forcing WAL checkpoint...")
        database.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        
        val dbFile = context.getDatabasePath("katui_db")
        if (!dbFile.exists()) {
            Log.e("KATUI_DRIVE", "Database file not found at: ${dbFile.absolutePath}")
            return@withContext false
        }

        Log.d("KATUI_DRIVE", "Database file found. Preparing metadata...")
        
        val appProperties = mutableMapOf(
            "db_version" to "1",
            "app_version" to (context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0")
        )

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = "katui_backup.db"
            parents = Collections.singletonList("appDataFolder")
            this.appProperties = appProperties
        }
        
        val mediaContent = FileContent("application/x-sqlite3", dbFile)

        // Search for existing backup
        Log.d("KATUI_DRIVE", "Searching for existing backup in appDataFolder...")
        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'katui_backup.db'")
            .setFields("files(id, appProperties)")
            .execute()

        if (result.files.isNotEmpty()) {
            val existingFileId = result.files[0].id
            Log.d("KATUI_DRIVE", "Existing backup found (ID: $existingFileId). Updating...")
            service.files().update(existingFileId, fileMetadata, mediaContent).execute()
        } else {
            Log.d("KATUI_DRIVE", "No existing backup found. Creating new file...")
            service.files().create(fileMetadata, mediaContent).execute()
        }
        Log.d("KATUI_DRIVE", "Backup process completed successfully.")
        true
    }

    suspend fun restoreFromDrive(account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        val service = getDriveService(account)
        
        val result = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'katui_backup.db'")
            .setFields("files(id, appProperties)")
            .execute()

        if (result.files.isEmpty()) return@withContext false

        val driveFile = result.files[0]
        val driveFileId = driveFile.id
        
        // Version Check
        val remoteDbVersion = driveFile.appProperties?.get("db_version")?.toIntOrNull() ?: 0
        val currentDbVersion = 1
        
        if (remoteDbVersion > currentDbVersion) {
            throw Exception("VERSAO_NOVA:${remoteDbVersion}")
        }

        val dbPath = context.getDatabasePath("katui_db")
        
        // Close database before overwriting
        database.close()
        
        dbPath.outputStream().use { output ->
            service.files().get(driveFileId).executeMediaAndDownloadTo(output)
        }
        
        true
    }
    
    suspend fun signOut() {
        googleSignInClient.signOut().await()
    }
}
