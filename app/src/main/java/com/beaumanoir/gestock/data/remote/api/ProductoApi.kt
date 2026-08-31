package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.producto.ProductoGetResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductoApi {

    @GET("producto")
    suspend fun getProductoValuesApi(
        @Query("ean") ean : String,
        @Query("codemag") codemag : Int
    ): ProductoGetResponse

}