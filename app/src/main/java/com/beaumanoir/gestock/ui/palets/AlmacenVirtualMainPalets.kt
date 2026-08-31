package com.beaumanoir.gestock.ui.palets

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.adapters.PaletAdapter
import com.beaumanoir.gestock.data.models.palet.Palet
import com.beaumanoir.gestock.data.remote.dto.palet.PaletCreateRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletDeleteRequest
import com.beaumanoir.gestock.data.remote.dto.palet.PaletsGetRequest
import com.beaumanoir.gestock.data.repository.PaletRepository
import com.beaumanoir.gestock.ui.cajas.AlmacenVirtualMainCajas
import com.beaumanoir.gestock.ui.stock.GestionCajasAlmacen
import com.beaumanoir.gestock.ui.stock.StockSearch
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class AlmacenVirtualMainPalets : AppCompatActivity(), PaletAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private var codigoAlmacen: Int = 0
    private lateinit var eanCajaEntrat: EditText
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var paletAdapter: PaletAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var collectionPalet: List<Palet>
    private val paletList: MutableList<Palet> = ArrayList()
    private var nombreAlmacen: String = ""
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var textViewImportando: TextView
    private lateinit var progresBarImportando: ProgressBar
    private var recyclerPosition = 0

    private val paletsRepository: PaletRepository by lazy {
        (application as GestockApp).paletRepository
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
            getPalets()
        }

        findViewById<TextView>(R.id.almacen_nombre).text = "$codigoAlmacen $nombreAlmacen"

        // ============================================= CODI MENU LATERAL ============================================= //
        val navigationView = findViewById<NavigationView>(R.id.nav_menu)
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.nombre_almacen_navigation).text = nombreAlmacen
        headerView.findViewById<TextView>(R.id.codigo_almacen_navigation).text = codigoAlmacen.toString()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearlayout_almacen_virtual_main_palets)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout_almacen_virtual_main_palets)
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

                    val intent = Intent(
                        this@AlmacenVirtualMainPalets,
                        GestionCajasAlmacen::class.java
                    ).apply {
                        putExtra("palet", palet)
                        putExtra("caja", caja)
                        putExtra("almacen", almacen)
                    }

                    startActivity(intent)

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

                deletePalet(codigoAlmacen, palet.palet)

            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        findViewById<AppCompatButton>(R.id.anadir_palet).setOnClickListener {
            createPalet(codigoAlmacen)
        }
    }

    override fun onItemClick(almacenVirtual: Palet) {
        val intent = Intent(this, AlmacenVirtualMainCajas::class.java)
        intent.putExtra("palet", almacenVirtual.palet)
        intent.putExtra("codigo_almacen", codigoAlmacen)
        intent.putExtra("nombre_almacen", nombreAlmacen)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        getPalets()

        val cajas = paletList.sumOf { it.cajas }
        if (cajas > 0) {
            eanCajaEntrat.isFocusable = true
        }
    }

    private fun deletePalet(almacen: Int, palet: Int) {

        lifecycleScope.launch {
            try {

                val response = paletsRepository.deletePalet(
                    almacen = almacen,
                    palet = palet
                )

                Log.e(
                    "GESTOCK_ERROR",
                    response.message
                )

                Toast.makeText(this@AlmacenVirtualMainPalets,response.message, Toast.LENGTH_SHORT).show()
                getPalets()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error eliminando el palet $palet del almacén $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainPalets,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getPalets() {

        lifecycleScope.launch {
            try {

                val response  = paletsRepository.getPalets(
                    codigoAlmacen
                )

                val palets = response.palets

                collectionPalet =
                    if (palets.isEmpty()) {
                        Toast.makeText(
                            this@AlmacenVirtualMainPalets,
                            "No se encontraron palets",
                            Toast.LENGTH_LONG
                        ).show()

                        emptyList()
                    } else {
                        palets.map { (palet, datos) ->
                            Palet(palet, datos.cajas, datos.cantidad)
                        }
                    }

                paletList.clear()
                paletList.addAll(collectionPalet)

                paletAdapter.notifyDataSetChanged()
                recyclerView.scheduleLayoutAnimation()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error obteniendo palets del almacén $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainPalets,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createPalet(almacen: Int) {

        lifecycleScope.launch {
            try {

                val response = paletsRepository.createPalet(
                    PaletCreateRequest(
                        almacen = almacen
                    )
                )

                Toast.makeText(this@AlmacenVirtualMainPalets,response.message, Toast.LENGTH_SHORT).show()
                getPalets()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error creando palets del almacén $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@AlmacenVirtualMainPalets,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
                                StockImportService.checkCajaExists(this, almacen, palet, caja, file.readLines(), file)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Error procesando archivo ${file.name}", Toast.LENGTH_SHORT).show()
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