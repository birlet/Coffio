package com.example.coffio.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sieves")
data class Sieve(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
