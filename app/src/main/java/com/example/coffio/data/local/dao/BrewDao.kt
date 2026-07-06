package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewWithCoffee
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewDao {
    @Transaction
    @Query("SELECT * FROM brews WHERE dataOnly = 0 ORDER BY timestamp DESC")
    fun getAllBrewsWithCoffee(): Flow<List<BrewWithCoffee>>

    @Transaction
    @Query("SELECT * FROM brews ORDER BY timestamp DESC")
    fun getAllBrewsWithCoffeeIncludingDataOnly(): Flow<List<BrewWithCoffee>>

    @Query("SELECT * FROM brews WHERE dataOnly = 0 ORDER BY timestamp DESC")
    fun getAllBrews(): Flow<List<Brew>>

    @Query("SELECT * FROM brews ORDER BY timestamp DESC")
    fun getAllBrewsIncludingDataOnly(): Flow<List<Brew>>

    @Query("SELECT * FROM brews WHERE source != 'REMOTE' ORDER BY timestamp DESC")
    fun getAllBrewsForCalculations(): Flow<List<Brew>>

    @Query("SELECT * FROM brews WHERE source = 'LOCAL' ORDER BY timestamp DESC")
    fun getAllLocalBrewsForConsumption(): Flow<List<Brew>>

    @Query("SELECT * FROM brews")
    suspend fun getAllBrewsList(): List<Brew>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrew(brew: Brew): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrews(brews: List<Brew>)

    @Delete
    suspend fun deleteBrew(brew: Brew)

    @Query("DELETE FROM brews")
    suspend fun deleteAllBrews()

    @Query("SELECT * FROM brews WHERE coffeeId = :coffeeId AND sieveId = :sieveId ORDER BY timestamp DESC")
    suspend fun getBrewsByCoffeeAndSieve(coffeeId: Long, sieveId: Long): List<Brew>

    @Query("SELECT * FROM brews WHERE coffeeId = :coffeeId AND sieveId = :sieveId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastBrewByCoffeeAndSieve(coffeeId: Long, sieveId: Long): Brew?
}
