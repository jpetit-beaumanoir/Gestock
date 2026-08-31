package com.beaumanoir.gestock.core.network

import com.beaumanoir.gestock.data.remote.api.AlmacenApi
import com.beaumanoir.gestock.data.remote.api.AuthApi
import com.beaumanoir.gestock.data.remote.api.CajaApi
import com.beaumanoir.gestock.data.remote.api.FamiliaApi
import com.beaumanoir.gestock.data.remote.api.PaletApi
import com.beaumanoir.gestock.data.remote.api.ProductoApi
import com.beaumanoir.gestock.data.remote.api.StockApi
import com.beaumanoir.gestock.data.remote.api.TemporadaApi
import retrofit2.Retrofit

class GestockApiFactory(
    private val retrofit: Retrofit
) {

    val almacenApi: AlmacenApi by lazy {
        retrofit.create(AlmacenApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val cajaApi: CajaApi by lazy {
        retrofit.create(CajaApi::class.java)
    }

    val paletApi: PaletApi by lazy {
        retrofit.create(PaletApi::class.java)
    }

    val familiaApi: FamiliaApi by lazy {
        retrofit.create(FamiliaApi::class.java)
    }

    val stockApi: StockApi by lazy {
        retrofit.create(StockApi::class.java)
    }

    val temporadaApi: TemporadaApi by lazy {
        retrofit.create(TemporadaApi::class.java)
    }

    val productoApi: ProductoApi by lazy {
        retrofit.create(ProductoApi::class.java)
    }
}