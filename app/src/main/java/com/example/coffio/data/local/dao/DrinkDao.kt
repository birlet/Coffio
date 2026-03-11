package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Drink
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks")
    fun getAllDrinks(): Flow<List<Drink>>

    @Query("SELECT * FROM drinks")
    suspend fun getAllDrinksList(): List<Drink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrink(drink: Drink)

    @Update
    suspend fun updateDrink(drink: Drink)

    @Delete
    suspend fun deleteDrink(drink: Drink)

    @Query("SELECT * FROM drinks WHERE id = :id")
    suspend fun getDrinkById(id: Long): Drink?
}
