package com.example.coffio.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.ExportImportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val exportImportManager = ExportImportManager(application)

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

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

    fun resetState() {
        _uiState.value = SettingsUiState.Idle
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
