package com.example.coffio.data.sync

import android.content.Context
import android.provider.Settings
import com.example.coffio.data.local.CoffioDatabase
import com.example.coffio.data.local.entities.Brew
import com.example.coffio.data.local.entities.Coffee
import com.example.coffio.data.local.entities.Drink
import com.example.coffio.data.local.entities.Sieve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest
import java.util.Locale

class SyncManager(private val context: Context) {
    private val database = CoffioDatabase.getDatabase(context)
    private val brewDao = database.brewDao()
    private val coffeeDao = database.coffeeDao()
    private val sieveDao = database.sieveDao()
    private val drinkDao = database.drinkDao()

    suspend fun sync(serverInput: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeServerBaseUrl(serverInput)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid server address"))

            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(SyncApiService::class.java)

            val localCoffees = coffeeDao.getAllCoffeesList()
            val localSieves = sieveDao.getAllSievesList()
            val localDrinks = drinkDao.getAllDrinksList()
            val localBrews = brewDao.getAllBrewsList()

            val coffeeNameById = localCoffees.associateBy({ it.id }, { it.name })
            val sieveNameById = localSieves.associateBy({ it.id }, { it.name })
            val drinkNameById = localDrinks.associateBy({ it.id }, { it.name })

            val response = api.sync(
                SyncRequestDto(
                    deviceId = deviceId(),
                    coffees = localCoffees.map { SyncCoffeeDto(name = it.name) },
                    sieves = localSieves.map { SyncSieveDto(name = it.name) },
                    drinks = localDrinks.map { drink ->
                        SyncDrinkDto(
                            name = drink.name,
                            defaultSieveName = drink.defaultSieveId?.let { sieveNameById[it] },
                            defaultCoffeeName = drink.defaultCoffeeId?.let { coffeeNameById[it] },
                            defaultTemperature = drink.defaultTemperature,
                            defaultCoffeeWeight = drink.defaultCoffeeWeight,
                            defaultTargetYield = drink.defaultTargetYield,
                            defaultGrindSize = drink.defaultGrindSize,
                            defaultDesiredTime = drink.defaultDesiredTime,
                            defaultTamperPressure = drink.defaultTamperPressure,
                            defaultMilkVolume = drink.defaultMilkVolume,
                            isVisible = drink.isVisible
                        )
                    },
                    brews = localBrews.mapNotNull { brew ->
                        val coffeeName = coffeeNameById[brew.coffeeId] ?: return@mapNotNull null
                        val sieveName = sieveNameById[brew.sieveId] ?: return@mapNotNull null
                        SyncBrewDto(
                            syncKey = brew.syncKey,
                            coffeeName = coffeeName,
                            sieveName = sieveName,
                            drinkName = brew.drinkId?.let { drinkNameById[it] },
                            temperature = brew.temperature,
                            coffeeWeight = brew.coffeeWeight,
                            targetYield = brew.targetYield,
                            actualYield = brew.actualYield,
                            tamperPressure = brew.tamperPressure,
                            milkVolume = brew.milkVolume,
                            grindSize = brew.grindSize,
                            brewTime = brew.brewTime,
                            timestamp = brew.timestamp,
                            dataOnly = brew.dataOnly
                        )
                    }
                )
            )

            // Upsert lookup tables by name.
            response.coffees.forEach { dto ->
                if (dto.name.isNotBlank() && coffeeDao.getCoffeeByName(dto.name) == null) {
                    coffeeDao.insertCoffee(Coffee(name = dto.name))
                }
            }

            response.sieves.forEach { dto ->
                if (dto.name.isNotBlank() && sieveDao.getSieveByName(dto.name) == null) {
                    sieveDao.insertSieve(Sieve(name = dto.name))
                }
            }

            response.drinks.forEach { dto ->
                if (dto.name.isBlank()) {
                    return@forEach
                }

                val defaultCoffeeId = dto.defaultCoffeeName?.let { coffeeDao.getCoffeeByName(it)?.id }
                val defaultSieveId = dto.defaultSieveName?.let { sieveDao.getSieveByName(it)?.id }
                val existing = drinkDao.getDrinkByName(dto.name)

                val toSave = Drink(
                    id = existing?.id ?: 0,
                    name = dto.name,
                    defaultSieveId = defaultSieveId,
                    defaultCoffeeId = defaultCoffeeId,
                    defaultTemperature = dto.defaultTemperature,
                    defaultCoffeeWeight = dto.defaultCoffeeWeight,
                    defaultTargetYield = dto.defaultTargetYield,
                    defaultGrindSize = dto.defaultGrindSize,
                    defaultDesiredTime = dto.defaultDesiredTime,
                    defaultTamperPressure = dto.defaultTamperPressure,
                    defaultMilkVolume = dto.defaultMilkVolume,
                    isVisible = dto.isVisible
                )

                drinkDao.insertDrink(toSave)
            }

            val syncedCoffees = coffeeDao.getAllCoffeesList()
            val syncedSieves = sieveDao.getAllSievesList()
            val syncedDrinks = drinkDao.getAllDrinksList()
            val syncedBrews = brewDao.getAllBrewsList()

            val syncedCoffeeIdByName = syncedCoffees.associateBy({ it.name }, { it.id })
            val syncedSieveIdByName = syncedSieves.associateBy({ it.name }, { it.id })
            val syncedDrinkIdByName = syncedDrinks.associateBy({ it.name }, { it.id })
            val syncedCoffeeNameById = syncedCoffees.associateBy({ it.id }, { it.name })
            val syncedSieveNameById = syncedSieves.associateBy({ it.id }, { it.name })
            val syncedDrinkNameById = syncedDrinks.associateBy({ it.id }, { it.name })
            val localSyncKeys = syncedBrews.map { it.syncKey }.filter { it.isNotBlank() }.toHashSet()

            val localSignatures = syncedBrews.map { brew ->
                val coffeeName = syncedCoffeeNameById[brew.coffeeId].orEmpty()
                val sieveName = syncedSieveNameById[brew.sieveId].orEmpty()
                val drinkName = brew.drinkId?.let { id -> syncedDrinkNameById[id] }
                brewSignature(
                    SyncBrewDto(
                        coffeeName = coffeeName,
                        sieveName = sieveName,
                        drinkName = drinkName,
                        temperature = brew.temperature,
                        coffeeWeight = brew.coffeeWeight,
                        targetYield = brew.targetYield,
                        actualYield = brew.actualYield,
                        tamperPressure = brew.tamperPressure,
                        milkVolume = brew.milkVolume,
                        grindSize = brew.grindSize,
                        brewTime = brew.brewTime,
                        timestamp = brew.timestamp,
                        dataOnly = brew.dataOnly
                    )
                )
            }.toHashSet()

            var inserted = 0
            response.brews.forEach { dto ->
                val syncKey = dto.syncKey?.takeIf { it.isNotBlank() } ?: brewSignature(dto)
                if (localSyncKeys.contains(syncKey)) {
                    return@forEach
                }

                val sig = brewSignature(dto)
                if (localSignatures.contains(sig)) {
                    return@forEach
                }

                val coffeeId = syncedCoffeeIdByName[dto.coffeeName] ?: return@forEach
                val sieveId = syncedSieveIdByName[dto.sieveName] ?: return@forEach
                val drinkId = dto.drinkName?.let { syncedDrinkIdByName[it] }

                brewDao.insertBrew(
                    Brew(
                        coffeeId = coffeeId,
                        sieveId = sieveId,
                        drinkId = drinkId,
                        temperature = dto.temperature,
                        coffeeWeight = dto.coffeeWeight,
                        targetYield = dto.targetYield,
                        actualYield = dto.actualYield,
                        tamperPressure = dto.tamperPressure,
                        milkVolume = dto.milkVolume,
                        grindSize = dto.grindSize,
                        brewTime = dto.brewTime,
                        timestamp = dto.timestamp,
                        dataOnly = dto.dataOnly,
                        syncKey = syncKey
                    )
                )
                localSyncKeys.add(syncKey)
                localSignatures.add(sig)
                inserted += 1
            }

            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearServerDb(serverInput: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeServerBaseUrl(serverInput)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid server address"))

            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(SyncApiService::class.java)

            api.deleteAllData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun normalizeServerBaseUrl(serverInput: String): String? {
        val trimmed = serverInput.trim().removeSuffix("/")
        if (trimmed.isBlank()) {
            return null
        }

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            if (":" in trimmed) {
                "http://$trimmed"
            } else {
                "http://$trimmed:8000"
            }
        }

        val candidate = "$withScheme/"
        return if (candidate.toHttpUrlOrNull() != null) candidate else null
    }

    private fun brewSignature(dto: SyncBrewDto): String {
        val raw = listOf(
            dto.coffeeName,
            dto.sieveName,
            dto.drinkName ?: "",
            String.format(Locale.US, "%.5f", dto.temperature),
            String.format(Locale.US, "%.5f", dto.coffeeWeight),
            String.format(Locale.US, "%.5f", dto.targetYield),
            String.format(Locale.US, "%.5f", dto.actualYield),
            String.format(Locale.US, "%.5f", dto.tamperPressure),
            String.format(Locale.US, "%.5f", dto.milkVolume),
            String.format(Locale.US, "%.5f", dto.grindSize),
            dto.brewTime.toString(),
            dto.timestamp.toString(),
            if (dto.dataOnly) "1" else "0"
        ).joinToString("|")

        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
