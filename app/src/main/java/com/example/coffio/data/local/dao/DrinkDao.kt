package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Drink
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks")
    fun getAllDrinks(): Flow<List<Drink>>

    @Query("SELECT * FROM drinks WHERE isVisible = 1")
    fun getVisibleDrinks(): Flow<List<Drink>>

    @Query("SELECT * FROM drinks")
    suspend fun getAllDrinksList(): List<Drink>

    @Query("SELECT * FROM drinks WHERE name = :name LIMIT 1")
    suspend fun getDrinkByName(name: String): Drink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrink(drink: Drink)

    @Update
    suspend fun updateDrink(drink: Drink)

    @Delete
    suspend fun deleteDrink(drink: Drink)

    @Query("DELETE FROM drinks")
    suspend fun deleteAllDrinks()

    @Query("SELECT * FROM drinks WHERE id = :id")
    suspend fun getDrinkById(id: Long): Drink?
}
