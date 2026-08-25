package com.beaumanoir.gestock

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.beaumanoir.gestock.data.API.Endpoints
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GestockApiFactory {

    private const val BASE_URL =
        "https://tresf.net:65432/"

    @Volatile
    private var apiInstance: Endpoints? = null

    fun getApi(context: Context): Endpoints {

        return apiInstance ?: synchronized(this) {

            apiInstance ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(
                    GestockHttpClient.create(
                        context.applicationContext
                    )
                )
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(Endpoints::class.java)
                .also { createdApi ->
                    apiInstance = createdApi
                }
        }
    }

    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
