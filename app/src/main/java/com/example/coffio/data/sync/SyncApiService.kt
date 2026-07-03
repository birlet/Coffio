package com.example.coffio.data.sync

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SyncApiService {
    @POST("api/v1/sync")
    suspend fun sync(@Body request: SyncRequestDto): SyncResponseDto

    @DELETE("api/v1/data")
    suspend fun deleteAllData(): Map<String, String>

    @DELETE("api/v1/brews/{syncKey}")
    suspend fun deleteBrew(@Path("syncKey") syncKey: String): Map<String, String>

    @PUT("api/v1/brews/{syncKey}")
    suspend fun updateBrew(@Path("syncKey") syncKey: String, @Body brew: SyncBrewDto): Map<String, String>
}
