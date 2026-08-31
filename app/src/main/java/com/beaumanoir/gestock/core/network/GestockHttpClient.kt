package com.beaumanoir.gestock.core.network

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.core.constants.GestockConstants
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object GestockHttpClient {

    fun create(context: Context): OkHttpClient {

        /*
         * 1. CA privada que firma el certificado del servidor.
         *
         * res/raw/ca.crt
         */
        val certificateFactory = CertificateFactory.getInstance("X.509")

        val caCertificate = context.resources
            .openRawResource(R.raw.aiq_ca)
            .use { inputStream ->
                certificateFactory.generateCertificate(inputStream)
                        as X509Certificate
            }

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("gestock-ca", caCertificate)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(trustStore)
        }

        val trustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .single()

        /*
         * 2. Certificado cliente Gestock.
         *
         * res/raw/gestock_android.p12
         *
         * Cambia el nombre por el nombre real del recurso.
         */
        val clientKeyStore = KeyStore.getInstance("PKCS12").apply {
            context.resources
                .openRawResource(R.raw.app_gestock)
                .use { inputStream ->
                    load(inputStream, charArrayOf())
                }
        }

        val aliases = clientKeyStore.aliases()

        while (aliases.hasMoreElements()) {
            Log.d(
                "GESTOCK_CERT",
                "Alias=${aliases.nextElement()}"
            )
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(clientKeyStore, charArrayOf())
        }

        /*
         * 3. Contexto TLS con:
         *
         * - Certificado cliente P12.
         * - CA de confianza del servidor.
         */
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(
                keyManagerFactory.keyManagers,
                arrayOf(trustManager),
                null
            )
        }

        /*
         * 4. DNS personalizado.
         *
         * La URL continúa usando tresf.net, por lo que:
         *
         * - SNI será tresf.net.
         * - La validación del certificado se hará contra tresf.net.
         * - La conexión TCP se realizará contra 80.58.141.75.
         */
        val gestockDns = object : Dns {

            override fun lookup(hostname: String): List<InetAddress> {

                Log.d("GestockDNS", "Resolviendo hostname=$hostname")

                return if (hostname.equals(GestockConstants.TLS_HOSTNAME, ignoreCase = true)) {

                    val address = InetAddress.getByAddress(
                        GestockConstants.TLS_HOSTNAME,
                        byteArrayOf(
                            80.toByte(),
                            58.toByte(),
                            141.toByte(),
                            75.toByte()
                        )
                    )

                    Log.d(
                        "GestockDNS",
                        "${GestockConstants.TLS_HOSTNAME} resuelto manualmente como ${address.hostAddress}"
                    )

                    listOf(address)

                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        }

        Log.d(
            "GESTOCK_DNS",
            gestockDns.lookup("tresf.net")
                .joinToString { it.hostAddress }
        )

        return OkHttpClient.Builder()
            .dns(gestockDns)
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManager
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

            .addInterceptor { chain ->

                val request = chain.request()
                    .newBuilder()
                    .addHeader(
                        "Key",
                        getApiKey()
                    )
                    .build()

                chain.proceed(request)
            }

            .addInterceptor { chain ->

                val request = chain.request()

                Log.d(
                    "GESTOCK_URL",
                    request.url.toString()
                )

                chain.proceed(request)
            }

            .build()
    }

    private fun getApiKey(): String {
        val prefs =
            GestockApp.instance.getSharedPreferences(
                "app_prefs",
                MODE_PRIVATE
            )

        return prefs.getString(
            "api_key_gestock",
            "4gWk87JnO1Bok61c3OrOSh8IVQGbkoWOpEyEcnTfyOjC1Gd24vjhbgiwKzBvo8Tc"
        ) ?: ""
    }

}