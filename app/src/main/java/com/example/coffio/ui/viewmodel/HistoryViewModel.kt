package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewWithCoffee
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val brewDao = CoffioDatabase.getDatabase(application).brewDao()

    val historyState: StateFlow<List<BrewWithCoffee>> = brewDao.getAllBrewsWithCoffeeIncludingDataOnly()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteBrew(brew: Brew) {
        viewModelScope.launch {
            brewDao.deleteBrew(brew)
        }
    }

    fun updateBrew(brew: Brew) {
        viewModelScope.launch {
            brewDao.insertBrew(brew) // Room with OnConflictStrategy.REPLACE acts as update if ID exists
        }
    }
}
