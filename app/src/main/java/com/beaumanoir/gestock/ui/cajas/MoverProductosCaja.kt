package com.beaumanoir.gestock.ui.cajas

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.CheckBox
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
import com.beaumanoir.gestock.data.local.adapters.MoverProductoAdapter
import com.beaumanoir.gestock.data.models.producto.MoverProducto
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateCantidadRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockMoveRequest
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2

class MoverProductosCaja : AppCompatActivity(), MoverProductoAdapter.OnItemClickListener {

    private var cajaDestino: Int = 0
    private var codigoAlmacen: Int = 0
    private lateinit var collectionStock: List<MoverProducto>
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private var paletDestino: Int = 0
    private lateinit var productMovAdapter: MoverProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var stock: ArrayList<MoverProducto>
    private val productMovAdapterList: MutableList<MoverProducto> = ArrayList()
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
        setContentView(R.layout.mover_producto_caja)

        val checkBox = findViewById<CheckBox>(R.id.checkbox_mover_caja_entera)
        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        val textView = findViewById<TextView>(R.id.total_productos_mover)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)

        collectionStock = stock.map {
            MoverProducto(
                id = it.id,
                ean = it.ean,
                color = it.color,
                talla = it.talla,
                nombre = it.nombre,
                temporada = it.temporada,
                cantidad = it.cantidad,
                vecesBeepeado = 0
            )
        }

        findViewById<TextView>(R.id.titulo_pantalla_mover).text =
            "MOVER ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_mover)
        recyclerView.layoutManager = LinearLayoutManager(this)

        getStockCaja()

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scannedProductIds.clear()
                for (producto in collectionStock) {
                    scannedProductIds.addAll(listOf(producto.id))
                    productMovAdapterList.add(producto)
                }
                if (::productMovAdapter.isInitialized) {
                    recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                    productMovAdapter.notifyDataSetChanged()
                } else {
                    productMovAdapter = MoverProductoAdapter(productMovAdapterList, this@MoverProductosCaja)
                    recyclerView.adapter = productMovAdapter
                    recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                }
                textView.text = "TOTAL: ${scannedProductIds.size}"
            }
        }

        val editTextEAN = findViewById<EditText>(R.id.ean_producto_mover)
        editTextEAN.requestFocus()
        editTextEAN.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 0 || actionId == 4 || actionId == 6) {
                editTextEAN.text.clear()
                editTextEAN.requestFocus()
                true
            } else {
                false
            }
        }
        editTextEAN.addTextChangedListener(object : TextWatcher {
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

                } else if (producto.id != producto.vecesBeepeado) {

                    scannedProductIds.add(producto.id)
                    producto.vecesBeepeado += 1
                    producto.cantidad = producto.vecesBeepeado

                    if (!productMovAdapterList.contains(producto)) {
                        productMovAdapterList.add(producto)
                    }

                    if (::productMovAdapter.isInitialized) {
                        recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                        productMovAdapter.notifyDataSetChanged()
                    } else {
                        productMovAdapter = MoverProductoAdapter(productMovAdapterList, this@MoverProductosCaja)
                        recyclerView.adapter = productMovAdapter
                        recyclerView.layoutParams.height = if (productMovAdapterList.size <= 4) -2 else 960
                    }
                } else {
                    Toast.makeText(applicationContext, "No mas productos $ean en la caja", Toast.LENGTH_SHORT).show()
                }
                editTextEAN.text.clear()
                editTextEAN.requestFocus()
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

            moveStock()
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
                        MoverProducto(
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

                    productMovAdapterList.addAll(collectionStock)

                    productMovAdapter.notifyDataSetChanged()
                    recyclerView.scheduleLayoutAnimation()
                }

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@MoverProductosCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun moveStock() {

        lifecycleScope.launch {
            try {

                val response = stockRepository.moveStock(
                    StockMoveRequest(
                        almacen = codigoAlmacen,
                        palet = paletDestino,
                        caja = cajaDestino,
                        ids = scannedProductIds
                    )
                )

                updateCantidadCaja(idPalet, idCaja)
                updateCantidadCaja(paletDestino, cajaDestino)

                Toast.makeText(this@MoverProductosCaja,response.message, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error movent caixes de la caixa $idCaja ($idPalet) -> $cajaDestino ($paletDestino) del magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@MoverProductosCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateCantidadCaja(palet: Int, caja: Int) {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.updateCantidadCaja(
                    CajaUpdateCantidadRequest(
                        almacen = codigoAlmacen,
                        palet = palet,
                        caja = caja
                    )
                )

                Toast.makeText(this@MoverProductosCaja,response.message, Toast.LENGTH_SHORT).show()

                finish()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@MoverProductosCaja,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
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

    override fun onItemClick(almacenVirtual: MoverProducto) {}

}
