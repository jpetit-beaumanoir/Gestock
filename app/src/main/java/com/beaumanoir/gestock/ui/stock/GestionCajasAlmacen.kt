package com.beaumanoir.gestock.ui.stock

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.database.DatabaseHelper
import com.beaumanoir.gestock.data.local.adapters.ProductoAdapter
import com.beaumanoir.gestock.data.models.producto.Producto
import com.beaumanoir.gestock.data.remote.dto.caja.CajaGetDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaUpdateDescTempRequest
import com.beaumanoir.gestock.data.remote.dto.stock.StockGetRequest
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import com.beaumanoir.gestock.ui.cajas.AddProductoCaja
import com.beaumanoir.gestock.ui.cajas.EliminarProductoCaja
import com.beaumanoir.gestock.ui.cajas.MoverProductosCaja
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import java.util.UUID

class GestionCajasAlmacen : AppCompatActivity(), ProductoAdapter.OnItemClickListener {

    private var bluetoothSocket: BluetoothSocket? = null
    private var codigoAlmacen: Int = 0
    private var collectionStock: Collection<Producto> = emptyList()
    private lateinit var device: BluetoothDevice
    private lateinit var editTextDescripcion: EditText
    private lateinit var editTextTemporada: EditText
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalProductosCaja: TextView
    private var productList: MutableList<Producto> = ArrayList()
    private val db = DatabaseHelper(this)

    private lateinit var textViewTitolActivity: TextView
    private var menuVisible = false
    private var menuAnimando = false
    private lateinit var descripcionActual: String
    private lateinit var temporadaActual: String
    private lateinit var menuOpciones: View
    private lateinit var botonOpcionesCaja: View

    private lateinit var botonEliminarProductosCaja: ImageButton
    private lateinit var botonAnadirProductosCaja: ImageButton
    private lateinit var botonMoverProductosCaja: ImageButton
    private lateinit var botonImprimirEtiquetaCaja: ImageButton
    private lateinit var botonGuardarDescTemp: AppCompatButton
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val cajaRepository: CajaRepository by lazy {
        (application as GestockApp).cajaRepository
    }

    private val stockRepository: StockRepository by lazy {
        (application as GestockApp).stockRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gestion_cajas_almacen)

