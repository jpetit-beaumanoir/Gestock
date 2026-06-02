package com.beaumanoir.gestock.data

import android.animation.ObjectAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.CajaAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import java.util.UUID

class AlmacenVirtualMainCajas : AppCompatActivity(), CajaAdapter.OnItemClickListener {

    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var cajaAdapter: CajaAdapter
    private var codigoAlmacen: Int = 0
    private lateinit var device: BluetoothDevice
    private var idPalet: Int = 0
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var recyclerView: RecyclerView
    private var cajasList: MutableList<AlmacenVirtualCaja> = ArrayList()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.almacen_virtual_main_cajas)

        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_cajas)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(refreshButton, "rotation", 0.0f, 360.0f).apply {
                duration = 700L
                start()
            }
            getCajasAPI()
        }

        findViewById<TextView>(R.id.numero_palet).text = "CAJAS PALET $idPalet"

        recyclerView = findViewById(R.id.mostrar_cajas_almacen)
        recyclerView.layoutManager = LinearLayoutManager(this)
        cajaAdapter = CajaAdapter(cajasList, this)
        recyclerView.adapter = cajaAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, 4) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val caja = cajasList[position]
                if (RetrofitClient.isConnectedToInternet(this@AlmacenVirtualMainCajas)) {
                    deleteCajaAPI(caja.caja, object : APIResponseCallback {
                        override fun onSuccess(response: String) {
                            getCajasAPI()
                            adjustRecyclerViewHeight()
                        }

                        override fun onError(errorMessage: String) {
                            cajaAdapter.notifyItemChanged(position)
                            Toast.makeText(this@AlmacenVirtualMainCajas, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(this@AlmacenVirtualMainCajas, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
        adjustRecyclerViewHeight()

        findViewById<AppCompatButton>(R.id.anadir_caja).setOnClickListener {
            if (RetrofitClient.isConnectedToInternet(this)) {
                createCajaAPI(object : APIResponseCallback {
                    override fun onSuccess(response: String) {
                        dialegImprimirEtiqueta(response.split(" ")[1].toInt())
                        getCajasAPI()
                    }

                    override fun onError(errorMessage: String) {
                        Toast.makeText(this@AlmacenVirtualMainCajas, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
            }
            cajaAdapter.notifyDataSetChanged()
            adjustRecyclerViewHeight()
        }
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualCaja) {
        val intent = Intent(this, GestionCajasAlmacen::class.java)
        intent.putExtra("palet", idPalet)
        intent.putExtra("caja", almacenVirtual.caja)
        intent.putExtra("almacen", codigoAlmacen)
        cerrarConexionBluetooth()
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (RetrofitClient.isConnectedToInternet(this)) {
            getCajasAPI()
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }
        cajaAdapter.notifyDataSetChanged()
        adjustRecyclerViewHeight()
    }

    override fun onDestroy() {
        super.onDestroy()
        cerrarConexionBluetooth()
    }

    private fun adjustRecyclerViewHeight() {
        recyclerView.layoutParams.height = if (cajasList.size > 6) 1200 else -2
    }

    private fun dialegImprimirEtiqueta(idCaja: Int) {
        AlertDialog.Builder(this)
            .setTitle("Imprimir etiqueta de la caja")
            .setItems(arrayOf("Si", "No")) { _, which ->
                if (which == 0) {
                    solicitarPermisos()
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
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT",
                    "android.permission.ACCESS_FINE_LOCATION"
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.ACCESS_FINE_LOCATION"),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            for (result in grantResults) {
                if (result != -1) {
                    return
                }
            }
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    fun buscarBluetoothPrinters() {
        bluetoothSocket?.close()
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

    private fun printData(data: ByteArray) {
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

    private fun getCajasAPI() {
        RetrofitClient.getApiService(this).getCajas(codigoAlmacen, idPalet)
            .enqueue(object : Callback<CajasResponse> {
                override fun onResponse(call: Call<CajasResponse>, response: Response<CajasResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body == null) {
                            cajasList.clear()
                        } else {
                            val nuevas = body.cajas.map { (caja, datos) ->
                                AlmacenVirtualCaja(caja, datos.temporada, datos.descripcion, datos.cantidad)
                            }
                            cajasList.clear()
                            cajasList.addAll(nuevas)
                            cajaAdapter = CajaAdapter(cajasList, this@AlmacenVirtualMainCajas)
                            recyclerView.adapter = cajaAdapter
                        }
                        recyclerView.scheduleLayoutAnimation()
                        adjustRecyclerViewHeight()
                    } else if (response.code() != 404) {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "Error al obtener las cajas", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<CajasResponse>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun createCajaAPI(callback: APIResponseCallback) {
        RetrofitClient.getApiService(this).createCaja(codigoAlmacen, idPalet)
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

    private fun deleteCajaAPI(caja: Int, callback: APIResponseCallback) {
        RetrofitClient.getApiService(this).deleteCaja(codigoAlmacen, idPalet, caja)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body()?.string() ?: "Operación exitosa")
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        callback.onError(JSONObject(errorString).getString("detail"))
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
}
