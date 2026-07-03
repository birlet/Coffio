package com.example.coffio.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brew_preferences")

data class BrewPreferences(
    val coffeeId: Long,
    val sieveId: Long,
    val temperature: Double,
    val coffeeWeight: Double,
    val targetYield: Double,
    val tamperPressure: Double,
    val milkVolume: Double,
    val grindSize: Double,
    val brewTime: Int
)

class BrewPreferencesManager(private val context: Context) {

    companion object {
        val COFFEE_ID = longPreferencesKey("coffee_id")
        val SIEVE_ID = longPreferencesKey("sieve_id")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val COFFEE_WEIGHT = doublePreferencesKey("coffee_weight")
        val TARGET_YIELD = doublePreferencesKey("target_yield")
        val TAMPER_PRESSURE = doublePreferencesKey("tamper_pressure")
        val MILK_VOLUME = doublePreferencesKey("milk_volume")
        val GRIND_SIZE = doublePreferencesKey("grind_size")
        val BREW_TIME = intPreferencesKey("brew_time")
        val BREW_PARAMS_EXPANDED = booleanPreferencesKey("brew_params_expanded")
        val LAST_BREW_EXPANDED = booleanPreferencesKey("last_brew_expanded")
    }

    val brewPreferencesFlow: Flow<BrewPreferences> = context.dataStore.data
        .map { preferences ->
            BrewPreferences(
                coffeeId = preferences[COFFEE_ID] ?: -1L,
                sieveId = preferences[SIEVE_ID] ?: -1L,
                temperature = preferences[TEMPERATURE] ?: 92.0,
                coffeeWeight = preferences[COFFEE_WEIGHT] ?: 18.0,
                targetYield = preferences[TARGET_YIELD] ?: 36.0,
                tamperPressure = preferences[TAMPER_PRESSURE] ?: 15.0,
                milkVolume = preferences[MILK_VOLUME] ?: 0.0,
                grindSize = preferences[GRIND_SIZE] ?: 0.0,
                brewTime = preferences[BREW_TIME] ?: 25
            )
        }

    suspend fun saveBrewPreferences(prefs: BrewPreferences) {
        context.dataStore.edit { preferences ->
            preferences[COFFEE_ID] = prefs.coffeeId
            preferences[SIEVE_ID] = prefs.sieveId
            preferences[TEMPERATURE] = prefs.temperature
            preferences[COFFEE_WEIGHT] = prefs.coffeeWeight
            preferences[TARGET_YIELD] = prefs.targetYield
            preferences[TAMPER_PRESSURE] = prefs.tamperPressure
            preferences[MILK_VOLUME] = prefs.milkVolume
            preferences[GRIND_SIZE] = prefs.grindSize
            preferences[BREW_TIME] = prefs.brewTime
        }
    }

    val brewParamsExpandedFlow: Flow<Boolean> = context.dataStore.data
        .map { it[BREW_PARAMS_EXPANDED] ?: false }

    val lastBrewExpandedFlow: Flow<Boolean> = context.dataStore.data
        .map { it[LAST_BREW_EXPANDED] ?: false }

    suspend fun setBrewParamsExpanded(expanded: Boolean) {
        context.dataStore.edit { it[BREW_PARAMS_EXPANDED] = expanded }
    }

    suspend fun setLastBrewExpanded(expanded: Boolean) {
        context.dataStore.edit { it[LAST_BREW_EXPANDED] = expanded }
    }
}