        // OBTENIR INFORMACIÓ DE LA ACTIVITY ANTERIOR
        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)
        //---------------------------------------------------------------

        // OBTENIR TOTES LES IDs DINTRE DE LA ACTIVITY
        textViewTitolActivity = findViewById(R.id.titulo_gestion_cajas_almacen)

        menuOpciones = findViewById(R.id.menu_inferior_opciones_caja)
        botonOpcionesCaja = findViewById(R.id.boto_opcions_caixa)

        botonEliminarProductosCaja = findViewById(R.id.eliminar_productos_caja)
        botonAnadirProductosCaja = findViewById(R.id.anadir_productos_caja)
        botonMoverProductosCaja = findViewById(R.id.mover_productos_caja)
        botonImprimirEtiquetaCaja = findViewById(R.id.imprimir_etiqueta_caja)

        editTextDescripcion = findViewById(R.id.descripcion_caja)
        editTextTemporada = findViewById(R.id.temporada_caja)

        botonGuardarDescTemp = findViewById(R.id.boton_guardar_desc_temp)
        //---------------------------------------------------------------

        // DEFINIR EL TITOL DE LA ACTIVITY
        textViewTitolActivity.text = "ARTÍCULOS PALET $idPalet CAJA $idCaja"

        // OBTENIR LA DESCRIPCIO I LA TEMPORADA DE LA CAIXA
        getDescTemp()

        editTextDescripcion.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_NEXT) {
                return@setOnEditorActionListener false
            }

            checkNewDescTemp()

            editTextTemporada.requestFocus()

            return@setOnEditorActionListener true

        }

        editTextTemporada.setOnEditorActionListener { v, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }

            checkNewDescTemp()

            val imm = getSystemService(INPUT_METHOD_SERVICE)
                    as InputMethodManager

            imm.hideSoftInputFromWindow(
                v.windowToken,
                0
            )

            return@setOnEditorActionListener true
        }

        botonGuardarDescTemp.setOnClickListener {

            val temporada = editTextTemporada.text.toString()
                .takeIf { it.isNotBlank() }
                ?.uppercase(Locale.ROOT)
                ?: editTextTemporada.hint.toString().uppercase(Locale.ROOT)

            val descripcion = editTextDescripcion.text.toString()
                .takeIf { it.isNotBlank() }
                ?.uppercase(Locale.ROOT)
                ?: editTextDescripcion.hint.toString().uppercase(Locale.ROOT)

            updateCajaDescTemp(
                descripcion = descripcion,
                temporada = temporada
            )
        }


        prepararMenuCerrado()

        botonOpcionesCaja.setOnClickListener {
            if (menuVisible) {
                cerrarMenuFlotante()
            } else {
                abrirMenuFlotante()
            }
        }

        botonEliminarProductosCaja.setOnClickListener {

            cerrarMenuFlotante()

            val intent = Intent(this, EliminarProductoCaja::class.java)
            intent.putExtra("caja", idCaja)
            intent.putExtra("palet", idPalet)
            intent.putExtra("almacen", codigoAlmacen)
            startActivity(intent)
        }

        botonAnadirProductosCaja.setOnClickListener {

            cerrarMenuFlotante()

            val intent = Intent(this, AddProductoCaja::class.java)
            intent.putExtra("caja", idCaja)
            intent.putExtra("palet", idPalet)
            intent.putExtra("almacen", codigoAlmacen)
            startActivity(intent)

            menuOpciones.visibility = View.GONE
            menuVisible = false
            botonOpcionesCaja.rotation = 180f
        }

        botonMoverProductosCaja.setOnClickListener {

            cerrarMenuFlotante()

            val intent = Intent(this, MoverProductosCaja::class.java)
            intent.putExtra("caja", idCaja)
            intent.putExtra("palet", idPalet)
            intent.putExtra("almacen", codigoAlmacen)
            intent.putExtra("stock", ArrayList(collectionStock))
            startActivity(intent)

            menuOpciones.visibility = View.GONE
            menuVisible = false
            botonOpcionesCaja.rotation = 180f
        }

        botonImprimirEtiquetaCaja.setOnClickListener {

            cerrarMenuFlotante()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    buscarBluetoothPrinters()
                    connectToPrinter(device)
                    val zpl = """
                                ^XA
                                ^PW400
                                ^LL400
                                ^LT0
    
                                ^FO50,10
                                ^BY2,2,60
                                ^BCN,60,N,N,N,A
                                ^FD${"%04d".format(codigoAlmacen)}${"%04d".format(idPalet)}${
                        "%04d".format(
                            idCaja
                        )
                    }^FS
    
                                ^FO0,80
                                ^A0N,20,20
                                ^FB300,1,0,C
                                ^FD${"%04d".format(codigoAlmacen)}${"%04d".format(idPalet)}${
                        "%04d".format(
                            idCaja
                        )
                    }^FS
    
                                ^FO0,130
                                ^A0N,40,40
                                ^FB300,1,0,C
                                ^FDalm: $codigoAlmacen^FS
    
                                ^FO0,180
                                ^A0N,40,40
                                ^FB300,1,0,C
                                ^FDpalet: $idPalet^FS
    
                                ^FO0,230
                                ^A0N,40,40
                                ^FB300,1,0,C
                                ^FDcaja: $idCaja^FS
    
                                ^XZ
                                """.trimIndent()
                    printData(zpl.toByteArray(Charsets.UTF_8))
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@GestionCajasAlmacen,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            menuOpciones.visibility = View.GONE
            menuVisible = false
            botonOpcionesCaja.rotation = 180f
        }

        recyclerView = findViewById(R.id.mostrar_productos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        totalProductosCaja = findViewById(R.id.total_productos_caja)

    }

    private fun activarDesactivarBotoGuardar(activar: Boolean) {
        if (activar) {
            botonGuardarDescTemp.isEnabled = true
            botonGuardarDescTemp.isClickable = true
            botonGuardarDescTemp.alpha = 1f
        } else {
            botonGuardarDescTemp.isEnabled = false
            botonGuardarDescTemp.isClickable = false
            botonGuardarDescTemp.alpha = 0.3f
        }
    }

    private fun checkNewDescTemp() {

        val temporada = editTextTemporada.text.toString()
            .takeIf { it.isNotBlank() }
            ?: editTextTemporada.hint.toString()

        val descripcion = editTextDescripcion.text.toString()
            .takeIf { it.isNotBlank() }
            ?: editTextDescripcion.hint.toString()

        val hayCambios =
            !temporada.equals(temporadaActual, true) ||
                    !descripcion.equals(descripcionActual, true)

        activarDesactivarBotoGuardar(hayCambios)
    }

    // FUNCIO CONVERTIR dp A PIXELS
    private fun Int.dp(): Float {
        return this * resources.displayMetrics.density
    }

    private fun prepararMenuCerrado() {

        val botones = listOf(
            botonEliminarProductosCaja,
            botonAnadirProductosCaja,
            botonImprimirEtiquetaCaja,
            botonMoverProductosCaja
        )

        botones.forEach { boton ->
            boton.translationX = 0f
            boton.translationY = 0f
            boton.alpha = 0f
            boton.scaleX = 0.2f
            boton.scaleY = 0.2f
        }

        menuOpciones.visibility = View.GONE
        menuVisible = false
        menuAnimando = false
        botonOpcionesCaja.rotation = 180f
    }

    private fun abrirMenuFlotante() {

        if (menuAnimando) return

        menuAnimando = true
        menuVisible = true

        menuOpciones.visibility = View.VISIBLE
        menuOpciones.alpha = 1f

        animarBotonMenu(
            botonImprimirEtiquetaCaja,
            translationX = (-115).dp(),
            translationY = (-30).dp(),
            delay = 0L
        )

        animarBotonMenu(
            botonEliminarProductosCaja,
            translationX = (-60).dp(),
            translationY = (-100).dp(),
            delay = 0L
        )

        animarBotonMenu(
            botonAnadirProductosCaja,
            translationX = 60.dp(),
            translationY = (-100).dp(),
            delay = 40L
        )

        animarBotonMenu(
            botonMoverProductosCaja,
            translationX = 115.dp(),
            translationY = (-30).dp(),
            delay = 40L
        )

        menuAnimando = false

        botonOpcionesCaja.animate()
            .rotation(0f)
            .setDuration(220)
            .start()
    }

    private fun animarBotonMenu(
        boton: View,
        translationX: Float,
        translationY: Float,
        delay: Long
    ) {
        boton.animate()
            .translationX(translationX)
            .translationY(translationY)
            .alpha( 1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    private fun cerrarMenuFlotante() {

        if (menuAnimando) return

        menuAnimando = true
        menuVisible = false

        val botones = listOf(
            botonEliminarProductosCaja,
            botonAnadirProductosCaja,
            botonImprimirEtiquetaCaja,
            botonMoverProductosCaja
        )

        botones.forEachIndexed { index, boton ->

            boton.animate()
                .translationX(0f)
                .translationY(0f)
                .alpha(0f)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .setDuration(200)
                .setStartDelay((index * 25).toLong())
                .withEndAction {
                    if (index == botones.lastIndex) {
                        menuOpciones.visibility = View.GONE
                        menuAnimando = false
                    }
                }
                .start()
        }

        botonOpcionesCaja.animate()
            .rotation(180f)
            .setDuration(220)
            .start()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun buscarBluetoothPrinters() {
        val adapter = bluetoothAdapter ?: throw Exception("EL DISPOSITIU NO SUPORTA BLUETOOTH")
        if (!adapter.isEnabled) {
            throw Exception("ACTIVA EL BLUETOOTH")
        }

        val bondedDevices = adapter.bondedDevices
        if (bondedDevices.isNotEmpty()) {
            for (bluetoothDevice in bondedDevices) {
                if (bluetoothDevice.name.startsWith("RJ-")) {
                    device = bluetoothDevice
                    return
                }
            }
        } else {
            throw Exception("NO HAY UNA IMPRESORA EMPAREJADA")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToPrinter(device: BluetoothDevice) {
        cerrarConexionBluetooth()
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(printerUUID)
            bluetoothSocket?.connect()
            Log.d("Impresora Conectada", "Conectado a ${device.name}")
        } catch (e: IOException) {
            throw Exception("NO SE PUDO CONECTAR A LA IMPRESORA: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun printData(data: ByteArray) {
        try {
            val socket = bluetoothSocket
            if (socket != null && !socket.isConnected) {
                connectToPrinter(device)
            }
            bluetoothSocket?.outputStream?.write(data)
            bluetoothSocket?.outputStream?.flush()
        } catch (e: IOException) {
            throw Exception("ERROR AL IMPRIMIR: ${e.message}")
        }
    }

    private fun cerrarConexionBluetooth() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.d("Bluetooth", "Conexión cerrada correctamente")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al cerrar la conexión: ${e.message}")
        }
    }

    private fun getDescTemp() {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.getDescTempCaja(
                    almacen = codigoAlmacen,
                    palet = idPalet,
                    caja = idCaja
                )

                descripcionActual = response.descripcion
                temporadaActual = response.temporada

                editTextDescripcion.hint = descripcionActual
                editTextTemporada.hint = temporadaActual


            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@GestionCajasAlmacen,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateCajaDescTemp(descripcion: String, temporada: String = "??") {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.updateDescTempCaja(
                    CajaUpdateDescTempRequest(
                        almacen = codigoAlmacen,
                        palet = idPalet,
                        caja = idCaja,
                        descripcion = descripcion,
                        temporada = temporada
                    )
                )

                Toast.makeText(this@GestionCajasAlmacen,response.message, Toast.LENGTH_SHORT).show()

                editTextDescripcion.hint = descripcion
                editTextTemporada.hint = temporada

                editTextDescripcion.setText("")
                editTextTemporada.setText("")


            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@GestionCajasAlmacen,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
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
                        Producto(
                            ean = ean,
                            color = stock.color,
                            talla = stock.talla,
                            nombre = stock.nombre,
                            temporada = stock.temporada,
                            cantidad = stock.cantidad
                        )
                    }

                    productList.addAll(collectionStock)
                    productoAdapter = ProductoAdapter(productList, this@GestionCajasAlmacen)

                    recyclerView.adapter = productoAdapter
                    recyclerView.scheduleLayoutAnimation()
                    recyclerView.layoutParams.height = if (productList.size > 4) 1140 else -2

                    productoAdapter.notifyDataSetChanged()

                    totalProductosCaja.text = "TOTAL: $total"
                }

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@GestionCajasAlmacen,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        prepararMenuCerrado()
        getStockCaja()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (menuVisible) {
            cerrarMenuFlotante()
            return
        }

        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        cerrarConexionBluetooth()
    }

    override fun onItemClick(almacenVirtual: Producto) {
        TODO("Not yet implemented")
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
