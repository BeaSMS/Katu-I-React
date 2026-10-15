package com.katui.offline.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Ajuda a guiar o usuário até as telas de permissão que garantem que os
 * alarmes do Katu'i consigam abrir a tela cheia mesmo em segundo plano.
 *
 * IMPORTANTE — duas categorias bem diferentes aqui:
 *
 * 1) Permissões OFICIAIS do Android (otimização de bateria, full-screen
 *    intent no Android 14+): existem em qualquer fabricante, são API
 *    pública e documentada, e podem ser verificadas em código.
 *
 * 2) Atalhos PROPRIETÁRIOS da MIUI/HyperOS (Autostart, "Outras permissões"
 *    / pop-up em segundo plano): não são API pública. Os nomes de
 *    Activity podem mudar entre versões e regiões da MIUI, e não existe
 *    forma de verificar programaticamente se o usuário realmente ativou.
 *    Por isso, sempre tentamos o Intent conhecido com fallback seguro
 *    para a tela genérica "Detalhes do app".
 */
object MiuiPermissionHelper {

    private const val TAG = "KATUI_PERMS"

    /** Detecta Xiaomi e suas sub-marcas (Redmi, Poco), que usam MIUI/HyperOS. */
    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer == "xiaomi" || manufacturer == "redmi" || manufacturer == "poco"
    }

    /** Tela padrão "Detalhes do app" — fallback universal, sempre existe. */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Não foi possível abrir Detalhes do app", e)
        }
    }

    // ---------------------------------------------------------------------
    // Oficiais do Android (funcionam em qualquer fabricante)
    // ---------------------------------------------------------------------

    /**
     * Pede para o sistema ignorar a otimização de bateria para o app.
     * Requer android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS no Manifest.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Intent de bateria indisponível, usando fallback", e)
            openAppDetailsSettings(context)
        }
    }

    /**
     * No Android 14+ (API 34), apps sem função de "ligação" ou "alarme"
     * declarada podem ter o full-screen intent bloqueado por padrão.
     * Esta checagem é 100% confiável via API oficial.
     */
    fun needsFullScreenIntentPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return notificationManager?.canUseFullScreenIntent() == false
    }

    /** Abre a tela oficial "Acesso especial > Tela cheia" do Android 14+. */
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Intent de full-screen intent indisponível, usando fallback", e)
            openAppDetailsSettings(context)
        }
    }

    // ---------------------------------------------------------------------
    // Proprietárias da MIUI/HyperOS (best-effort, sem garantia)
    // ---------------------------------------------------------------------

    /** Tela de "Inicialização automática" (Autostart) da MIUI. */
    fun openAutoStartSettings(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Tela de Autostart não encontrada nesta MIUI, usando fallback", e)
            openAppDetailsSettings(context)
        }
    }

    /**
     * Tela de "Outras permissões" da MIUI — inclui "Exibir janelas pop-up
     * enquanto em segundo plano", essencial para o alarme abrir por cima
     * de outros apps sem precisar de toque na notificação.
     */
    fun openOtherPermissionsSettings(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Tela de Outras Permissões não encontrada nesta MIUI, usando fallback", e)
            openAppDetailsSettings(context)
        }
    }
}
