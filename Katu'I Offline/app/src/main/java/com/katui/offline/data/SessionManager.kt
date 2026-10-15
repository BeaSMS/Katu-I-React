package com.katui.offline.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val DYSLEXIA_FONT_KEY = booleanPreferencesKey("dyslexia_font")
        private val ALARM_SOUND_KEY = stringPreferencesKey("alarm_sound")
        private val PREGNANCY_HIDDEN_KEY = booleanPreferencesKey("pregnancy_hidden")
    }

    val userId: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: 1L
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETE_KEY] ?: false
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY]
    }

    val fontSize: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: "Normal"
    }

    val dyslexiaFont: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYSLEXIA_FONT_KEY] ?: false
    }

    val alarmSound: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALARM_SOUND_KEY] ?: "default_alarm"
    }

    val pregnancyHidden: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREGNANCY_HIDDEN_KEY] ?: false
    }

    suspend fun saveUserId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = id
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        Log.d("KATUI_DEBUG", "setOnboardingComplete($complete)")
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE_KEY] = complete
        }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(DARK_MODE_KEY)
            } else {
                preferences[DARK_MODE_KEY] = enabled
            }
        }
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size
        }
    }

    suspend fun setDyslexiaFont(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYSLEXIA_FONT_KEY] = enabled
        }
    }

    suspend fun setAlarmSound(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_SOUND_KEY] = uri
        }
    }

    suspend fun setPregnancyHidden(hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREGNANCY_HIDDEN_KEY] = hidden
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
        }
    }
}
