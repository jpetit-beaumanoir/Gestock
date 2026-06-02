package com.beaumanoir.gestock.data

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StockSearch : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private lateinit var autoCompleteFamilia: AutoCompleteTextView
    private var codigoAlmacen: Int = 0
    private lateinit var colorEntrado: EditText
    private lateinit var eanEntrado: EditText
    private lateinit var nombreEntrado: EditText
    private lateinit var progresBarImportando: ProgressBar
    private lateinit var tallaEntrada: EditText
    private lateinit var temporadaEntrada: EditText
    private lateinit var textViewImportando: TextView

    interface APIResponseCallback<T> {
        fun onError(errorMessage: String)
        fun onSuccess(response: List<T>)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stock_search)

        codigoAlmacen = intent.getIntExtra("codigo_almacen", 0)
        val nombreAlmacen = intent.getStringExtra("nombre_almacen")

        textViewImportando = findViewById(R.id.textview_importando)
        progresBarImportando = findViewById(R.id.progresbar_importando)

        findViewById<TextView>(R.id.nombre_almacen).text = nombreAlmacen?.uppercase(Locale.ROOT)

        val navigationView = findViewById<NavigationView>(R.id.nav_menu)
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.nombre_almacen_navigation).text = nombreAlmacen
        headerView.findViewById<TextView>(R.id.codigo_almacen_navigation).text = codigoAlmacen.toString()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.stocksearch)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eanEntrado = findViewById(R.id.ean_entrado)
        tallaEntrada = findViewById(R.id.talla_entrada)
        colorEntrado = findViewById(R.id.color_entrado)
        temporadaEntrada = findViewById(R.id.temporada_entrada)
        nombreEntrado = findViewById(R.id.nombre_entrado)
        autoCompleteFamilia = findViewById(R.id.auto_complete_familia)
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_familia)

        if (RetrofitClient.isConnectedToInternet(this)) {
            getFamiliasAPI()
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }

        val selectedFamilias = ArrayList<String>()
        autoCompleteFamilia.setOnItemClickListener { adapterView, _, position, _ ->
            val familia = adapterView.getItemAtPosition(position).toString()
            if (!selectedFamilias.contains(familia)) {
                selectedFamilias.add(familia)
                addChipToGroup(familia, chipGroup, selectedFamilias)
            }
            autoCompleteFamilia.text.clear()
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
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

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerOpened(drawerView: View) {
                eanEntrado.isFocusable = false
                tallaEntrada.isFocusable = false
                colorEntrado.isFocusable = false
                temporadaEntrada.isFocusable = false
                nombreEntrado.isFocusable = false
                autoCompleteFamilia.isEnabled = false
            }

            override fun onDrawerClosed(drawerView: View) {
                eanEntrado.isFocusable = true
                tallaEntrada.isFocusable = true
                colorEntrado.isFocusable = true
                temporadaEntrada.isFocusable = true
                nombreEntrado.isFocusable = true
                autoCompleteFamilia.isFocusable = true
                eanEntrado.isFocusableInTouchMode = true
                tallaEntrada.isFocusableInTouchMode = true
                colorEntrado.isFocusableInTouchMode = true
                temporadaEntrada.isFocusableInTouchMode = true
                nombreEntrado.isFocusableInTouchMode = true
                autoCompleteFamilia.isFocusableInTouchMode = true
                autoCompleteFamilia.isEnabled = true
            }
        })

        findViewById<AppCompatButton>(R.id.boton_exportar_resultados).setOnClickListener {
            val ean = eanEntrado.text.toString()
            val talla = tallaEntrada.text.toString()
            val color = colorEntrado.text.toString()
            val temporada = temporadaEntrada.text.toString()
            val nombre = nombreEntrado.text.toString()
            val familias = selectedFamilias.joinToString(",")
            if (RetrofitClient.isConnectedToInternet(this)) {
                exportFilteredSearchAPI(
                    codigoAlmacen, ean, talla, nombre, familias, color, temporada,
                    object : APIResponseCallback<FilteredSearch> {
                        override fun onSuccess(response: List<FilteredSearch>) {
                            if (response.isNotEmpty()) {
                                exportarBusqueda(response)
                            } else {
                                Toast.makeText(this@StockSearch, "No se han encontrado resultados para esta búsqueda", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onError(errorMessage: String) {
                            Toast.makeText(this@StockSearch, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<AppCompatButton>(R.id.boton_almacen_virtual).setOnClickListener {
            val intent = Intent(this, AlmacenVirtualMainPalets::class.java)
            intent.putExtra("codigo_almacen", codigoAlmacen)
            intent.putExtra("nombre_almacen", nombreAlmacen)
            startActivity(intent)
        }
    }

    private fun addChipToGroup(familia: String, chipGroup: ChipGroup, selectedFamilias: MutableList<String>) {
        val chip = Chip(this)
        chip.text = familia
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            selectedFamilias.remove(familia)
            chipGroup.removeView(chip)
        }
        chipGroup.addView(chip)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
            return
        }
        val intent = Intent(this, LogIn::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.boton_exportar_stock -> {
                exportStockSearchAPI(codigoAlmacen, object : APIResponseCallback<FilteredSearch> {
                    override fun onSuccess(response: List<FilteredSearch>) {
                        if (response.isNotEmpty()) {
                            exportarBusqueda(response)
                        } else {
                            Toast.makeText(this@StockSearch, "No se han encontrado resultados para esta búsqueda", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onError(errorMessage: String) {
                        Toast.makeText(this@StockSearch, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
            }

            R.id.boton_importar_catalogo -> {
                if (checkPermissions()) {
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
                }
            }

            R.id.boton_asignar_stock -> {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
                                checkCajaExists(this, almacen, palet, caja, file.readLines(), file)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Error procesando archivo ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        // Nombre de archivo con formato inesperado: se ignora.
                    }
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.END)
        return true
    }

    private fun checkCajaExists(context: Context, almacen: Int, palet: Int, caja: Int, eans: List<String>, file: File) {
        RetrofitClient.getApiService(context).existCaja(almacen, palet, caja)
            .enqueue(object : Callback<CajaExiste> {
                override fun onResponse(call: Call<CajaExiste>, response: Response<CajaExiste>) {
                    if (response.isSuccessful) {
                        checkStockExists(almacen, palet, caja, eans, context, file)
                    } else {
                        Toast.makeText(context, "Caja $caja del palet $palet, no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CajaExiste>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(context, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun checkStockExists(almacen: Int, palet: Int, caja: Int, eans: List<String>, context: Context, file: File) {
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
            getValuesfromProductoAPI(ean, context, object : AñadirProductoCaja.APIResponseCallback {
                override fun onSuccess(result: ProductValues) {
                    if (stop) return
                    processed++
                    if (processed != distinct.size || stop) return
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

    private fun getValuesfromProductoAPI(ean: String, context: Context, callback: AñadirProductoCaja.APIResponseCallback) {
        RetrofitClient.getApiService(context).getProductValues(ean, codigoAlmacen)
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
                            callback.onError("Error: $errorString")
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
        RetrofitClient.getApiService(context).getStockCaja(almacen, palet, caja)
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
                                    deleteStockAPI(productIds, palet, caja)
                                }
                                addStockAPI(eans, palet, caja)
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

    private fun addStockAPI(eans: List<String>, palet: Int, caja: Int) {
        RetrofitClient.getApiService(this)
            .addStock(AddStockRequest(codigoAlmacen, palet, caja, eans))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI(palet, caja)
                        Toast.makeText(applicationContext, "${eans.size} Productos añadidos", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@StockSearch, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@StockSearch, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StockSearch, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun deleteStockAPI(deleteIds: List<Int>, palet: Int, caja: Int) {
        RetrofitClient.getApiService(this)
            .deleteStock(DeleteStockRequest(codigoAlmacen, palet, caja, deleteIds))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI(palet, caja)
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@StockSearch, JSONObject(errorString).getString("detail"), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@StockSearch, "No tienes conexión a internet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@StockSearch, "Error: " + t.message, Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@StockSearch, "Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@StockSearch, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StockSearch, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

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

    private fun getFamiliasAPI() {
        RetrofitClient.getApiService(this).getFamilias(codigoAlmacen)
            .enqueue(object : Callback<List<Familia>> {
                override fun onResponse(call: Call<List<Familia>>, response: Response<List<Familia>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            val nombres = body.map { it.nombre }
                            val adapter = ArrayAdapter(this@StockSearch, android.R.layout.simple_dropdown_item_1line, nombres)
                            autoCompleteFamilia.setAdapter(adapter)
                            autoCompleteFamilia.threshold = 1
                        } else {
                            Toast.makeText(this@StockSearch, "No se encontraron familias", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@StockSearch, "Error al obtener las familias", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<List<Familia>>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@StockSearch, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StockSearch, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun exportFilteredSearchAPI(
        almacen: Int, ean: String, talla: String, nombre: String,
        familia: String, color: String, temporada: String,
        callback: APIResponseCallback<FilteredSearch>
    ) {
        RetrofitClient.getApiService(this)
            .filteredSearch(almacen, ean, talla, nombre, familia, color, temporada)
            .enqueue(object : Callback<List<FilteredSearch>> {
                override fun onResponse(call: Call<List<FilteredSearch>>, response: Response<List<FilteredSearch>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            callback.onSuccess(body)
                        } else {
                            callback.onError("No se encontraron productos")
                        }
                    } else {
                        callback.onError("Error al obtener los productos")
                    }
                }

                override fun onFailure(call: Call<List<FilteredSearch>>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        callback.onError("No tienes conexión a internet")
                    } else {
                        callback.onError("Error: " + t.message)
                    }
                }
            })
    }

    private fun exportStockSearchAPI(almacen: Int, callback: APIResponseCallback<FilteredSearch>) {
        RetrofitClient.getApiService(this).stockExport(almacen)
            .enqueue(object : Callback<List<FilteredSearch>> {
                override fun onResponse(call: Call<List<FilteredSearch>>, response: Response<List<FilteredSearch>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            callback.onSuccess(body)
                        } else {
                            callback.onError("No se encontraron productos")
                        }
                    } else {
                        callback.onError("Error al obtener los productos")
                    }
                }

                override fun onFailure(call: Call<List<FilteredSearch>>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        callback.onError("No tienes conexión a internet")
                    } else {
                        callback.onError("Error: " + t.message)
                    }
                }
            })
    }

    private fun alertInsertarNombre(onNombreEntered: (String) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.alert_exportar_stock, null)
        val editText = view.findViewById<EditText>(R.id.nombre_archivo_exportar)
        editText.requestFocus()
        val button = view.findViewById<Button>(R.id.boton_confirmar_nombre)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setTitle("Introduce el nombre del archivo")
            .create()
        button.setOnClickListener {
            val nombre = editText.text.toString()
            if (nombre.isNotEmpty()) {
                onNombreEntered(nombre)
                dialog.dismiss()
            } else {
                editText.error = "El nombre no puede estar vacío"
            }
        }
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != 6) {
                false
            } else {
                val nombre = editText.text.toString()
                if (nombre.isNotEmpty()) {
                    onNombreEntered(nombre)
                    dialog.dismiss()
                } else {
                    editText.error = "El nombre no puede estar vacío"
                }
                true
            }
        }
        dialog.show()
    }

    private fun exportarBusqueda(productos: List<FilteredSearch>) {
        alertInsertarNombre { nombreFile ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val headers = listOf(
                        "EAN", "Palet", "Caja", "Desc Caja", "Temp Caja", "Almacen",
                        "Talla", "Nombre", "Familia", "Subfamilia", "Color", "Temporada",
                        "PVP", "PRMP", "Marca"
                    )
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"))
                    val outputFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "${nombreFile}_$timestamp.csv"
                    )
                    outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(headers.joinToString(";") + "\n")
                        for (producto in productos) {
                            val campos = listOf(
                                producto.ean, producto.palet, producto.caja, producto.descCaja,
                                producto.tempCaja, producto.almacen, producto.talla, producto.nombre,
                                producto.familia, producto.subfamilia, producto.color, producto.temporada,
                                producto.pvp, producto.prmp, producto.marca
                            )
                            val line = campos.joinToString(";") { it.toString().replace(";", " ") }
                            writer.write(line + "\n")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StockSearch, "CSV exportado correctamente en 'Descargas'", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StockSearch, "Error al exportar CSV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun uploadCSV(file: File, brandName: String) {
        val csvPart = MultipartBody.Part.createFormData(
            "csv_file",
            file.name,
            file.asRequestBody("text/csv".toMediaTypeOrNull())
        )
        val brandBody = brandName.toRequestBody("text/plain".toMediaTypeOrNull())
        RetrofitClient.getApiService(this).subirCatalogo(csvPart, brandBody)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    textViewImportando.visibility = View.GONE
                    progresBarImportando.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@StockSearch, "Respuesta: ${response.body()?.string()}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@StockSearch, "No hay productos a insertar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    textViewImportando.visibility = View.GONE
                    progresBarImportando.visibility = View.GONE
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@StockSearch, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StockSearch, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }
}
