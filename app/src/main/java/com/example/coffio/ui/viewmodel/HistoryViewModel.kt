package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.BrewWithCoffee
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val brewDao = CoffioDatabase.getDatabase(application).brewDao()

    val historyState: StateFlow<List<BrewWithCoffee>> = brewDao.getAllBrewsWithCoffee()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
