package com.katui.offline.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.katui.offline.R
import com.katui.offline.util.GoogleDriveManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {

    @Inject
    lateinit var googleDriveManager: GoogleDriveManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_BACKUP = "com.katui.offline.action.BACKUP"
        const val ACTION_RESTORE = "com.katui.offline.action.RESTORE"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "katui_sync_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("KATUI_SYNC", "SyncService started with action: $action")

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Log.e("KATUI_SYNC", "No Google account found for sync")
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_BACKUP -> {
                startForeground(NOTIFICATION_ID, createNotification("Realizando backup..."))
                serviceScope.launch {
                    try {
                        val success = googleDriveManager.backupToDrive(account)
                        Log.d("KATUI_SYNC", "Backup success: $success")
                    } catch (e: Exception) {
                        Log.e("KATUI_SYNC", "Backup failed", e)
                    } finally {
                        stopSelf()
                    }
                }
            }
            ACTION_RESTORE -> {
                startForeground(NOTIFICATION_ID, createNotification("Restaurando dados..."))
                serviceScope.launch {
                    try {
                        val success = googleDriveManager.restoreFromDrive(account)
                        Log.d("KATUI_SYNC", "Restore success: $success")
                    } catch (e: Exception) {
                        Log.e("KATUI_SYNC", "Restore failed", e)
                    } finally {
                        stopSelf()
                    }
                }
            }
            else -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(message: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Sincronização Katu'I")
        .setContentText(message)
        .setSmallIcon(com.katui.offline.R.drawable.ic_notification_custom)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronização de Dados",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
