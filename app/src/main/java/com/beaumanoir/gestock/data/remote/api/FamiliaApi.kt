package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.familia.FamiliasGetResponse
import retrofit2.http.GET

interface FamiliaApi {

    @GET("familias")
    suspend fun getFamiliasApi(): FamiliasGetResponse

}