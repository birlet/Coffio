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
import java.util.Calendar

data class ConsumptionData(
    val today: Double = 0.0,
    val thisWeek: Double = 0.0,
    val thisMonth: Double = 0.0
)

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

    private val _allBrews = MutableStateFlow<List<Brew>>(emptyList())

    val consumption: StateFlow<ConsumptionData> = _allBrews.combine(
        MutableStateFlow(selectedCoffee)
    ) { allBrews, _ ->
        computeConsumption(allBrews, selectedCoffee)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConsumptionData())

    // Map of sieveId to list of brews for the selected coffee
    val brewsBySieve: StateFlow<Map<Long, List<Brew>>> = combine(_brews, sieves) { brews, _ ->
        brews.groupBy { it.sieveId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            brewDao.getAllBrews().collectLatest { brews ->
                _allBrews.value = brews
                updateConsumption()
            }
        }
    }

    fun onCoffeeSelected(coffee: Coffee) {
        selectedCoffee = coffee
        updateConsumption()
        viewModelScope.launch {
            brewDao.getAllBrews().collectLatest { allBrews ->
                _brews.value = allBrews.filter { it.coffeeId == coffee.id }
                    .sortedBy { it.timestamp }
            }
        }
    }

    private val _consumptionState = MutableStateFlow(ConsumptionData())
    val consumptionState: StateFlow<ConsumptionData> = _consumptionState

    private fun updateConsumption() {
        _consumptionState.value = computeConsumption(_allBrews.value, selectedCoffee)
    }

    private fun computeConsumption(allBrews: List<Brew>, coffee: Coffee?): ConsumptionData {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeek = cal.timeInMillis

        cal.time = java.util.Date(now)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        val filtered = if (coffee != null) {
            allBrews.filter { it.coffeeId == coffee.id }
        } else {
            allBrews
        }

        return ConsumptionData(
            today = filtered.filter { it.timestamp >= startOfDay }.sumOf { it.coffeeWeight },
            thisWeek = filtered.filter { it.timestamp >= startOfWeek }.sumOf { it.coffeeWeight },
            thisMonth = filtered.filter { it.timestamp >= startOfMonth }.sumOf { it.coffeeWeight }
        )
    }
}
