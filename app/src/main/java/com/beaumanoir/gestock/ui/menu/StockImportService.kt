package com.beaumanoir.gestock.ui.menu

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.beaumanoir.gestock.core.network.GestockApiFactory
import com.beaumanoir.gestock.data.models.producto.Producto
import com.beaumanoir.gestock.data.remote.api.ProductoApi
import com.beaumanoir.gestock.data.remote.dto.producto.ProductoGetRequest
import com.beaumanoir.gestock.data.repository.ProductoRepository
import com.beaumanoir.gestock.ui.cajas.AddProductoCaja
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Locale

object StockImportService {
    /*private fun checkStockExists(almacen: Int, palet: Int, caja: Int, eans: List<String>, context: Context, file: File) {
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
            getValuesfromProductoAPI(ean, almacen, context, object : AddProductoCaja.APIResponseCallback {
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

    private fun getValuesfromProducto(ean: String) {

        lifecycleScope.launch {
            try {

                val response = ProductoRepository.g(
                    ProductoGetRequest(
                        ean = ean,
                        codemag = codigoAlmacen
                    )
                )

                eansAfegirList.add(response.ean)
                productoList.add(
                    Producto(
                        ean = response.ean,
                        color = response.color,
                        talla = response.talla,
                        nombre = response.nombre,
                        temporada = response.temporada,
                        cantidad = 1
                    )
                )
                eanEditText.text.clear()
                totalProductosTextView.text = "TOTAL: ${eansAfegirList.size}"

            } catch (e: Exception) {

                eanEditText.text.clear()

                mediaPlayer.start()

                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint valors del producte $ean",
                    e
                )

                Toast.makeText(
                    this@AddProductoCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getValuesfromProductoAPI(ean: String, codigoAlmacen: Int, context: Context, callback: AddProductoCaja.APIResponseCallback) {



        GestockApiFactory.getApi(context).getProductValues(ean, codigoAlmacen)
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
        GestockApiFactory.getApi(context).getStockCaja(almacen, palet, caja)
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
        GestockApiFactory.getApi(context)
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

    private fun deleteStockAPI(context: Context, deleteIds: List<Int>, almacen:Int, palet: Int, caja: Int) {
        GestockApiFactory.getApi(context)
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
        GestockApiFactory.getApi(context)
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
*/
}