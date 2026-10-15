package com.katui.offline.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs: SharedPreferences = try {
        createSharedPrefs(context, masterKey)
    } catch (e: Exception) {
        Log.e("SecureStorage", "Erro ao inicializar EncryptedSharedPreferences, deletando arquivo e tentando novamente", e)
        // Deleta o arquivo fisicamente para garantir um fresh start
        context.deleteSharedPreferences("secure_prefs")
        createSharedPrefs(context, masterKey)
    }

    private fun createSharedPrefs(context: Context, masterKey: MasterKey): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveGeminiKey(key: String) {
        Log.d("KATUI_DEBUG", "SecureStorage: Salvando nova Gemini Key...")
        sharedPrefs.edit().putString("gemini_key", key.trim()).apply()
    }

    fun getGeminiKey(): String? {
        val key = sharedPrefs.getString("gemini_key", null)
        Log.d("KATUI_DEBUG", "SecureStorage: Recuperando Gemini Key (está vazia: ${key.isNullOrBlank()})")
        return key
    }

    fun saveOpenRouterKey(key: String) {
        sharedPrefs.edit().putString("openrouter_key", key.trim()).apply()
    }

    fun getOpenRouterKey(): String? = sharedPrefs.getString("openrouter_key", null)

    fun saveMistralKey(key: String) {
        sharedPrefs.edit().putString("mistral_key", key.trim()).apply()
    }

    fun getMistralKey(): String? = sharedPrefs.getString("mistral_key", null)

    fun saveOcrProvider(provider: String) {
        sharedPrefs.edit().putString("ocr_provider", provider).apply()
    }

    // "gemini", "openrouter" ou "mistral"
    fun getOcrProvider(): String = sharedPrefs.getString("ocr_provider", "gemini") ?: "gemini"

    fun saveOpenRouterModel(model: String) {
        sharedPrefs.edit().putString("openrouter_model", model.trim()).apply()
    }

    fun getOpenRouterModel(): String =
        sharedPrefs.getString("openrouter_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
}
