package com.example.coffio.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drinks")
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultSieveId: Long? = null,
    val defaultCoffeeId: Long? = null,
    val defaultTemperature: Double = 93.0,
    val defaultCoffeeWeight: Double = 18.0,
    val defaultTargetYield: Double = 36.0,
    val defaultGrindSize: Double = 2.0,
    val defaultTamperPressure: Double = 15.0,
    val defaultMilkVolume: Double = 0.0,
    val isVisible: Boolean = true
)
