package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.datastore.AppPreferencesManager
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewSource
import com.example.coffio.data.local.entities.BrewWithCoffee
import com.example.coffio.data.local.entities.DeletedBrew
import com.example.coffio.data.sync.SyncBrewDto
import com.example.coffio.data.sync.SyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val brewDao = database.brewDao()
    private val coffeeDao = database.coffeeDao()
    private val sieveDao = database.sieveDao()
    private val drinkDao = database.drinkDao()
    private val syncManager = SyncManager(application)
    private val appPreferencesManager = AppPreferencesManager(application)

    val historyState: StateFlow<List<BrewWithCoffee>> = brewDao.getAllBrewsWithCoffeeIncludingDataOnly()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteBrew(brew: Brew) {
        viewModelScope.launch {
            brewDao.deleteBrew(brew)
            if (brew.syncKey.isNotBlank()) {
                database.deletedBrewDao().insert(DeletedBrew(syncKey = brew.syncKey))
            }
            if (brew.source != BrewSource.IMPORTED) {
                syncDeleteToServer(brew.syncKey)
            }
        }
    }

    fun updateBrew(brew: Brew) {
        viewModelScope.launch {
            brewDao.insertBrew(brew)
            syncUpdateToServer(brew)
        }
    }

    private suspend fun syncDeleteToServer(syncKey: String) {
        val server = serverIfEnabled() ?: return
        syncManager.deleteBrewOnServer(server, syncKey)
    }

    private suspend fun syncUpdateToServer(brew: Brew) {
        if (brew.source == BrewSource.IMPORTED) {
            return
        }
        val server = serverIfEnabled() ?: return
        val coffeeName = coffeeDao.getCoffeeById(brew.coffeeId)?.name ?: return
        val sieveName = sieveDao.getSieveById(brew.sieveId)?.name ?: return
        val drinkName = brew.drinkId?.let { drinkDao.getDrinkById(it)?.name }
        syncManager.updateBrewOnServer(
            server,
            brew.syncKey,
            SyncBrewDto(
                syncKey = brew.syncKey,
                coffeeName = coffeeName,
                sieveName = sieveName,
                drinkName = drinkName,
                temperature = brew.temperature,
                coffeeWeight = brew.coffeeWeight,
                targetYield = brew.targetYield,
                actualYield = brew.actualYield,
                tamperPressure = brew.tamperPressure,
                milkVolume = brew.milkVolume,
                grindSize = brew.grindSize,
                brewTime = brew.brewTime,
                timestamp = brew.timestamp,
                dataOnly = brew.dataOnly,
                source = brew.source.name
            )
        )
    }

    private suspend fun serverIfEnabled(): String? {
        val enabled = appPreferencesManager.syncEnabledFlow.first()
        if (!enabled) return null
        val server = appPreferencesManager.syncServerFlow.first()
        return server.takeIf { it.isNotBlank() }
    }
}
