package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockAddRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockMoveRequest
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StockApi {

    @GET("stock")
    suspend fun getStockApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): StockGetResponse

    @POST("stock/add")
    suspend fun addStockApi(
        data: StockAddRequest
    ): MessageResponse

    @DELETE("stock/delete")
    suspend fun deleteStockApi(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int,
        @Query("ids") ids: List<Int>
    ): MessageResponse

    @POST("stock/move")
    suspend fun moveStockApi(
        data: StockMoveRequest
    ): MessageResponse

    @GET("stock/export")
    suspend fun exportStockApi(
        @Query("almacen") almacen: Int,
        @Query("ean") ean: String?,
        @Query("talla") talla: String?,
        @Query("nombre") nombre: String?,
        @Query("familia") familia: String?,
        @Query("color") color: String?,
        @Query("temporada") temporada: String?
    ): StockExportResponse

}