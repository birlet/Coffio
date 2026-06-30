package com.example.coffio.data.sync

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface SyncApiService {
    @POST("api/v1/sync")
    suspend fun sync(@Body request: SyncRequestDto): SyncResponseDto

    @DELETE("api/v1/data")
    suspend fun deleteAllData(): Map<String, String>
}
