package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.datastore.BrewPreferences
import com.example.coffio.data.local.datastore.BrewPreferencesManager
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Sieve
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrewingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val coffeeDao = database.coffeeDao()
    private val sieveDao = database.sieveDao()
    private val brewDao = database.brewDao()
    private val prefsManager = BrewPreferencesManager(application)

    val coffees: StateFlow<List<Coffee>> = coffeeDao.getAllCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sieves: StateFlow<List<Sieve>> = sieveDao.getAllSieves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCoffee by mutableStateOf<Coffee?>(null)
    var selectedSieve by mutableStateOf<Sieve?>(null)
    var temperature by mutableStateOf("")
    var coffeeWeight by mutableStateOf("")
    var targetYield by mutableStateOf("")
    var tamperPressure by mutableStateOf("")
    var milkVolume by mutableStateOf("")
    var grindSize by mutableStateOf("")
    var brewTime by mutableStateOf("")
    var actualYield by mutableStateOf("")

    init {
        loadLastPreferences()
    }

    private fun loadLastPreferences() {
        viewModelScope.launch {
            val prefs = prefsManager.brewPreferencesFlow.first()
            temperature = prefs.temperature.toString()
            coffeeWeight = prefs.coffeeWeight.toString()
            targetYield = prefs.targetYield.toString()
            tamperPressure = prefs.tamperPressure.toString()
            milkVolume = prefs.milkVolume.toString()
            grindSize = prefs.grindSize.toString()
            brewTime = prefs.brewTime.toString()

            // Try to match coffee and sieve from IDs
            val coffeeList = coffees.first { it.isNotEmpty() }
            selectedCoffee = coffeeList.find { it.id == prefs.coffeeId }

            val sieveList = sieves.first { it.isNotEmpty() }
            selectedSieve = sieveList.find { it.id == prefs.sieveId }
        }
    }

    fun addCoffee(name: String) {
        viewModelScope.launch {
            val id = coffeeDao.insertCoffee(Coffee(name = name))
            selectedCoffee = Coffee(id = id, name = name)
        }
    }

    fun addSieve(name: String) {
        viewModelScope.launch {
            val id = sieveDao.insertSieve(Sieve(name = name))
            selectedSieve = Sieve(id = id, name = name)
        }
    }

    fun saveBrew(onSuccess: () -> Unit) {
        val coffee = selectedCoffee ?: return
        val sieve = selectedSieve ?: return
        
        val temp = temperature.toDoubleOrNull() ?: 0.0
        val weight = coffeeWeight.toDoubleOrNull() ?: 0.0
        val target = targetYield.toDoubleOrNull() ?: 0.0
        val pressure = tamperPressure.toDoubleOrNull() ?: 0.0
        val milk = milkVolume.toDoubleOrNull() ?: 0.0
        val grind = grindSize.toDoubleOrNull() ?: 0.0
        val time = brewTime.toIntOrNull() ?: 0
        val actual = actualYield.toDoubleOrNull() ?: target

        viewModelScope.launch {
            val brew = Brew(
                coffeeId = coffee.id,
                sieveId = sieve.id,
                temperature = temp,
                coffeeWeight = weight,
                targetYield = target,
                actualYield = actual,
                tamperPressure = pressure,
                milkVolume = milk,
                grindSize = grind,
                brewTime = time
            )
            brewDao.insertBrew(brew)
            
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
                    brewTime = time
                )
            )
            onSuccess()
        }
    }
}
