package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Sieve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChartsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val coffeeDao = database.coffeeDao()
    private val brewDao = database.brewDao()
    private val sieveDao = database.sieveDao()

    val coffees: StateFlow<List<Coffee>> = coffeeDao.getAllCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sieves: StateFlow<List<Sieve>> = sieveDao.getAllSieves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCoffee by mutableStateOf<Coffee?>(null)
    
    private val _brews = MutableStateFlow<List<Brew>>(emptyList())
    val brews: StateFlow<List<Brew>> = _brews

    // Map of sieveId to list of brews for the selected coffee
    val brewsBySieve: StateFlow<Map<Long, List<Brew>>> = combine(_brews, sieves) { brews, _ ->
        brews.groupBy { it.sieveId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onCoffeeSelected(coffee: Coffee) {
        selectedCoffee = coffee
        viewModelScope.launch {
            brewDao.getAllBrews().collectLatest { allBrews ->
                _brews.value = allBrews.filter { it.coffeeId == coffee.id }
                    .sortedBy { it.timestamp }
            }
        }
    }
}
