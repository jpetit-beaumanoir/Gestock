package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.PaletApi
import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.palet.PaletCreateRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletsGetRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletsGetResponse

class PaletRepository(
    private val api: PaletApi
) {

    suspend fun getPalets(
        almacen: Int
    ): PaletsGetResponse {
        return api.getPaletsApi(almacen)
    }

    suspend fun createPalet(
        request: PaletCreateRequest
    ): MessageResponse {
        return api.createPaletApi(request)
    }

    suspend fun deletePalet(
        almacen: Int,
        palet: Int
    ): MessageResponse {
        return api.deletePaletApi(almacen,palet)
    }

}