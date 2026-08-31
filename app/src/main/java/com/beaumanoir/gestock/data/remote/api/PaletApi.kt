package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.palet.PaletCreateRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletsGetRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletsGetResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PaletApi {

    @GET("palets")
    suspend fun getPaletsApi(
        @Query("almacen") almacen: Int
    ): PaletsGetResponse

    @POST("palets/create")
    suspend fun createPaletApi(
        @Body data: PaletCreateRequest
    ): MessageResponse

    @DELETE("palets/delete")
    suspend fun deletePaletApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int
    ): MessageResponse
}