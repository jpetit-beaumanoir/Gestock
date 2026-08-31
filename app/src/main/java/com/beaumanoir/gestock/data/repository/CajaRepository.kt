package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.CajaApi
import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.caja.CajaCreateRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaGetDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaGetDescTempResponse
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateCantidadRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajasGetRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajasGetResponse

class CajaRepository(
    private val api: CajaApi
)
{
    suspend fun getCajas(
        almacen: Int,
        palet: Int
    ): CajasGetResponse {

        return api.getCajasApi(almacen,palet)
    }

    suspend fun createCaja(
        request: CajaCreateRequest
    ): MessageResponse {

        return api.createCajaApi(request)
    }

    suspend fun deleteCaja(
        almacen: Int,
        palet: Int,
        caja: Int
    ): MessageResponse {
        return api.deleteCajaApi(almacen, palet, caja)
    }

    suspend fun getDescTempCaja(
        almacen: Int,
        palet: Int,
        caja: Int
    ): CajaGetDescTempResponse {

        return api.getDescTempCajaApi(almacen, palet, caja)
    }

    suspend fun updateDescTempCaja(
        request: CajaUpdateDescTempRequest
    ): MessageResponse {

        return api.updateDescTempCajaApi(request)
    }

    suspend fun updateCantidadCaja(
        request: CajaUpdateCantidadRequest
    ): MessageResponse {

        return api.updateCantidadCajaApi(request)
    }

}