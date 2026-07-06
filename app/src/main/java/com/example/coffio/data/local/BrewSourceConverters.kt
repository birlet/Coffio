package com.example.coffio.data.local

import androidx.room.TypeConverter
import com.example.coffio.data.local.entities.BrewSource

class BrewSourceConverters {
    @TypeConverter
    fun fromBrewSource(value: BrewSource): String = value.name

    @TypeConverter
    fun toBrewSource(value: String): BrewSource = BrewSource.valueOf(value)
}