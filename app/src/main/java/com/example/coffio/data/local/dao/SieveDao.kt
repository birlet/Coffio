package com.example.coffio.data.local.dao

import androidx.room.*
import com.example.coffio.data.local.entities.Sieve
import kotlinx.coroutines.flow.Flow

@Dao
interface SieveDao {
    @Query("SELECT * FROM sieves")
    fun getAllSieves(): Flow<List<Sieve>>

    @Query("SELECT * FROM sieves")
    suspend fun getAllSievesList(): List<Sieve>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSieve(sieve: Sieve): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSieves(sieves: List<Sieve>)

    @Delete
    suspend fun deleteSieve(sieve: Sieve)

    @Query("DELETE FROM sieves")
    suspend fun deleteAllSieves()
}
