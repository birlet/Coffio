package com.example.coffio.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.coffio.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferencesManager(private val context: Context) {

    companion object {
        val LANGUAGE = stringPreferencesKey("language")
    }

    val languageFlow: Flow<AppLanguage> = context.appDataStore.data
        .map { preferences ->
            when (preferences[LANGUAGE]) {
                AppLanguage.GERMAN.code -> AppLanguage.GERMAN
                else -> AppLanguage.ENGLISH
            }
        }

    suspend fun saveLanguage(language: AppLanguage) {
        context.appDataStore.edit { preferences ->
            preferences[LANGUAGE] = language.code
        }
    }
}
