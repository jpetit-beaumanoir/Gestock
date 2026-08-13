package com.beaumanoir.gestock.data

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.EliminarProductoAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class EliminarProductoCaja : AppCompatActivity(), EliminarProductoAdapter.OnItemClickListener {

    private var codigoAlmacen: Int = 0
    private lateinit var collectionStock: List<AlmacenVirtualEliminarProducto>
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var productDelAdapter: EliminarProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var stock: ArrayList<AlmacenVirtualProducto>
    private val productDelAdapterList: MutableList<AlmacenVirtualEliminarProducto> = ArrayList()
    private val scannedProductIds: MutableList<Int> = ArrayList()

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eliminar_producto_caja)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        val textView = findViewById<TextView>(R.id.total_productos_eliminar)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)

        stock = (intent.getSerializableExtra("stock") as? ArrayList<AlmacenVirtualProducto>)
            ?: ArrayList()
        collectionStock = stock.map {
            AlmacenVirtualEliminarProducto(
                it.id, it.ean, it.color, it.talla, it.nombre, it.cantidad, 0, it.temporada
            )
        }

        findViewById<TextView>(R.id.titulo_pantalla_eliminar).text =
            "ELIMINAR ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_eliminar)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productDelAdapter = EliminarProductoAdapter(productDelAdapterList, this)
        recyclerView.adapter = productDelAdapter
        productDelAdapterList.addAll(collectionStock)
        recyclerView.layoutParams.height = if (collectionStock.size > 4) 1120 else -2
        productDelAdapter.notifyDataSetChanged()

        val editText = findViewById<EditText>(R.id.ean_producto_eliminar)
        editText.requestFocus()
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 0 || actionId == 4 || actionId == 6) {
                editText.text.clear()
                editText.requestFocus()
                true
            } else {
                false
            }
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s == null || s.length != 13) {
                    return
                }
                val ean = s.toString()
                val producto = collectionStock.firstOrNull { it.ean == ean }
                if (producto == null) {
                    mediaPlayer.start()
                    Toast.makeText(applicationContext, "EAN $ean no encontrado en la caja", Toast.LENGTH_SHORT).show()
                    editText.text.clear()
                    editText.requestFocus()
                    return
                }
                if (producto.cantidadEscaneada < producto.cantidad) {
                    scannedProductIds.add(producto.id[producto.cantidadEscaneada])
                    productDelAdapter.actualizarCantidadEscaneada(ean)
                    editText.text.clear()
                    editText.requestFocus()
                    textView.text = "TOTAL: ${scannedProductIds.size}"
                    return
                }
                mediaPlayer.start()
                Toast.makeText(applicationContext, "No hay más productos con EAN $ean en la caja", Toast.LENGTH_SHORT).show()
                editText.text.clear()
                editText.requestFocus()
            }
        })

        findViewById<AppCompatButton>(R.id.cancelar_eliminacion_productos).setOnClickListener {
            finish()
        }
        findViewById<AppCompatButton>(R.id.confirmar_eliminacion_productos).setOnClickListener {
            deleteStockAPI()
        }
    }

    private fun deleteStockAPI() {
        RetrofitClient.getApiService()
            .deleteStock(DeleteStockRequest(codigoAlmacen, idPalet, idCaja, scannedProductIds))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI()
                        Toast.makeText(this@EliminarProductoCaja, "${scannedProductIds.size} eliminados", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@EliminarProductoCaja, JSONObject(errorString).getString("detail"), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@EliminarProductoCaja, "No tienes conexión a internet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EliminarProductoCaja, "Error: " + t.message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateCantidadCajaAPI() {
        RetrofitClient.getApiService()
            .updateCantidadCaja(codigoAlmacen, idPalet, idCaja)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@EliminarProductoCaja, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@EliminarProductoCaja, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@EliminarProductoCaja, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    override fun onItemClick(almacenVirtualEliminar: AlmacenVirtualEliminarProducto) {
        val ean = almacenVirtualEliminar.ean
        val producto = collectionStock.firstOrNull { it.ean == ean }
        if (producto != null && producto.cantidadEscaneada < producto.cantidad) {
            scannedProductIds.add(producto.id[producto.cantidadEscaneada])
            productDelAdapter.actualizarCantidadEscaneada(ean)
            findViewById<TextView>(R.id.total_productos_eliminar).text =
                "TOTAL: ${scannedProductIds.size}"
        } else {
            Toast.makeText(applicationContext, "No hay más productos con EAN $ean", Toast.LENGTH_SHORT).show()
        }
    }
}
