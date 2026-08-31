package com.beaumanoir.gestock.data.repository

import com.beaumanoir.gestock.data.remote.api.FamiliaApi
import com.beaumanoir.gestock.data.remote.dto.familia.FamiliasGetResponse

class FamiliaRepository(
    private val api: FamiliaApi
) {

    suspend fun getFamilias(): FamiliasGetResponse{
        return api.getFamiliasApi()
    }

}