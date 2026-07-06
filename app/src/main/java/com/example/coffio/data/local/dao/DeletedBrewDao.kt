package com.example.coffio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coffio.data.local.entities.DeletedBrew

@Dao
interface DeletedBrewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletedBrew: DeletedBrew)

    @Query("SELECT syncKey FROM deleted_brews")
    suspend fun getAllSyncKeys(): List<String>

    @Query("DELETE FROM deleted_brews")
    suspend fun deleteAll()
}
