package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenCreateRequest
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenLoginResponse
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenGetResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AlmacenApi {

    @GET("almacenes")
    suspend fun getAlmacenesApi(): AlmacenGetResponse

    @GET("almacenes/login")
    suspend fun loginAlmacenApi(
        @Query("codigo") codigo: Int
    ): AlmacenLoginResponse

    @POST("almacenes/create")
    suspend fun createAlmacenApi(
        @Body data: AlmacenCreateRequest
    ): MessageResponse

    @DELETE("almacenes/delete")
    suspend fun deleteAlmacenApi(
        @Body data: AlmacenDeleteRequest
    ): MessageResponse
}