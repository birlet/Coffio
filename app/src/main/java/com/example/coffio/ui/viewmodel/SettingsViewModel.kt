package com.example.coffio.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.coffio.data.ExportImportManager
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.datastore.AppPreferencesManager
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.local.entities.Sieve
import com.example.coffio.data.sync.SyncManager
import com.example.coffio.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val drinkDao = database.drinkDao()
    private val sieveDao = database.sieveDao()
    private val coffeeDao = database.coffeeDao()
    private val exportImportManager = ExportImportManager(application)
    private val appPreferencesManager = AppPreferencesManager(application)
    private val syncManager = SyncManager(application)

    val language: StateFlow<AppLanguage> = appPreferencesManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val syncEnabled: StateFlow<Boolean> = appPreferencesManager.syncEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncServer: StateFlow<String> = appPreferencesManager.syncServerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val drinks: StateFlow<List<Drink>> = drinkDao.getAllDrinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sieves: StateFlow<List<Sieve>> = sieveDao.getAllSieves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coffees: StateFlow<List<Coffee>> = coffeeDao.getAllCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun addDrink(drink: Drink) {
        viewModelScope.launch {
            drinkDao.insertDrink(drink)
        }
    }

    fun updateDrink(drink: Drink) {
        viewModelScope.launch {
            drinkDao.updateDrink(drink)
        }
    }

    fun deleteDrink(drink: Drink) {
        viewModelScope.launch {
            drinkDao.deleteDrink(drink)
        }
    }

    fun exportDrinks(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val result = exportImportManager.exportDrinksToJson(uri)
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("Drinks exported successfully")
            } else {
                _uiState.value = SettingsUiState.Error("Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importDrinks(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val result = exportImportManager.importDrinksFromJson(uri)
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("Drinks imported successfully")
            } else {
                _uiState.value = SettingsUiState.Error("Import failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val result = exportImportManager.exportDatabaseToCsv(uri)
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("Database exported successfully")
            } else {
                _uiState.value = SettingsUiState.Error("Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val result = exportImportManager.importDatabaseFromCsv(uri)
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("Database imported successfully")
            } else {
                _uiState.value = SettingsUiState.Error("Import failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appPreferencesManager.saveLanguage(language)
        }
    }

    fun resetState() {
        _uiState.value = SettingsUiState.Idle
    }

    fun resetDatabase() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                database.withTransaction {
                    brewDao.deleteAllBrews()
                    drinkDao.deleteAllDrinks()
                    coffeeDao.deleteAllCoffees()
                    sieveDao.deleteAllSieves()
                }
                _uiState.value = SettingsUiState.Success("Database reset completed")
            } catch (exception: Exception) {
                _uiState.value = SettingsUiState.Error("Reset failed: ${exception.message}")
            }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.saveSyncEnabled(enabled)
        }
    }

    fun setSyncServer(server: String) {
        viewModelScope.launch {
            appPreferencesManager.saveSyncServer(server)
        }
    }

    fun runSyncNow() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val server = syncServer.value
            if (server.isBlank()) {
                _uiState.value = SettingsUiState.Error("Sync server is empty")
                return@launch
            }

            val result = syncManager.sync(server)
            if (result.isSuccess) {
                val inserted = result.getOrNull() ?: 0
                _uiState.value = SettingsUiState.Success("Sync completed. Added $inserted new brews")
            } else {
                _uiState.value = SettingsUiState.Error("Sync failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
