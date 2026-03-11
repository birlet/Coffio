package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.BrewWithCoffee
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewDao {
    @Transaction
    @Query("SELECT * FROM brews ORDER BY timestamp DESC")
    fun getAllBrewsWithCoffee(): Flow<List<BrewWithCoffee>>

    @Query("SELECT * FROM brews ORDER BY timestamp DESC")
    fun getAllBrews(): Flow<List<Brew>>

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
}
