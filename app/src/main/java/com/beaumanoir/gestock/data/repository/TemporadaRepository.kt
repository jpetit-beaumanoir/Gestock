package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.TemporadaApi
import com.beaumanoir.gestock.data.remote.dto.temporada.TemporadasGetResponse

class TemporadaRepository(
    private val api: TemporadaApi
) {

    suspend fun getTemporadas(): TemporadasGetResponse{
        return api.getTemporadasApi()
    }

}