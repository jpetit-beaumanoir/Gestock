package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.StockApi
import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockAddRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetResponse
import com.beaumanoir.gestock.data.remote.dto.stock.StockMoveRequest

class StockRepository(
    private val api: StockApi
) {

    suspend fun getStock(
        almacen: Int,
        palet: Int,
        caja: Int
    ): StockGetResponse {
        return api.getStockApi(almacen,palet,caja)
    }

    suspend fun addStock(
        request: StockAddRequest
    ): MessageResponse {
        return api.addStockApi(request)
    }

    suspend fun deleteStock(
        almacen: Int,
        palet: Int,
        caja: Int,
        ids: List<Int>
    ): MessageResponse {
        return api.deleteStockApi(almacen,palet,caja,ids)
    }

    suspend fun moveStock(
        request: StockMoveRequest
    ): MessageResponse {
        return api.moveStockApi(request)
    }

    suspend fun exportStock(
        almacen: Int,
        ean: String? = null,
        talla: String? = null,
        nombre: String? = null,
        familia: String? = null,
        color: String? = null,
        temporada: String? = null
    ): StockExportResponse {
        return api.exportStockApi(
            almacen,
            ean,
            talla,
            nombre,
            familia,
            color,
            temporada
        )
    }

}