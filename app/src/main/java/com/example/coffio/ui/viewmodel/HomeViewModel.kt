package com.example.coffio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.Drink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CoffioDatabase.getDatabase(application)
    private val drinkDao = database.drinkDao()

    val drinks: StateFlow<List<Drink>> = drinkDao.getVisibleDrinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
