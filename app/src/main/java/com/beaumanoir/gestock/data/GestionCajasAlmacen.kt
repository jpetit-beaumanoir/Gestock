package com.beaumanoir.gestock.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.DatabaseHelper
import com.beaumanoir.gestock.data.sqlite.ProductoAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import java.util.UUID

class GestionCajasAlmacen : AppCompatActivity(), ProductoAdapter.OnItemClickListener {

    private var bluetoothSocket: BluetoothSocket? = null
    private var codigoAlmacen: Int = 0
    private var collectionStock: Collection<AlmacenVirtualProducto> = emptyList()
    private lateinit var device: BluetoothDevice
    private lateinit var editViewDescripcion: EditText
    private lateinit var editViewTemporada: EditText
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalProductosCaja: TextView
    private var productList: MutableList<AlmacenVirtualProducto> = ArrayList()
    private val db = DatabaseHelper(this)
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gestion_cajas_almacen)

        val textView = findViewById<TextView>(R.id.titulo_gestion_cajas_almacen)
        editViewDescripcion = findViewById(R.id.descripcion_caja)
        editViewTemporada = findViewById(R.id.temporada_caja)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)
        textView.text = "ARTÍCULOS PALET $idPalet CAJA $idCaja"
        getDescTempAPI()

        recyclerView = findViewById(R.id.mostrar_productos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        totalProductosCaja = findViewById(R.id.total_productos_caja)

        findViewById<AppCompatButton>(R.id.boto_opcions_caixa).setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.menu_opcions_caixa, null)
            bottomSheetDialog.setContentView(view)

            view.findViewById<AppCompatButton>(R.id.eliminar_productos_caja).setOnClickListener {
                val intent = Intent(this, EliminarProductoCaja::class.java)
                intent.putExtra("caja", idCaja)
                intent.putExtra("palet", idPalet)
                intent.putExtra("almacen", codigoAlmacen)
                intent.putExtra("stock", ArrayList(collectionStock))
                startActivity(intent)
                bottomSheetDialog.hide()
            }
            view.findViewById<AppCompatButton>(R.id.anadir_productos_caja).setOnClickListener {
                val intent = Intent(this, AñadirProductoCaja::class.java)
                intent.putExtra("caja", idCaja)
                intent.putExtra("palet", idPalet)
                intent.putExtra("almacen", codigoAlmacen)
                startActivity(intent)
                bottomSheetDialog.hide()
            }
            view.findViewById<AppCompatButton>(R.id.mover_productos_caja).setOnClickListener {
                val intent = Intent(this, MoverProductosCaja::class.java)
                intent.putExtra("caja", idCaja)
                intent.putExtra("palet", idPalet)
                intent.putExtra("almacen", codigoAlmacen)
                intent.putExtra("stock", ArrayList(collectionStock))
                startActivity(intent)
                bottomSheetDialog.hide()
            }

            view.findViewById<AppCompatButton>(R.id.imprimir_etiqueta_caja).setOnClickListener {
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
                            ^FD${"%04d".format(codigoAlmacen)}${"%04d".format(idPalet)}${"%04d".format(idCaja)}^FS

                            ^FO0,80
                            ^A0N,20,20
                            ^FB300,1,0,C
                            ^FD${"%04d".format(codigoAlmacen)}${"%04d".format(idPalet)}${"%04d".format(idCaja)}^FS

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
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        bottomSheetDialog.hide()
                    }
                }
            }
            bottomSheetDialog.show()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun buscarBluetoothPrinters() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "EL DISPOSITIU NO SUPORTA BLUETOOTH", Toast.LENGTH_SHORT).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(this, "ACTIVA EL BLUETOOTH", Toast.LENGTH_SHORT).show()
            return
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
            Log.d("BluetoothCheck", "No hay dispositivos emparejados.")
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
            Log.e("Error conexión", "No se pudo conectar a la impresora: ${e.message}")
            cerrarConexionBluetooth()
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
            Toast.makeText(this, "Error al imprimir: $e", Toast.LENGTH_SHORT).show()
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

    private fun getDescTempAPI() {
        RetrofitClient.getApiService(this)
            .getDescTempCaja(codigoAlmacen, idPalet, idCaja)
            .enqueue(object : Callback<DescTempCajaResponse> {
                override fun onResponse(call: Call<DescTempCajaResponse>, response: Response<DescTempCajaResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            editViewTemporada.hint = body.temporada
                            editViewDescripcion.hint = body.descripcion
                        }
                    } else {
                        Toast.makeText(this@GestionCajasAlmacen, "Error al obtener los almacenes", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<DescTempCajaResponse>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@GestionCajasAlmacen, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@GestionCajasAlmacen, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun updateCajaDescTempAPI(descripcion: String, temporada: String = "SIN TEMPORADA", callback: APIResponseCallback) {
        RetrofitClient.getApiService(this)
            .updateDescTempCaja(codigoAlmacen, idPalet, idCaja, descripcion, temporada)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body()?.string() ?: "Operación exitosa")
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        callback.onError("Error ${response.code()}: $errorString")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        callback.onError("No tienes conexión a internet")
                    } else {
                        callback.onError("Error: " + t.message)
                    }
                }
            })
    }

    private fun getStockCajaAPI() {
        RetrofitClient.getApiService(this)
            .getStockCaja(codigoAlmacen, idPalet, idCaja)
            .enqueue(object : Callback<StockCajaResponse> {
                override fun onResponse(call: Call<StockCajaResponse>, response: Response<StockCajaResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()!!
                        if (body.stock.isNotEmpty()) {
                            collectionStock = body.stock.map { (ean, stock) ->
                                AlmacenVirtualProducto(
                                    ean, stock.id, stock.color, stock.talla,
                                    stock.nombre, stock.temporada, stock.cantidad, 0
                                )
                            }
                            productList.addAll(collectionStock)
                            productoAdapter = ProductoAdapter(productList, this@GestionCajasAlmacen)
                            recyclerView.adapter = productoAdapter
                            recyclerView.scheduleLayoutAnimation()
                            recyclerView.layoutParams.height = if (productList.size > 4) 1140 else -2
                            productoAdapter.notifyDataSetChanged()
                            totalProductosCaja.text = "TOTAL: ${body.total}"
                        }
                    }
                }

                override fun onFailure(call: Call<StockCajaResponse>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@GestionCajasAlmacen, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@GestionCajasAlmacen, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        recyclerView = findViewById(R.id.mostrar_productos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productList.clear()
        getStockCajaAPI()
        totalProductosCaja.text = "TOTAL: ${productList.size}"
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val editText = findViewById<EditText>(R.id.descripcion_caja)
        val editText2 = findViewById<EditText>(R.id.temporada_caja)
        val temporada: String
        if (editText2.text.toString().isEmpty()) {
            if (editText2.hint == "TEMP") {
                editText2.hint = "SIN TEMPORADA"
            }
            temporada = editText2.hint.toString()
        } else {
            editText2.hint = editText2.text
            temporada = editText2.hint.toString()
        }
        if (editText.text.toString().isEmpty()) {
            if (editText.hint == "DESCRIPCIÓN") {
                editText.hint = "SIN DESCRIPCIÓN"
            }
            editText.setText(editText.hint)
        } else {
            editText.hint = editText.text
        }
        updateCajaDescTempAPI(
            editText.text.toString().uppercase(Locale.ROOT),
            temporada.uppercase(Locale.ROOT),
            object : APIResponseCallback {
                override fun onSuccess(response: String) {}
                override fun onError(errorMessage: String) {
                    Toast.makeText(this@GestionCajasAlmacen, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        )
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        cerrarConexionBluetooth()
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualProducto) {
        TODO("Not yet implemented")
    }
}
