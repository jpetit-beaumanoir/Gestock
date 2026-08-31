package com.beaumanoir.gestock.ui.cajas

import android.Manifest
import android.animation.ObjectAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.adapters.CajaAdapter
import com.beaumanoir.gestock.data.models.caja.Caja
import com.beaumanoir.gestock.data.remote.dto.caja.CajaCreateRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajaDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.caja.CajasGetRequest
import com.beaumanoir.gestock.data.repository.CajaRepository
import com.beaumanoir.gestock.ui.stock.StockSearch
import com.beaumanoir.gestock.ui.stock.GestionCajasAlmacen
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class AlmacenVirtualMainCajas : AppCompatActivity(), CajaAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var cajaAdapter: CajaAdapter
    private var codigoAlmacen: Int = 0
    private lateinit var nombreAlmacen: String
    private lateinit var device: BluetoothDevice
    private var idPalet: Int = 0
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var recyclerView: RecyclerView

    private lateinit var collectionCajas: List<Caja>
    private var cajasList: MutableList<Caja> = ArrayList()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var textViewImportando: TextView
    private lateinit var progresBarImportando: ProgressBar
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val cajaRepository: CajaRepository by lazy {
        (application as GestockApp).cajaRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.almacen_virtual_main_cajas)

        codigoAlmacen = intent.getIntExtra("codigo_almacen", 0)
        nombreAlmacen = intent.getStringExtra("nombre_almacen").toString().uppercase()
        idPalet = intent.getIntExtra("palet", 0)

        findViewById<TextView>(R.id.numero_palet).text = "CAJAS PALET $idPalet"

        textViewImportando = findViewById(R.id.textview_importando)
        progresBarImportando = findViewById(R.id.progresbar_importando)

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_cajas)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(refreshButton, "rotation", 0.0f, 360.0f).apply {
                duration = 700L
                start()
            }
            getCajas()
        }

        // ============================================= CODI MENU LATERAL ============================================= //
        val navigationView = findViewById<NavigationView>(R.id.nav_menu)
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.nombre_almacen_navigation).text = nombreAlmacen
        headerView.findViewById<TextView>(R.id.codigo_almacen_navigation).text = codigoAlmacen.toString()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearlayout_almacen_virtual_main_cajas)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout_almacen_virtual_main_cajas)
        val botonMenu = findViewById<AppCompatButton>(R.id.boton_menu)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, null,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close

        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
        botonMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
        // ============================================================================================================= //

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

                deleteCaja(caja.caja)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        findViewById<AppCompatButton>(R.id.anadir_caja).setOnClickListener {

            createCaja()

        }
    }

    override fun onItemClick(almacenVirtual: Caja) {
        val intent = Intent(this, GestionCajasAlmacen::class.java)
        intent.putExtra("palet", idPalet)
        intent.putExtra("caja", almacenVirtual.caja)
        intent.putExtra("almacen", codigoAlmacen)
        cerrarConexionBluetooth()
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        getCajas()
    }

    override fun onDestroy() {
        super.onDestroy()
        cerrarConexionBluetooth()
    }

    private fun dialegImprimirEtiqueta(idCaja: Int) {
        AlertDialog.Builder(this)
            .setTitle("Imprimir etiqueta de la caja")
            .setItems(arrayOf("Si", "No")) { _, which ->
                if (which == 0) {
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

    private fun getCajas() {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.getCajas(
                    almacen = codigoAlmacen,
                    palet = idPalet
                )

                val cajas = response.cajas

                collectionCajas =
                    if (cajas.isEmpty()){
                        Toast.makeText(
                            this@AlmacenVirtualMainCajas,
                            "No se encontraron palets",
                            Toast.LENGTH_LONG
                        ).show()

                        emptyList()
                    } else {
                        cajas.map { (caja, datos) ->
                            Caja(
                                caja,
                                datos.temporada,
                                datos.descripcion,
                                datos.cantidad
                            )
                        }
                    }
                cajasList.clear()
                cajasList.addAll(collectionCajas)

                cajaAdapter.notifyDataSetChanged()
                recyclerView.scheduleLayoutAnimation()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error obtenint caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainCajas,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createCaja() {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.createCaja(
                    CajaCreateRequest(
                        almacen = codigoAlmacen,
                        palet = idPalet
                    )
                )

                Toast.makeText(this@AlmacenVirtualMainCajas,response.message, Toast.LENGTH_SHORT).show()

                dialegImprimirEtiqueta(response.message.split(" ")[4].toInt())

                getCajas()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error creant caixes del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainCajas,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteCaja(caja: Int) {

        lifecycleScope.launch {
            try {

                val response = cajaRepository.deleteCaja(
                    almacen = codigoAlmacen,
                    palet = idPalet,
                    caja = caja
                )

                Toast.makeText(this@AlmacenVirtualMainCajas,response.message, Toast.LENGTH_SHORT).show()
                getCajas()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error eliminant la caixa $caja del palet $idPalet magatzem $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainCajas,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_almacen_virtual_main_cajas)
        when (item.itemId) {
            R.id.boton_exportar_stock -> {
                val intent = Intent(this@AlmacenVirtualMainCajas, StockSearch::class.java)
                intent.putExtra("codigo_almacen", codigoAlmacen)
                intent.putExtra("nombre_almacen", nombreAlmacen)
                startActivity(intent)
            }

            R.id.boton_importar_catalogo -> {
                Toast.makeText(this,"Boto deshabilitat", Toast.LENGTH_SHORT).show()

                /*if (checkPermissions()) {
                    val file = File("/storage/emulated/0/Download").listFiles()
                        ?.firstOrNull { it.isFile && it.name.startsWith("Catalogo") }
                    if (file != null) {
                        try {
                            val brand = file.name.split(".")[0].split("_")[1]
                            if (brand.isNotEmpty()) {
                                progresBarImportando.visibility = View.VISIBLE
                                textViewImportando.visibility = View.VISIBLE
                                textViewImportando.text = "Importando ${file.name}"
                                uploadCSV(file, brand)
                            } else {
                                Toast.makeText(this, "No se pudo extraer la marca del archivo", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error al procesar el archivo: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "No se encontró un archivo de catálogo válido", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Se requieren permisos para acceder al almacenamiento", Toast.LENGTH_LONG).show()
                }*/
            }

            R.id.boton_asignar_stock -> {
                Toast.makeText(this,"Boto deshabilitat", Toast.LENGTH_SHORT).show()

                /*val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists() || !downloads.isDirectory) {
                    Toast.makeText(this, "No se ha encontrado la carpeta ${downloads.name}", Toast.LENGTH_LONG).show()
                }
                val files = downloads.listFiles { _, name ->
                    name.endsWith(".txt", true) && name.length == 16 &&
                            name.substring(0, 4).toInt() == codigoAlmacen
                } ?: emptyArray()
                if (files.isEmpty()) {
                    Toast.makeText(this, "No se han encontrado archivos para este almacén", Toast.LENGTH_LONG).show()
                    return false
                }
                for (file in files) {
                    try {
                        val almacen = file.name.substring(0, 4).toInt()
                        val palet = file.name.substring(4, 8).toInt()
                        val caja = file.name.substring(8, 12).toInt()
                        if (almacen == codigoAlmacen) {
                            try {
                                Menu.checkCajaExists(this, almacen, palet, caja, file.readLines(), file)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Error procesando archivo ${file.name}: $e", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        // Nombre de archivo con formato inesperado: se ignora.
                    }
                }*/
            }
        }
        drawerLayout.closeDrawer(GravityCompat.END)
        return true
    }

    /*private fun uploadCSV(file: File, brandName: String) {
        val csvPart = MultipartBody.Part.createFormData(
            "csv_file",
            file.name,
            file.asRequestBody("text/csv".toMediaTypeOrNull())
        )
        val brandBody = brandName.toRequestBody("text/plain".toMediaTypeOrNull())
        GestockApiFactory.getApi(this).subirCatalogo(csvPart, brandBody)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    textViewImportando.visibility = View.GONE
                    progresBarImportando.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "Respuesta: ${response.body()?.string()}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "No hay productos a insertar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    textViewImportando.visibility = View.GONE
                    progresBarImportando.visibility = View.GONE
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainCajas, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }*/

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
            return true
        }
        ActivityCompat.requestPermissions(this, arrayOf("android.permission.READ_EXTERNAL_STORAGE"), REQUEST_CODE_PERMISSIONS)
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == 0) {
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
            return
        }

        super.onBackPressed()
    }
}