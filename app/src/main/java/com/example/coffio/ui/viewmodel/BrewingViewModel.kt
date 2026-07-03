package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.datastore.AppPreferencesManager
import com.example.coffio.data.local.datastore.BrewPreferences
import com.example.coffio.data.local.datastore.BrewPreferencesManager
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Sieve
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.model.GrindSizeModel
import com.example.coffio.data.sync.SyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BrewingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val coffeeDao = database.coffeeDao()
    private val sieveDao = database.sieveDao()
    private val brewDao = database.brewDao()
    private val drinkDao = database.drinkDao()
    private val prefsManager = BrewPreferencesManager(application)
    private val appPreferencesManager = AppPreferencesManager(application)
    private val syncManager = SyncManager(application)

    val coffees: StateFlow<List<Coffee>> = coffeeDao.getAllCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sieves: StateFlow<List<Sieve>> = sieveDao.getAllSieves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCoffee by mutableStateOf<Coffee?>(null)
    var selectedSieve by mutableStateOf<Sieve?>(null)
    var selectedDrink by mutableStateOf<Drink?>(null)
    var lastBrew by mutableStateOf<Brew?>(null)
        private set
    
    var temperature by mutableStateOf("")
    var coffeeWeight by mutableStateOf("")
    var targetYield by mutableStateOf("")
    var tamperPressure by mutableStateOf("")
    var milkVolume by mutableStateOf("")
    var grindSize by mutableStateOf("")
    var desiredBrewTime by mutableStateOf("")
    var resultBrewTime by mutableStateOf("")
    var actualYield by mutableStateOf("")

    var calculatedGrindSize by mutableStateOf<String?>(null)
        private set
    private val grindSizeModel = GrindSizeModel()
    private var isSaving = false

    var brewParamsExpanded by mutableStateOf(false)
        private set
    var lastBrewExpanded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            brewParamsExpanded = prefsManager.brewParamsExpandedFlow.first()
            lastBrewExpanded = prefsManager.lastBrewExpandedFlow.first()
        }
    }

    fun toggleBrewParamsExpanded() {
        brewParamsExpanded = !brewParamsExpanded
        viewModelScope.launch { prefsManager.setBrewParamsExpanded(brewParamsExpanded) }
    }

    fun toggleLastBrewExpanded() {
        lastBrewExpanded = !lastBrewExpanded
        viewModelScope.launch { prefsManager.setLastBrewExpanded(lastBrewExpanded) }
    }

    fun onCoffeeSelected(coffee: Coffee?) {
        selectedCoffee = coffee
        persistDrinkSelection()
        refreshSelectionData()
    }

    fun onSieveSelected(sieve: Sieve?) {
        selectedSieve = sieve
        persistDrinkSelection()
        refreshSelectionData()
    }

    private fun persistDrinkSelection() {
        val drink = selectedDrink ?: return
        viewModelScope.launch {
            val updated = drink.copy(
                defaultCoffeeId = selectedCoffee?.id ?: drink.defaultCoffeeId,
                defaultSieveId = selectedSieve?.id ?: drink.defaultSieveId,
                lastModified = System.currentTimeMillis()
            )
            drinkDao.updateDrink(updated)
            selectedDrink = updated
        }
    }

    private fun refreshSelectionData() {
        updateCalculatedGrindSize()
        loadLastBrewForSelection()
    }

    private fun loadLastBrewForSelection() {
        val coffee = selectedCoffee
        val sieve = selectedSieve

        if (coffee == null || sieve == null) {
            lastBrew = null
            return
        }

        viewModelScope.launch {
            lastBrew = brewDao.getLastBrewByCoffeeAndSieve(coffee.id, sieve.id)
        }
    }

    fun updateCalculatedGrindSize() {
        val coffee = selectedCoffee ?: return run { calculatedGrindSize = null }
        val sieve = selectedSieve ?: return run { calculatedGrindSize = null }
        val target = targetYield.toDoubleOrNull() ?: return run { calculatedGrindSize = null }
        val desiredTime = desiredBrewTime.toDoubleOrNull() ?: return run { calculatedGrindSize = null }

        viewModelScope.launch {
            val brews = brewDao.getBrewsByCoffeeAndSieve(coffee.id, sieve.id)
            if (grindSizeModel.fit(brews)) {
                val predicted = grindSizeModel.predict(target, desiredTime)
                calculatedGrindSize = predicted?.let {
                    String.format(java.util.Locale.US, "%.1f", it)
                }
            } else {
                calculatedGrindSize = null
            }
        }
    }

    fun initialize(drinkId: Long) {
        viewModelScope.launch {
            if (drinkId != -1L) {
                val drink = drinkDao.getDrinkById(drinkId)
                selectedDrink = drink
                drink?.let { applyDrinkDefaults(it) }
            }
            loadLastPreferences()
            refreshSelectionData()
        }
    }

    private suspend fun applyDrinkDefaults(drink: Drink) {
        temperature = drink.defaultTemperature.toString()
        coffeeWeight = drink.defaultCoffeeWeight.toString()
        targetYield = drink.defaultTargetYield.toString()
        grindSize = drink.defaultGrindSize.toString()
        desiredBrewTime = drink.defaultDesiredTime.toString()
        tamperPressure = drink.defaultTamperPressure.toString()
        milkVolume = drink.defaultMilkVolume.toString()

        resultBrewTime = drink.defaultDesiredTime.toInt().toString()
        actualYield = drink.defaultTargetYield.toInt().toString()

        // Pre-select coffee if defined for the drink
        if (drink.defaultCoffeeId != null) {
            val coffeeList = coffees.first { list -> list.isNotEmpty() }
            selectedCoffee = coffeeList.find { c -> c.id == drink.defaultCoffeeId }
        }

        // Pre-select sieve if defined for the drink
        if (drink.defaultSieveId != null) {
            val sieveList = sieves.first { list -> list.isNotEmpty() }
            selectedSieve = sieveList.find { s -> s.id == drink.defaultSieveId }
        }
    }

    /** Re-check DB for newer version of same drink. If lastModified is newer, apply those values. */
    fun refreshDrinkFromDb() {
        val drink = selectedDrink ?: return
        viewModelScope.launch {
            val dbDrink = drinkDao.getDrinkById(drink.id) ?: return@launch
            if (dbDrink.lastModified > drink.lastModified) {
                selectedDrink = dbDrink
                applyDrinkDefaults(dbDrink)
                refreshSelectionData()
            }
        }
    }

    private suspend fun loadLastPreferences() {
        val prefs = prefsManager.brewPreferencesFlow.first()
        
        // Only load prefs if they weren't set by the drink defaults
        if (temperature.isEmpty()) temperature = prefs.temperature.toString()
        if (coffeeWeight.isEmpty()) coffeeWeight = prefs.coffeeWeight.toString()
        if (targetYield.isEmpty()) targetYield = prefs.targetYield.toString()
        if (tamperPressure.isEmpty()) tamperPressure = prefs.tamperPressure.toString()
        if (milkVolume.isEmpty()) milkVolume = prefs.milkVolume.toString()
        if (grindSize.isEmpty()) grindSize = prefs.grindSize.toString()
        if (desiredBrewTime.isEmpty()) desiredBrewTime = prefs.brewTime.toString()

        // Try to match coffee from IDs if not already set
        if (selectedCoffee == null) {
            val coffeeList = coffees.first { it.isNotEmpty() }
            selectedCoffee = coffeeList.find { it.id == prefs.coffeeId }
        }

        // Try to match sieve from IDs if not already set by drink default
        if (selectedSieve == null) {
            val sieveList = sieves.first { it.isNotEmpty() }
            selectedSieve = sieveList.find { it.id == prefs.sieveId }
        }
    }

    fun addCoffee(name: String) {
        viewModelScope.launch {
            val id = coffeeDao.insertCoffee(Coffee(name = name))
            onCoffeeSelected(Coffee(id = id, name = name))
        }
    }

    fun addSieve(name: String) {
        viewModelScope.launch {
            val id = sieveDao.insertSieve(Sieve(name = name))
            onSieveSelected(Sieve(id = id, name = name))
        }
    }

    fun saveBrew(onSuccess: () -> Unit, dataOnly: Boolean = false) {
        if (isSaving) return
        val coffee = selectedCoffee ?: return
        val sieve = selectedSieve ?: return
        isSaving = true
        
        val temp = temperature.toDoubleOrNull() ?: 0.0
        val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
        val target = targetYield.toDoubleOrNull() ?: 0.0
        val pressure = tamperPressure.toDoubleOrNull() ?: 0.0
        val milk = milkVolume.toDoubleOrNull() ?: 0.0
        val grind = grindSize.toDoubleOrNull() ?: 0.0
        val time = resultBrewTime.toIntOrNull() ?: desiredBrewTime.toIntOrNull() ?: 0
        val actual = actualYield.toDoubleOrNull() ?: target

        viewModelScope.launch {
            val brew = Brew(
                coffeeId = coffee.id,
                sieveId = sieve.id,
                drinkId = selectedDrink?.id,
                temperature = temp,
                coffeeWeight = weight,
                targetYield = target,
                actualYield = actual,
                tamperPressure = pressure,
                milkVolume = milk,
                grindSize = grind,
                brewTime = time,
                dataOnly = dataOnly,
                syncKey = UUID.randomUUID().toString()
            )
            brewDao.insertBrew(brew)
            lastBrew = brew
            
            // Save preferences
            prefsManager.saveBrewPreferences(
                BrewPreferences(
                    coffeeId = coffee.id,
                    sieveId = sieve.id,
                    temperature = temp,
                    coffeeWeight = weight,
                    targetYield = target,
                    tamperPressure = pressure,
                    milkVolume = milk,
                    grindSize = grind,
                    brewTime = desiredBrewTime.toIntOrNull() ?: time
                )
            )

            val autoSyncEnabled = appPreferencesManager.syncEnabledFlow.first()
            val syncServer = appPreferencesManager.syncServerFlow.first()
            if (autoSyncEnabled && syncServer.isNotBlank()) {
                syncManager.sync(syncServer)
            }

            onSuccess()
        }
    }
}
