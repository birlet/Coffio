package com.example.coffio.data.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncRequestDto(
    val deviceId: String,
    val coffees: List<SyncCoffeeDto>,
    val sieves: List<SyncSieveDto>,
    val drinks: List<SyncDrinkDto>,
    val brews: List<SyncBrewDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(
    val coffees: List<SyncCoffeeDto>,
    val sieves: List<SyncSieveDto>,
    val drinks: List<SyncDrinkDto>,
    val brews: List<SyncBrewDto>
)

@JsonClass(generateAdapter = true)
data class SyncCoffeeDto(
    val name: String
)

@JsonClass(generateAdapter = true)
data class SyncSieveDto(
    val name: String
)

@JsonClass(generateAdapter = true)
data class SyncDrinkDto(
    val name: String,
    val defaultSieveName: String? = null,
    val defaultCoffeeName: String? = null,
    val defaultTemperature: Double = 93.0,
    val defaultCoffeeWeight: Double = 18.0,
    val defaultTargetYield: Double = 36.0,
    val defaultGrindSize: Double = 2.0,
    val defaultDesiredTime: Double = 25.0,
    val defaultTamperPressure: Double = 15.0,
    val defaultMilkVolume: Double = 0.0,
    val isVisible: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SyncBrewDto(
    val syncKey: String? = null,
    val coffeeName: String,
    val sieveName: String,
    val drinkName: String? = null,
    val temperature: Double,
    val coffeeWeight: Double,
    val targetYield: Double,
    val actualYield: Double,
    val tamperPressure: Double,
    val milkVolume: Double,
    val grindSize: Double,
    val brewTime: Int,
    val timestamp: Long,
    val dataOnly: Boolean
)
