package com.example.coffio.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class BrewSource {
    LOCAL,
    REMOTE,
    IMPORTED
}

@Entity(
    tableName = "brews",
    indices = [Index(value = ["syncKey"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Coffee::class,
            parentColumns = ["id"],
            childColumns = ["coffeeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Sieve::class,
            parentColumns = ["id"],
            childColumns = ["sieveId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Drink::class,
            parentColumns = ["id"],
            childColumns = ["drinkId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Brew(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coffeeId: Long,
    val sieveId: Long,
    val drinkId: Long? = null,
    val temperature: Double,
    val coffeeWeight: Double,
    val targetYield: Double,
    val actualYield: Double,
    val tamperPressure: Double,
    val milkVolume: Double,
    val grindSize: Double = 0.0,
    val brewTime: Int = 0, // in seconds
    val timestamp: Long = System.currentTimeMillis(),
    val dataOnly: Boolean = false,
    val source: BrewSource = BrewSource.LOCAL,
    val syncKey: String = UUID.randomUUID().toString()
)
