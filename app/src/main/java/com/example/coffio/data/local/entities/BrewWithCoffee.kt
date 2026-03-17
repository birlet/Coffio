package com.example.coffio.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class BrewWithCoffee(
    @Embedded val brew: Brew,
    @Relation(
        parentColumn = "coffeeId",
        entityColumn = "id"
    )
    val coffee: Coffee,
    @Relation(
        parentColumn = "sieveId",
        entityColumn = "id"
    )
    val sieve: Sieve,
    @Relation(
        parentColumn = "drinkId",
        entityColumn = "id"
    )
    val drink: Drink?
)
