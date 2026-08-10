package com.beaumanoir.gestock.data

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.PaletAdapter
import com.google.android.material.navigation.NavigationView
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
import java.util.Locale

class AlmacenVirtualMainPalets : AppCompatActivity(), PaletAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private var codigoAlmacen: Int = 0
    private lateinit var eanCajaEntrat: EditText
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var paletAdapter: PaletAdapter
    private lateinit var recyclerView: RecyclerView
    private var paletList: MutableList<AlmacenVirtualPalets> = ArrayList()
    private var nombreAlmacen: String = ""
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var textViewImportando: TextView
    private lateinit var progresBarImportando: ProgressBar
    private var recyclerPosition = 0

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.almacen_virtual_main_palets)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)

        codigoAlmacen = intent.getIntExtra("codigo_almacen", 0)
        nombreAlmacen = intent.getStringExtra("nombre_almacen").toString().uppercase()

        textViewImportando = findViewById(R.id.textview_importando)
        progresBarImportando = findViewById(R.id.progresbar_importando)

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_palets)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(refreshButton, "rotation", 0.0f, 360.0f).apply {
                duration = 700L
                start()
            }
            getPaletsAPI()
        }

        findViewById<TextView>(R.id.almacen_nombre).text = "$codigoAlmacen $nombreAlmacen"

        // ============================================= CODI MENU LATERAL ============================================= //
        val navigationView = findViewById<NavigationView>(R.id.nav_menu)
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.nombre_almacen_navigation).text = nombreAlmacen
        headerView.findViewById<TextView>(R.id.codigo_almacen_navigation).text = codigoAlmacen.toString()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearlayout_almacen_virtual_main_palets)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout_almacen_virtual_main_palets)
        val botonMenu = findViewById<AppCompatButton>(R.id.boton_menu)
        val toggle = ActionBarDrawerToggle(
            this,drawerLayout,null,
            R.string.navigation_drawer_open,R.string.navigation_drawer_close

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

        recyclerView = findViewById(R.id.mostrar_palets_almacen)
        recyclerView.layoutManager = LinearLayoutManager(this)
        paletAdapter = PaletAdapter(paletList, this)
        recyclerView.adapter = paletAdapter

        eanCajaEntrat = findViewById(R.id.ean_identificativo)
        eanCajaEntrat.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(eanEntrat: CharSequence, start: Int, before: Int, count: Int) {
                if (eanEntrat.length == 12) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(eanCajaEntrat.windowToken, 0)
                    val almacen = eanEntrat.subSequence(0..3).toString().toInt()
                    val palet = eanEntrat.subSequence(4..7).toString().toInt()
                    val caja = eanEntrat.subSequence(8..11).toString().toInt()
                    RetrofitClient.getApiService(this@AlmacenVirtualMainPalets)
                        .existCaja(almacen, palet, caja)
                        .enqueue(object : Callback<CajaExiste> {
                            override fun onResponse(call: Call<CajaExiste>, response: Response<CajaExiste>) {
                                eanCajaEntrat.setText("")
                                if (!response.isSuccessful) {
                                    mediaPlayer.start()
                                    Toast.makeText(this@AlmacenVirtualMainPalets, "No se ha encontrado la caja especificada", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val intent = Intent(this@AlmacenVirtualMainPalets, GestionCajasAlmacen::class.java)
                                intent.putExtra("palet", palet)
                                intent.putExtra("caja", caja)
                                intent.putExtra("almacen", almacen)
                                startActivity(intent)
                            }

                            override fun onFailure(call: Call<CajaExiste>, t: Throwable) {
                                val message = t.message?.lowercase(Locale.ROOT)
                                if (message != null && message.contains("unable to resolve host")) {
                                    Toast.makeText(this@AlmacenVirtualMainPalets, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@AlmacenVirtualMainPalets, "Error " + t.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        })
                }
            }
        })

        drawerLayout.addDrawerListener(object: DrawerLayout.DrawerListener {
            override fun onDrawerSlide(p0: View, p1: Float) {}
            override fun onDrawerStateChanged(p0: Int) {}

            override fun onDrawerOpened(p0: View) {
                recyclerView.isFocusable = false
                eanCajaEntrat.isFocusable = false
            }

            override fun onDrawerClosed(p0: View) {
                recyclerView.isFocusable = true
                eanCajaEntrat.isFocusable = true
            }
        })

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, 4) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val palet = paletList[position]
                if (RetrofitClient.isConnectedToInternet(this@AlmacenVirtualMainPalets)) {
                    deletePaletAPI(codigoAlmacen, palet.palet, object : APIResponseCallback {
                        override fun onSuccess(response: String) {
                            paletList.removeAt(position)
                            paletAdapter.notifyItemRemoved(position)
                        }

                        override fun onError(errorMessage: String) {
                            paletAdapter.notifyItemChanged(position)
                            Toast.makeText(this@AlmacenVirtualMainPalets, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(this@AlmacenVirtualMainPalets, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        findViewById<AppCompatButton>(R.id.anadir_palet).setOnClickListener {
            if (RetrofitClient.isConnectedToInternet(this)) {
                createPaletAPI(codigoAlmacen, object : APIResponseCallback {
                    override fun onSuccess(response: String) {
                        getPaletsAPI()
                    }

                    override fun onError(errorMessage: String) {
                        Toast.makeText(this@AlmacenVirtualMainPalets, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
            }
            paletAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualPalets) {

        recyclerPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val intent = Intent(this, AlmacenVirtualMainCajas::class.java)
        intent.putExtra("palet", almacenVirtual.palet)
        intent.putExtra("codigo_almacen", codigoAlmacen)
        intent.putExtra("nombre_almacen", nombreAlmacen)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (RetrofitClient.isConnectedToInternet(this)) {
            getPaletsAPI()
            recyclerView.scrollToPosition(recyclerPosition)
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }
        paletAdapter.notifyDataSetChanged()

        val cajas = paletList.sumOf { it.cajas }
        if (cajas > 0) {
            eanCajaEntrat.isFocusable = true
        }
    }

    private fun deletePaletAPI(almacen: Int, palet: Int, callback: APIResponseCallback) {
        RetrofitClient.getApiService(this).deletePalet(almacen, palet)
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

    private fun getPaletsAPI() {
        RetrofitClient.getApiService(this).getPalets(codigoAlmacen)
            .enqueue(object : Callback<PaletsResponse> {
                override fun onResponse(call: Call<PaletsResponse>, response: Response<PaletsResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            val nuevos = body.palets.map { (palet, datos) ->
                                AlmacenVirtualPalets(palet, datos.cajas, datos.cantidad)
                            }
                            paletList.clear()
                            paletList.addAll(nuevos)
                            paletAdapter = PaletAdapter(paletList, this@AlmacenVirtualMainPalets)
                            recyclerView.adapter = paletAdapter
                            recyclerView.scheduleLayoutAnimation()
                        }
                    } else if (response.code() != 404) {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "Error al obtener los palets", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<PaletsResponse>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "Error " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun createPaletAPI(almacen: Int, callback: APIResponseCallback) {
        RetrofitClient.getApiService(this).createPalet(almacen)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_almacen_virtual_main_palets)
        when (item.itemId) {
            R.id.boton_exportar_stock -> {
                val intent = Intent(this@AlmacenVirtualMainPalets, StockSearch::class.java)
                intent.putExtra("codigo_almacen", codigoAlmacen)
                intent.putExtra("nombre_almacen", nombreAlmacen)
                startActivity(intent)
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
                                Menu.checkCajaExists(this, almacen, palet, caja, file.readLines(), file)
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
                        Toast.makeText(this@AlmacenVirtualMainPalets, "Respuesta: ${response.body()?.string()}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "No hay productos a insertar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    textViewImportando.visibility = View.GONE
                    progresBarImportando.visibility = View.GONE
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AlmacenVirtualMainPalets, "Error: " + t.message, Toast.LENGTH_LONG).show()
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
