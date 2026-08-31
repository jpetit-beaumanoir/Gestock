package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.AuthApi
import com.beaumanoir.gestock.data.remote.dto.auth.AuthUserResponse

class AuthRepository(
    private val api: AuthApi
) {
    suspend fun validarUsuari(
        key: String
    ): AuthUserResponse{
        return api.validarUsuariApi(key)
    }

}