package com.example.coffio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.coffio.data.local.dao.BrewDao
import com.example.coffio.data.local.dao.CoffeeDao
import com.example.coffio.data.local.dao.DrinkDao
import com.example.coffio.data.local.dao.SieveDao
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.local.entities.Sieve

@Database(entities = [Coffee::class, Sieve::class, Brew::class, Drink::class], version = 11, exportSchema = false)
@TypeConverters(BrewSourceConverters::class)
abstract class CoffioDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun sieveDao(): SieveDao
    abstract fun brewDao(): BrewDao
    abstract fun drinkDao(): DrinkDao

    companion object {
        @Volatile
        private var INSTANCE: CoffioDatabase? = null

        fun getDatabase(context: Context): CoffioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CoffioDatabase::class.java,
                    "coffio_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
