package com.beaumanoir.gestock.data.API

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.GestockApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManagerFactory

object RetrofitClient {

    private const val BASE_URL = "https://gestock.ignorelist.com/gestock/"

    private class ApiKeyInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val prefs = GestockApp.instance.getSharedPreferences("app_prefs", MODE_PRIVATE)
            val apiKeyGuardada: String = prefs.getString("api_key_gestock", "4gWk87JnO1Bok61c3OrOSh8IVQGbkoWOpEyEcnTfyOjC1Gd24vjhbgiwKzBvo8Tc").toString()

            val request = chain.request()
                .newBuilder()
                .addHeader("Key", apiKeyGuardada)
                .build()

            return chain.proceed(request)
        }
    }

    private fun getRetrofit(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor())
            .writeTimeout(90L, TimeUnit.SECONDS)
            .readTimeout(90L, TimeUnit.SECONDS)
            .connectTimeout(20L, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    fun getApiService(): Endpoints {
        return getRetrofit().create(Endpoints::class.java)
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