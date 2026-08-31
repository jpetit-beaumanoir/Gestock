package com.beaumanoir.gestock

import android.app.Application
import com.beaumanoir.gestock.core.constants.GestockConstants
import com.beaumanoir.gestock.core.network.GestockApiFactory
import com.beaumanoir.gestock.core.network.GestockHttpClient
import com.beaumanoir.gestock.data.repository.AlmacenRepository
import com.beaumanoir.gestock.data.repository.AuthRepository
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.data.repository.FamiliaRepository
import com.beaumanoir.gestock.data.repository.PaletRepository
import com.beaumanoir.gestock.data.repository.ProductoRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import com.beaumanoir.gestock.data.repository.TemporadaRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GestockApp : Application() {

    lateinit var almacenRepository: AlmacenRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var cajaRepository: CajaRepository
        private set

    lateinit var paletRepository: PaletRepository
        private set

    lateinit var familiaRepository: FamiliaRepository
        private set

    lateinit var stockRepository: StockRepository
        private set

    lateinit var temporadaRepository: TemporadaRepository
        private set

    lateinit var productoRepository: ProductoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val retrofit = Retrofit.Builder()
            .baseUrl(GestockConstants.API_BASE_URL)
            .client(GestockHttpClient.create(applicationContext))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiFactory = GestockApiFactory(retrofit)

        almacenRepository = AlmacenRepository(
            apiFactory.almacenApi
        )

        authRepository = AuthRepository(
            apiFactory.authApi
        )

        cajaRepository = CajaRepository(
            apiFactory.cajaApi
        )

        paletRepository = PaletRepository(
            apiFactory.paletApi
        )

        familiaRepository = FamiliaRepository(
            apiFactory.familiaApi
        )

        stockRepository = StockRepository(
            apiFactory.stockApi
        )

        temporadaRepository = TemporadaRepository(
            apiFactory.temporadaApi
        )

        productoRepository = ProductoRepository(
            apiFactory.productoApi
        )
    }

    companion object {
        lateinit var instance: GestockApp
            private set
    }
}