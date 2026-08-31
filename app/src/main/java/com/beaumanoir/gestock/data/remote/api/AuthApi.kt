package com.beaumanoir.gestock.data.remote.api

import com.beaumanoir.gestock.data.remote.dto.auth.AuthUserResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface AuthApi {

    @GET("/user")
    suspend fun validarUsuariApi(
        @Path("key") key: String
    ): AuthUserResponse

}