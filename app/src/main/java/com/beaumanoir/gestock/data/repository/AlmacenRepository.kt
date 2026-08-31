package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.AlmacenApi
import com.beaumanoir.gestock.data.remote.dto.MessageResponse
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenCreateRequest
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenLoginResponse
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenGetResponse

class AlmacenRepository (
    private val api: AlmacenApi
)
{

    suspend fun getAlmacenes(): AlmacenGetResponse {
        return api.getAlmacenesApi()
    }

    suspend fun loginAlmacen(
        codigo: Int
    ): AlmacenLoginResponse {
        return api.loginAlmacenApi(codigo)
    }

    suspend fun createAlmacen(
        data: AlmacenCreateRequest
    ): MessageResponse {
        return api.createAlmacenApi(data)
    }

    suspend fun deleteAlmacen(
        data: AlmacenDeleteRequest
    ): MessageResponse {
        return api.deleteAlmacenApi(data)
    }

}