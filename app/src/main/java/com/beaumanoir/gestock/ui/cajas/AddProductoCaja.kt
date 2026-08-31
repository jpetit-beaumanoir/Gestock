package com.beaumanoir.gestock.ui.cajas

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.adapters.ProductoAdapter
import com.beaumanoir.gestock.data.models.producto.Producto
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateCantidadRequest
import com.beaumanoir.gestock.data.remote.dto.producto.ProductoGetRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockAddRequest
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.data.repository.ProductoRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AddProductoCaja : AppCompatActivity(), ProductoAdapter.OnItemClickListener {

    private var codigoAlmacen: Int = 0
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var productAddAdapter: ProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var collectionProducto: List<Producto>
    private val productoList: MutableList<Producto> = ArrayList()
    private val eansAfegirList: MutableList<String> = ArrayList()
    private lateinit var eanEditText: EditText
    private lateinit var totalProductosTextView: TextView
    private lateinit var botonConfirmar: AppCompatButton
    private lateinit var botonCancelar: AppCompatButton

    private val stockRepository: StockRepository by lazy {
        (application as GestockApp).stockRepository
    }

    private val cajaRepository: CajaRepository by lazy {
        (application as GestockApp).cajaRepository
    }

    private val productoRepository: ProductoRepository by lazy {
        (application as GestockApp).productoRepository
    }

    override fun onItemClick(almacenVirtual: Producto) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.anadir_producto_caja)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)

        totalProductosTextView = findViewById<TextView>(R.id.total_productos_anadir)
        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)
        findViewById<TextView>(R.id.titulo_pantalla_anadir).text =
            "AÑADIR ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_anadir)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productAddAdapter = ProductoAdapter(productoList,this)
        recyclerView.adapter = productAddAdapter

        botonCancelar = findViewById(R.id.cancelar_adicion_productos)
        botonConfirmar = findViewById(R.id.confirmar_adicion_productos)

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, 12) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                productoList.removeAt(position)
                //eansAfegirList.removeAt(position)
                productAddAdapter.notifyItemRemoved(position)

                findViewById<TextView>(R.id.total_productos_anadir).text =
                    "TOTAL: ${eansAfegirList.size}"
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        eanEditText = findViewById(R.id.ean_producto_anadir)
        eanEditText.requestFocus()
        eanEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 0 || actionId == 4 || actionId == 6) {
                eanEditText.text.clear()
                eanEditText.requestFocus()
                true
            } else {
                false
            }
        }
        eanEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                eanEditText.requestFocus()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length == 13) {
                    getValuesfromProducto(s.toString())
                }
                eanEditText.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
                eanEditText.requestFocus()
            }
        })

        botonCancelar.setOnClickListener {
            if (eansAfegirList.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("¿SEGURO QUE QUIERES CANCELAR?")
                    .setCancelable(false)
                    .setPositiveButton("Sí") { _, _ -> finish() }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            } else {
                finish()
            }
        }

        botonConfirmar.setOnClickListener {
            if (eansAfegirList.isNotEmpty()) {
                addStock(eansAfegirList)
            }
            finish()
        }
    }

    private fun getValuesfromProducto(ean: String) {

        lifecycleScope.launch {
            try {

                val response = productoRepository.getProductValues(
                    ean = ean,
                    codemag = codigoAlmacen
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

    private fun addStock(eans: List<String>) {

        lifecycleScope.launch {
            try {

                val response = stockRepository.addStock(
                    StockAddRequest(
                        almacen = codigoAlmacen,
                        palet = idPalet,
                        caja = idCaja,
                        eans = eans
                    )
                )

                Toast.makeText(this@AddProductoCaja,response.message, Toast.LENGTH_SHORT).show()

                updateCantidadCaja()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error creando palets del almacén $codigoAlmacen",
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

                Toast.makeText(this@AddProductoCaja,response.message, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error creando palets del almacén $codigoAlmacen",
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

    override fun onBackPressed() {

        botonCancelar.callOnClick()

        super.onBackPressed()
    }

}
