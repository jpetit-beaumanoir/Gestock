package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.temporada.TemporadasGetResponse
import retrofit2.http.GET

interface TemporadaApi {

    @GET("temporadas")
    suspend fun getTemporadasApi(): TemporadasGetResponse

}