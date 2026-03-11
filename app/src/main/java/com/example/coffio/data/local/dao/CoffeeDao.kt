package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Coffee
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Query("SELECT * FROM coffees")
    fun getAllCoffees(): Flow<List<Coffee>>

    @Query("SELECT * FROM coffees")
    suspend fun getAllCoffeesList(): List<Coffee>

    @Query("SELECT * FROM coffees WHERE name = :name LIMIT 1")
    suspend fun getCoffeeByName(name: String): Coffee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffee(coffee: Coffee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffees(coffees: List<Coffee>)

    @Delete
    suspend fun deleteCoffee(coffee: Coffee)

    @Query("DELETE FROM coffees")
    suspend fun deleteAllCoffees()
}
