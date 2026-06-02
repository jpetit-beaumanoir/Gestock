package com.beaumanoir.gestock.data

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.ProductoAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MoverProductosCaja : AppCompatActivity(), ProductoAdapter.OnItemClickListener {

    private var cajaDestino: Int = 0
    private var codigoAlmacen: Int = 0
    private lateinit var collectionStock: List<AlmacenVirtualProducto>
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private var paletDestino: Int = 0
    private lateinit var productMovAdapter: ProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var stock: ArrayList<AlmacenVirtualProducto>
    private val productMovAdapterList: MutableList<AlmacenVirtualProducto> = ArrayList()
    private val scannedProductIds: MutableSet<Int> = LinkedHashSet()

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(result: String)
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualProducto) {
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mover_producto_caja)

        val checkBox = findViewById<CheckBox>(R.id.checkbox_mover_caja_entera)
        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        val textView = findViewById<TextView>(R.id.total_productos_mover)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)

        stock = (intent.getSerializableExtra("stock") as? ArrayList<AlmacenVirtualProducto>)
            ?: ArrayList()
        collectionStock = stock.map {
            AlmacenVirtualProducto(
                it.ean, it.id, it.color, it.talla, it.nombre, it.temporada, it.cantidad, 0
            )
        }

        findViewById<TextView>(R.id.titulo_pantalla_mover).text =
            "MOVER ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_mover)
        recyclerView.layoutManager = LinearLayoutManager(this)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scannedProductIds.clear()
                for (producto in collectionStock) {
                    scannedProductIds.addAll(producto.id)
                    productMovAdapterList.add(producto)
                }
                if (::productMovAdapter.isInitialized) {
                    recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                    productMovAdapter.notifyDataSetChanged()
                } else {
                    productMovAdapter = ProductoAdapter(productMovAdapterList, this)
                    recyclerView.adapter = productMovAdapter
                    recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                }
                textView.text = "TOTAL: ${scannedProductIds.size}"
            }
        }

        val editText = findViewById<EditText>(R.id.ean_producto_mover)
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
                if (s.toString().length != 13) {
                    return
                }
                val ean = s.toString()
                val producto = collectionStock.firstOrNull { it.ean == ean }
                if (producto == null) {
                    mediaPlayer.start()
                    Toast.makeText(applicationContext, "$ean no encontrado en la caja", Toast.LENGTH_SHORT).show()
                } else if (producto.id.size != producto.cantidadEscaneada) {
                    scannedProductIds.add(producto.id[producto.cantidadEscaneada])
                    producto.cantidadEscaneada += 1
                    producto.cantidad = producto.cantidadEscaneada
                    if (!productMovAdapterList.contains(producto)) {
                        productMovAdapterList.add(producto)
                    }
                    if (::productMovAdapter.isInitialized) {
                        recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                        productMovAdapter.notifyDataSetChanged()
                    } else {
                        productMovAdapter = ProductoAdapter(productMovAdapterList, this@MoverProductosCaja)
                        recyclerView.adapter = productMovAdapter
                        recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                    }
                } else {
                    Toast.makeText(applicationContext, "No mas productos $ean en la caja", Toast.LENGTH_SHORT).show()
                }
                editText.text.clear()
                editText.requestFocus()
                textView.text = "TOTAL: ${scannedProductIds.size}"
            }
        })

        findViewById<AppCompatButton>(R.id.cancelar_movimiento_productos).setOnClickListener {
            finish()
        }
        findViewById<AppCompatButton>(R.id.confirmar_movimiento_productos).setOnClickListener {
            val destino = findViewById<EditText>(R.id.ean_caja_destino).text.toString()
            if (destino.isEmpty() || destino.length < 12) {
                Toast.makeText(applicationContext, "Especifica una caja de destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            paletDestino = destino.substring(4..7).toInt()
            cajaDestino = destino.substring(8..11).toInt()
            moveStockAPI(object : APIResponseCallback {
                override fun onSuccess(result: String) {
                    Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(errorMessage: String) {
                    Toast.makeText(this@MoverProductosCaja, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun moveStockAPI(callback: APIResponseCallback) {
        RetrofitClient.getApiService(this)
            .moveStock(MoveStockRequest(codigoAlmacen, paletDestino, cajaDestino, scannedProductIds.toList()))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI(idPalet, idCaja)
                        updateCantidadCajaAPI(paletDestino, cajaDestino)
                        callback.onSuccess(
                            "${scannedProductIds.size} productos movidos a caja $cajaDestino ; palet $paletDestino"
                        )
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@MoverProductosCaja, JSONObject(errorString).getString("detail"), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@MoverProductosCaja, "No tienes conexión a internet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MoverProductosCaja, "Error: " + t.message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateCantidadCajaAPI(palet: Int, caja: Int) {
        RetrofitClient.getApiService(this)
            .updateCantidadCaja(codigoAlmacen, palet, caja)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@MoverProductosCaja, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@MoverProductosCaja, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MoverProductosCaja, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }
}
