package com.beaumanoir.gestock.data

import android.app.AlertDialog
import android.content.Context
import android.widget.ProgressBar
import android.widget.Toast
import com.beaumanoir.gestock.data.API.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Locale

object Menu {

    fun checkCajaExists(context: Context, almacen: Int, palet: Int, caja: Int, eans: List<String>, file: File) {
        RetrofitClient.getApiService().existCaja(almacen, palet, caja)
            .enqueue(object : Callback<CajaExiste> {
                override fun onResponse(call: Call<CajaExiste>, response: Response<CajaExiste>) {
                    if (response.isSuccessful) {
                        checkStockExists(almacen, palet, caja, eans, context, file)
                    } else {
                        Toast.makeText(context, "Caja $caja del palet $palet, no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CajaExiste>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(context, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun checkStockExists(almacen: Int, palet: Int, caja: Int, eans: List<String>, context: Context, file: File) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Procesando EANs")
            .setMessage("Por favor, espera...")
            .setCancelable(false)
            .setView(ProgressBar(context).apply { isIndeterminate = true })
            .create()
        dialog.show()

        val distinct = eans.distinct()
        val notFound = ArrayList<String>()
        var processed = 0
        var stop = false

        for (ean in distinct) {
            if (stop) return
            getValuesfromProductoAPI(ean, almacen, context, object : AñadirProductoCaja.APIResponseCallback {
                override fun onSuccess(result: ProductValues) {
                    if (stop) return
                    processed++
                    if (processed != distinct.size) return
                    if (notFound.isEmpty()) {
                        getStockCajaFromAPI(almacen, palet, caja, eans, context, file)
                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                        showNotFoundDialog(context, notFound)
                    }
                }

                override fun onError(errorMessage: String) {
                    if (errorMessage.contains("demasiadas peticiones", true)) {
                        stop = true
                        dialog.dismiss()
                        Toast.makeText(context, "Demasiadas peticiones, contacta con el administrador para aumentar la tasa", Toast.LENGTH_LONG).show()
                    } else {
                        if (stop) return
                        notFound.add(ean)
                        processed++
                        if (processed == distinct.size) {
                            dialog.dismiss()
                            showNotFoundDialog(context, notFound)
                        }
                    }
                }
            })
        }
    }

    private fun showNotFoundDialog(context: Context, notFound: List<String>) {
        val message = buildString {
            append("No se puede importar el archivo porque hay productos no encontrados.\n\n")
            append("Productos no encontrados: ${notFound.size}\n\n")
            append(notFound.joinToString("\n"))
        }
        AlertDialog.Builder(context)
            .setTitle("Error de importación")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getValuesfromProductoAPI(ean: String, codigoAlmacen: Int, context: Context, callback: AñadirProductoCaja.APIResponseCallback) {
        RetrofitClient.getApiService().getProductValues(ean, codigoAlmacen)
            .enqueue(object : Callback<ProductValues> {
                override fun onResponse(call: Call<ProductValues>, response: Response<ProductValues>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            callback.onSuccess(body)
                        } else {
                            callback.onError("No se ha encontrado información de este producto")
                        }
                    } else if (response.code() == 429) {
                        callback.onError("Se han realizado demasiadas peticiones. Contacta con el administrador si es necesario aumentarlas")
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        try {
                            callback.onError(JSONObject(errorString).optString("detail", "Error: $errorString"))
                        } catch (e: Exception) {
                            callback.onError("Error: $e")
                        }
                    }
                }

                override fun onFailure(call: Call<ProductValues>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        callback.onError("No tienes conexión a internet")
                    } else {
                        callback.onError("Error " + t.message)
                    }
                }
            })
    }

    private fun getStockCajaFromAPI(almacen: Int, palet: Int, caja: Int, eans: List<String>, context: Context, file: File) {
        RetrofitClient.getApiService().getStockCaja(almacen, palet, caja)
            .enqueue(object : Callback<StockCajaResponse> {
                override fun onResponse(call: Call<StockCajaResponse>, response: Response<StockCajaResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()!!
                        var productIds: List<Int> = emptyList()
                        if (body.stock.isNotEmpty()) {
                            productIds = body.stock.map { (ean, stock) ->
                                AlmacenVirtualProducto(
                                    ean, stock.id, stock.color, stock.talla,
                                    stock.nombre, stock.temporada, stock.cantidad, 0
                                )
                            }.flatMap { it.id }
                        }
                        val size = eans.size
                        AlertDialog.Builder(context)
                            .setTitle("Palet $palet Caja $caja")
                            .setMessage("RFID: $size productos\nGESTOCK: ${body.total} productos\n\n¿Desea sobreescribir la caja?")
                            .setPositiveButton("Sí") { d, _ ->
                                d.dismiss()
                                if (productIds.isNotEmpty()) {
                                    deleteStockAPI(context,productIds, almacen, palet, caja)
                                }
                                addStockAPI(context, eans, almacen, palet, caja)
                                file.delete()
                            }
                            .setNegativeButton("No") { d, _ -> d.dismiss() }
                            .show()
                    } else {
                        Toast.makeText(context, "Error stock: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<StockCajaResponse>, t: Throwable) {
                    Toast.makeText(context, "Error en stock: " + t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun addStockAPI(context: Context, eans: List<String>, almacen:Int, palet: Int, caja: Int) {
        RetrofitClient.getApiService()
            .addStock(AddStockRequest(almacen, palet, caja, eans))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI(context, almacen,palet, caja)
                        Toast.makeText(context, "${eans.size} Productos añadidos", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(context, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(context, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun deleteStockAPI(context: Context, deleteIds: List<Int>, almacen:Int,  palet: Int, caja: Int) {
        RetrofitClient.getApiService()
            .deleteStock(DeleteStockRequest(almacen, palet, caja, deleteIds))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI(context,almacen,palet, caja)
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(context, JSONObject(errorString).getString("detail"), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(context, "No tienes conexión a internet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: " + t.message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateCantidadCajaAPI(context: Context, almacen:Int, palet: Int, caja: Int) {
        RetrofitClient.getApiService()
            .updateCantidadCaja(almacen, palet, caja)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(context, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(context, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

}