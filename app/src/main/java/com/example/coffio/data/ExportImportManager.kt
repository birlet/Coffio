package com.example.coffio.data

import android.content.Context
import android.net.Uri
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Sieve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ExportImportManager(private val context: Context) {
    private val database = CoffioDatabase.getDatabase(context)
    private val brewDao = database.brewDao()
    private val coffeeDao = database.coffeeDao()
    private val sieveDao = database.sieveDao()

    suspend fun exportDatabaseToCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Export Coffees
                    writer.write("TABLE:COFFEES\n")
                    writer.write("id,name\n")
                    coffeeDao.getAllCoffeesList().forEach {
                        writer.write("${it.id},${escapeCsv(it.name)}\n")
                    }

                    // Export Sieves
                    writer.write("\nTABLE:SIEVES\n")
                    writer.write("id,name\n")
                    sieveDao.getAllSievesList().forEach {
                        writer.write("${it.id},${escapeCsv(it.name)}\n")
                    }

                    // Export Brews
                    writer.write("\nTABLE:BREWS\n")
                    writer.write("id,coffeeId,sieveId,temperature,coffeeWeight,targetYield,actualYield,tamperPressure,milkVolume,grindSize,brewTime,timestamp\n")
                    brewDao.getAllBrewsList().forEach {
                        writer.write("${it.id},${it.coffeeId},${it.sieveId},${it.temperature},${it.coffeeWeight},${it.targetYield},${it.actualYield},${it.tamperPressure},${it.milkVolume},${it.grindSize},${it.brewTime},${it.timestamp}\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDatabaseFromCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var currentTable = ""
                    val coffees = mutableListOf<Coffee>()
                    val sieves = mutableListOf<Sieve>()
                    val brews = mutableListOf<Brew>()

                    var line = reader.readLine()
                    while (line != null) {
                        if (line.isBlank()) {
                            line = reader.readLine()
                            continue
                        }

                        if (line.startsWith("TABLE:")) {
                            currentTable = line.substringAfter("TABLE:")
                            reader.readLine() // Skip header
                        } else {
                            val parts = line.split(",")
                            when (currentTable) {
                                "COFFEES" -> {
                                    if (parts.size >= 2) {
                                        coffees.add(Coffee(id = parts[0].toLong(), name = unescapeCsv(parts[1])))
                                    }
                                }
                                "SIEVES" -> {
                                    if (parts.size >= 2) {
                                        sieves.add(Sieve(id = parts[0].toLong(), name = unescapeCsv(parts[1])))
                                    }
                                }
                                "BREWS" -> {
                                    if (parts.size >= 12) {
                                        brews.add(Brew(
                                            id = parts[0].toLong(),
                                            coffeeId = parts[1].toLong(),
                                            sieveId = parts[2].toLong(),
                                            temperature = parts[3].toDouble(),
                                            coffeeWeight = parts[4].toDouble(),
                                            targetYield = parts[5].toDouble(),
                                            actualYield = parts[6].toDouble(),
                                            tamperPressure = parts[7].toDouble(),
                                            milkVolume = parts[8].toDouble(),
                                            grindSize = parts[9].toDouble(),
                                            brewTime = parts[10].toInt(),
                                            timestamp = parts[11].toLong()
                                        ))
                                    }
                                }
                            }
                        }
                        line = reader.readLine()
                    }

                    // Atomic-ish update
                    if (coffees.isNotEmpty() || sieves.isNotEmpty() || brews.isNotEmpty()) {
                        database.runInTransaction {
                            // Optionally clear existing data or just merge. 
                            // For a full "Import", usually we want to clear or replace.
                            // I'll stick to a simple replace for now if IDs match, but clearing is safer for a full restore.
                            // However, let's just insert with REPLACE strategy which is already in DAOs.
                        }
                        coffeeDao.insertCoffees(coffees)
                        sieveDao.insertSieves(sieves)
                        brewDao.insertBrews(brews)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun unescapeCsv(value: String): String {
        var result = value.trim()
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1).replace("\"\"", "\"")
        }
        return result
    }
}
