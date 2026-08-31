package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.caja.CajaCreateRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaGetDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaGetDescTempResponse
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateCantidadRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajasGetRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajasGetResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CajaApi {

    @GET("cajas")
    suspend fun getCajasApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int
    ): CajasGetResponse

    @POST("cajas/create")
    suspend fun createCajaApi(
        @Body data: CajaCreateRequest
    ): MessageResponse

    @DELETE("cajas/delete")
    suspend fun deleteCajaApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): MessageResponse

    @GET("get-desc-temp")
    suspend fun getDescTempCajaApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): CajaGetDescTempResponse

    @POST("cajas/update-desc-temp")
    suspend fun updateDescTempCajaApi(
        @Body data: CajaUpdateDescTempRequest
    ): MessageResponse

    @POST("cajas/update-cantidad")
    suspend fun updateCantidadCajaApi(
        @Body data: CajaUpdateCantidadRequest
    ): MessageResponse



}