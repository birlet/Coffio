package com.example.coffio.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.coffio.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferencesManager(private val context: Context) {

    companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val SYNC_SERVER = stringPreferencesKey("sync_server")
    }

    val languageFlow: Flow<AppLanguage> = context.appDataStore.data
        .map { preferences ->
            when (preferences[LANGUAGE]) {
                AppLanguage.GERMAN.code -> AppLanguage.GERMAN
                else -> AppLanguage.ENGLISH
            }
        }

    val syncEnabledFlow: Flow<Boolean> = context.appDataStore.data
        .map { preferences -> preferences[SYNC_ENABLED] ?: false }

    val syncServerFlow: Flow<String> = context.appDataStore.data
        .map { preferences -> preferences[SYNC_SERVER] ?: "" }

    suspend fun saveLanguage(language: AppLanguage) {
        context.appDataStore.edit { preferences ->
            preferences[LANGUAGE] = language.code
        }
    }

    suspend fun saveSyncEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveSyncServer(server: String) {
        context.appDataStore.edit { preferences ->
            preferences[SYNC_SERVER] = server.trim()
        }
    }
}
