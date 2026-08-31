package com.beaumanoir.gestock.ui.cajas

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.adapters.EliminarProductoAdapter
import com.beaumanoir.gestock.data.models.producto.EliminarProducto
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateCantidadRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetRequest
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2

class EliminarProductoCaja : AppCompatActivity(), EliminarProductoAdapter.OnItemClickListener {

    private var codigoAlmacen: Int = 0
    private lateinit var collectionStock: List<EliminarProducto>
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var productDelAdapter: EliminarProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private val stockList: MutableList<EliminarProducto> = ArrayList()
    private val scannedProductIds: MutableList<Int> = ArrayList()

    private val stockRepository: StockRepository by lazy {
        (application as GestockApp).stockRepository
    }

    private val cajaRepository: CajaRepository by lazy {
        (application as GestockApp).cajaRepository
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eliminar_producto_caja)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        val textView = findViewById<TextView>(R.id.total_productos_eliminar)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)

        findViewById<TextView>(R.id.titulo_pantalla_eliminar).text =
            "ELIMINAR ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_eliminar)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productDelAdapter = EliminarProductoAdapter(stockList, this@EliminarProductoCaja)
        recyclerView.adapter = productDelAdapter

        getStockCaja()

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
                if (producto.vecesBeepeado < producto.cantidad) {
                    scannedProductIds.add(producto.id)
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
            deleteStock()
        }
    }

    private fun getStockCaja() {

        lifecycleScope.launch {
            try {

                val response = stockRepository.getStock(
                    almacen = codigoAlmacen,
                    palet = idPalet,
                    caja = idCaja

                )

                val stock = response.stock
                val total = response.total

                if (stock.isNotEmpty()) {

                    collectionStock = stock.map { (ean, stock) ->
                        EliminarProducto(
                            id = stock.id,
                            ean = ean,
                            color = stock.color,
                            talla = stock.talla,
                            nombre = stock.nombre,
                            temporada = stock.temporada,
                            cantidad = stock.cantidad,
                            vecesBeepeado = 0
                        )
                    }

                    stockList.addAll(collectionStock)

                    productDelAdapter.notifyDataSetChanged()
                    recyclerView.scheduleLayoutAnimation()
                }

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@EliminarProductoCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteStock() {

        lifecycleScope.launch {
            try {

                val response = stockRepository.deleteStock(
                    almacen = codigoAlmacen,
                    palet = idPalet,
                    caja = idCaja,
                    ids = scannedProductIds

                )

                updateCantidadCaja()

                Toast.makeText(this@EliminarProductoCaja,response.message, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error eliminant stock de la caixa $idCaja del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@EliminarProductoCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateCantidadCaja() {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.updateCantidadCaja(
                    CajaUpdateCantidadRequest(
                        almacen = codigoAlmacen,
                        palet = idPalet,
                        caja = idCaja
                    )
                )

                Toast.makeText(this@EliminarProductoCaja,response.message, Toast.LENGTH_SHORT).show()

                finish()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error actualitzant cantitat stock de la caixa $idCaja del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@EliminarProductoCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onItemClick(almacenVirtualEliminar: EliminarProducto) {
        val ean = almacenVirtualEliminar.ean
        val producto = collectionStock.firstOrNull { it.ean == ean }

        if (producto != null && producto.vecesBeepeado < producto.cantidad) {

            scannedProductIds.add(producto.id)
            productDelAdapter.actualizarCantidadEscaneada(ean)
            findViewById<TextView>(R.id.total_productos_eliminar).text =
                "TOTAL: ${scannedProductIds.size}"

        } else {
            Toast.makeText(applicationContext, "No hay más productos con EAN $ean", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerMensajeError(exception: Exception): String {
        return when (exception) {
            is java.net.UnknownHostException ->
                "No se ha podido resolver la dirección del servidor"

            is java.net.ConnectException ->
                "No se ha podido conectar con el servidor"

            is java.net.SocketTimeoutException ->
                "El servidor ha tardado demasiado en responder"

            is javax.net.ssl.SSLException ->
                "No se ha podido establecer una conexión segura"

            is retrofit2.HttpException ->
                "Error del servidor: ${exception.code()}"

            else ->
                exception.localizedMessage ?: "Error inesperado"
        }
    }

}

