package com.beaumanoir.gestock.data.API

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.beaumanoir.gestock.R
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

    private const val API_KEY = "p9WAIq8tJjWdOI3beUhpr8MN3XYjE1aLpEvif6QmqsESGHgpvxjz7odgvgiuBoF4"
    private const val BASE_URL = "https://gestock.ignorelist.com/gestock/"


    /*private fun getSslContext(context: Context): Pair<SSLContext, X509TrustManager> {
        val certificateFactory = CertificateFactory.getInstance("X.509")

        val inputStream: InputStream =
            context.resources.openRawResource(R.raw.gestock_chain)

        val certificates: Collection<Certificate> =
            certificateFactory.generateCertificates(inputStream)

        inputStream.close()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)

        var index = 0
        for (certificate in certificates) {
            keyStore.setCertificateEntry("ca$index", certificate)
            index++
        }

        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        trustManagerFactory.init(keyStore)

        val trustManager = trustManagerFactory.trustManagers[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
            null,
            arrayOf(trustManager),
            null
        )

        return Pair(sslContext, trustManager)
    } */

    private class ApiKeyInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
                .newBuilder()
                .addHeader("Key", API_KEY)
                .build()

            return chain.proceed(request)
        }
    }

    /*
    private fun getRetrofit(context: Context): Retrofit {
        val sslContextPair = getSslContext(context)

        val sslContext = sslContextPair.component1()
        val trustManager = sslContextPair.component2()

        val socketFactory = sslContext.socketFactory

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(socketFactory, trustManager)
            .addInterceptor(ApiKeyInterceptor())
            .writeTimeout(90L, TimeUnit.SECONDS)
            .readTimeout(90L, TimeUnit.SECONDS)
            .connectTimeout(90L, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
     */

    private fun getRetrofit(context: Context): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor())
            .writeTimeout(90L, TimeUnit.SECONDS)
            .readTimeout(90L, TimeUnit.SECONDS)
            .connectTimeout(90L, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    fun getApiService(context: Context): Endpoints {
        return getRetrofit(context).create(Endpoints::class.java)
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