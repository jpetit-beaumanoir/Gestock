package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.ProductoApi
import com.beaumanoir.gestock.data.remote.dto.producto.ProductoGetRequest
import com.beaumanoir.gestock.data.remote.dto.producto.ProductoGetResponse

class ProductoRepository(
    private val api: ProductoApi
) {

    suspend fun getProductValues(
        ean: String,
        codemag: Int
    ): ProductoGetResponse {
        return api.getProductoValuesApi(ean,codemag)
    }

}