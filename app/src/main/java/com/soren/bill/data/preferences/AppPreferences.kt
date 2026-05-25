package com.soren.bill.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {
    private val THEME_MODE_KEY = stringPreferencesKey("mode")
    private val CONFIRM_BEFORE_SAVING_KEY = booleanPreferencesKey("confirm_before_saving")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeString)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    val confirmBeforeSaving: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CONFIRM_BEFORE_SAVING_KEY] ?: true
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setConfirmBeforeSaving(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONFIRM_BEFORE_SAVING_KEY] = confirm
        }
    }
}
