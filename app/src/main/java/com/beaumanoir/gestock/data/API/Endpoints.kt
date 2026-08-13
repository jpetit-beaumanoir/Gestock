package com.beaumanoir.gestock.data.API

import android.R
import com.beaumanoir.gestock.data.AddStockRequest
import com.beaumanoir.gestock.data.AlmacenCreateRequest
import com.beaumanoir.gestock.data.AlmacenResponse
import com.beaumanoir.gestock.data.CajaExiste
import com.beaumanoir.gestock.data.CajasResponse
import com.beaumanoir.gestock.data.DeleteStockRequest
import com.beaumanoir.gestock.data.DescTempCajaResponse
import com.beaumanoir.gestock.data.Familia
import com.beaumanoir.gestock.data.FilteredSearch
import com.beaumanoir.gestock.data.MoveStockRequest
import com.beaumanoir.gestock.data.PaletsResponse
import com.beaumanoir.gestock.data.ProductValues
import com.beaumanoir.gestock.data.StockCajaResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface Endpoints {

    @POST("/gestock/stock/add")
    fun addStock(
        @Body request: AddStockRequest
    ): Call<ResponseBody>

    @POST("/gestock/almacenes/create")
    fun createAlmacen(
        @Body almacenRequest: AlmacenCreateRequest
    ): Call<ResponseBody>

    @POST("/gestock/cajas/create")
    fun createCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int
    ): Call<ResponseBody>

    @POST("/gestock/palets/create")
    fun createPalet(
        @Query("almacen") almacen: Int
    ): Call<ResponseBody>

    @DELETE("/gestock/almacenes/delete")
    fun deleteAlmacen(
        @Query("code") code: Int
    ): Call<ResponseBody>

    @DELETE("/gestock/cajas/delete")
    fun deleteCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): Call<ResponseBody>

    @DELETE("/gestock/palets/delete")
    fun deletePalet(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int
    ): Call<ResponseBody>

    @POST("/gestock/stock/delete")
    fun deleteStock(
        @Body request: DeleteStockRequest
    ): Call<ResponseBody>

    @GET("/gestock/cajas/{almacen}/{palet}/{caja}")
    fun existCaja(
        @Path("almacen") almacen: Int,
        @Path("palet") palet: Int,
        @Path("caja") caja: Int
    ): Call<CajaExiste>

    @GET("/gestock/filtered-search")
    fun filteredSearch(
        @Query("almacen") almacen: Int,
        @Query("ean") ean: String? = null,
        @Query("talla") talla: String? = null,
        @Query("nombre") nombre: String? = null,
        @Query("familia") familia: String? = null,
        @Query("color") color: String? = null,
        @Query("temporada") temporada: String? = null
    ): Call<List<FilteredSearch>>

    @GET("/gestock/almacenes")
    fun getAlmacenes(): Call<AlmacenResponse>

    @GET("/gestock/cajas")
    fun getCajas(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int
    ): Call<CajasResponse>

    @GET("/gestock/cajas/get-desc-temp")
    fun getDescTempCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): Call<DescTempCajaResponse>

    @GET("/gestock/familias")
    fun getFamilias(
        @Query("almacen") almacen: Int
    ): Call<List<Familia>>

    @GET("/gestock/palets")
    fun getPalets(
        @Query("almacen") almacen: Int
    ): Call<PaletsResponse>

    @GET("/gestock/products/values")
    fun getProductValues(
        @Query("ean") ean: String,
        @Query("codemag") codemag: Int
    ): Call<ProductValues>

    @GET("/gestock/stock")
    fun getStockCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): Call<StockCajaResponse>

    @GET("/gestock/login")
    fun loginAlmacen(
        @Query("code") code: Int
    ): Call<ResponseBody>

    @GET("/gestock/user")
    fun validarUser(
        @Query("key") key: String
    ): Call<ResponseBody>

    @POST("/gestock/stock/move")
    fun moveStock(
        @Body request: MoveStockRequest
    ): Call<ResponseBody>

    @GET("/gestock/stock-export")
    fun stockExport(
        @Query("almacen") almacen: Int
    ): Call<List<FilteredSearch>>

    @Multipart
    @POST("/gestock/products/add")
    fun subirCatalogo(
        @Part csvFile: MultipartBody.Part,
        @Part("brand") brand: RequestBody
    ): Call<ResponseBody>

    @POST("/gestock/cajas/update-cantidad")
    fun updateCantidadCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int
    ): Call<ResponseBody>

    @POST("/gestock/cajas/update-desc-temp")
    fun updateDescTempCaja(
        @Query("almacen") almacen: Int,
        @Query("palet") palet: Int,
        @Query("caja") caja: Int,
        @Query("descripcion") descripcion: String = "SIN DESCRIPCIÓN",
        @Query("temporada") temporada: String = "SIN TEMPORADA"
    ): Call<ResponseBody>
}