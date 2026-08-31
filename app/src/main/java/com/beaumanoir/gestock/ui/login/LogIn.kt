package com.beaumanoir.gestock.ui.login

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.local.adapters.AlmacenAdapter
import com.beaumanoir.gestock.data.models.almacen.Almacen
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenCreateRequest
import com.beaumanoir.gestock.data.repository.AlmacenRepository
import com.beaumanoir.gestock.data.repository.AuthRepository
import com.beaumanoir.gestock.ui.palets.AlmacenVirtualMainPalets
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.data.remote.dto.almacen.AlmacenDeleteRequest

class LogIn : AppCompatActivity(), AlmacenAdapter.OnItemClickListener {


    private lateinit var permissionLauncher: ActivityResultLauncher<Intent>

    private lateinit var recyclerView: RecyclerView

    private lateinit var botonOpcionesAlmacenes: AppCompatButton
    private lateinit var linearLayoutRecycler: LinearLayout
    private lateinit var collectionAlmacen: List<Almacen>
    private lateinit var almacenAdapter: AlmacenAdapter
    private val almacenList: MutableList<Almacen> = ArrayList()

    private lateinit var editTextNombreAlmacen: EditText
    private lateinit var editTextCodigoAlmacen: EditText
    private lateinit var btnCrearAlmacen: AppCompatButton
    private lateinit var btnEliminarAlmacen: AppCompatButton
    private lateinit var editTextKey: EditText
    private lateinit var usuariTextView: TextView
    private lateinit var textViewValdiarUsuario: TextView
    private var apiKeyGuardada: Boolean = false
    private lateinit var usuarioLogeado: String

    private val almacenRepository: AlmacenRepository by lazy {
        (application as GestockApp).almacenRepository
    }

    private val authRepository: AuthRepository by lazy {
        (application as GestockApp).authRepository
    }


    // ARREGLAR LA RESPOSTA DEL SERVIDOR PER RETORNAR FORBIDEN EN COMTPES DE NO S'HAN TROBAT MAGATZEMS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        apiKeyGuardada = prefs.getString("api_key_gestock", null)?.isNotEmpty() ?: false
        usuarioLogeado = prefs.getString("user", "").toString()

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!Environment.isExternalStorageManager()) {
                finish()
            }
        }

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            permissionLauncher.launch(intent)
        }

        // ========================================================================= //

        setContentView(R.layout.log_in)

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_almacenes)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(
                refreshButton,
                "rotation",
                0.0f,
                360.0f
            ).apply {
                duration = 700L
                start()
            }
            if (apiKeyGuardada && usuarioLogeado != "anonim") {
                getAlmacenes()
            }
        }

        textViewValdiarUsuario = findViewById(R.id.textview_validar_usuario)
        linearLayoutRecycler = findViewById(R.id.linearLayout)

        botonOpcionesAlmacenes = findViewById(R.id.boton_opciones_login)

        botonOpcionesAlmacenes.setOnClickListener {

            val view = layoutInflater.inflate(R.layout.alert_crear_eliminar_almacen, null)

            val dialog = AlertDialog.Builder(this)
                .setView(view)
                .create()

            editTextNombreAlmacen = view.findViewById(R.id.nombre_almacen)
            editTextCodigoAlmacen = view.findViewById(R.id.codigo_almacen)

            editTextKey = view.findViewById(R.id.key_usuari)
            usuariTextView = view.findViewById(R.id.usuari_registrat)
            usuariTextView.text = usuarioLogeado

            btnCrearAlmacen = view.findViewById(R.id.boton_crear_almacen)
            btnEliminarAlmacen = view.findViewById(R.id.boton_eliminar_almacen)

            editTextCodigoAlmacen.addTextChangedListener {
                actualizarBotones()
            }

            editTextNombreAlmacen.addTextChangedListener {
                actualizarBotones()
            }

            editTextKey.setOnEditorActionListener { _, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return@setOnEditorActionListener false
                }

                val key = editTextKey.text.toString().trim()

                if (key.isEmpty()) {
                    Toast.makeText(this@LogIn,"Introdueix una clau", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener false
                }

                validarUsuari(
                    key = key,
                    dialog=dialog
                )

                true

            }

            btnCrearAlmacen.setOnClickListener {
                val codigoAlmacenCrear = editTextCodigoAlmacen.text.toString().toIntOrNull()
                val nombreAlmacenCrear = editTextNombreAlmacen.text.toString()

                if (codigoAlmacenCrear == null || nombreAlmacenCrear.isEmpty()) {
                    Toast.makeText(this, "Especifica nombre y codigo antes de crear", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                createAlmacen(nombreAlmacenCrear, codigoAlmacenCrear, dialog)

                dialog.dismiss()

                /*onItemClick(
                    Almacenes(
                        codigoAlmacenCrear,
                        nombreAlmacenCrear.uppercase(Locale.ROOT),
                        0,
                        0,
                        0
                    )
                )*/
            }

            btnEliminarAlmacen.setOnClickListener {
                val codigoAlmacenEliminar = editTextCodigoAlmacen.text.toString().trim().toIntOrNull()

                if (codigoAlmacenEliminar == null) {
                    Toast.makeText(this, "Introdueix un codi de magatzem vàliddddddd", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                deleteAlmacen(codigoAlmacenEliminar, dialog)

            }

            dialog.show()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        }

        recyclerView = findViewById(R.id.mostrar_almacenes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        almacenAdapter = AlmacenAdapter(
            almacenList,
            this
        )

        recyclerView.adapter = almacenAdapter

        findViewById<TextView>(R.id.textview_version).text =
            packageManager.getPackageInfo(packageName, 0).versionName

    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoPantalla()
    }
    private fun actualizarBotones() {

        val codigoEntrado = editTextCodigoAlmacen.text.toString().trim().isNotEmpty()
        val nombreEntrado = editTextNombreAlmacen.text.toString().trim().isNotEmpty()

        when {

            codigoEntrado && nombreEntrado -> {

                btnCrearAlmacen.isEnabled = true
                btnCrearAlmacen.alpha = 1f

                btnEliminarAlmacen.isEnabled = false
                btnEliminarAlmacen.alpha = 0.5f
            }

            codigoEntrado -> {

                btnCrearAlmacen.isEnabled = false
                btnCrearAlmacen.alpha = 0.5f

                btnEliminarAlmacen.isEnabled = true
                btnEliminarAlmacen.alpha = 1f
            }

            else -> {

                btnCrearAlmacen.isEnabled = false
                btnCrearAlmacen.alpha = 0.5f

                btnEliminarAlmacen.isEnabled = false
                btnEliminarAlmacen.alpha = 0.5f
            }
        }

    }

    private fun getAlmacenes() {

        lifecycleScope.launch {

            try {

                val response =
                    almacenRepository.getAlmacenes()

                val almacenes = response.almacenes

                collectionAlmacen =
                    if (almacenes.isEmpty()) {

                        Toast.makeText(
                            this@LogIn,
                            "No se encontraron almacenes",
                            Toast.LENGTH_LONG
                        ).show()

                        emptyList()

                    } else {

                        almacenes.map { (codigo, almacen) ->

                            Almacen(
                                codigo,
                                almacen.nombre,
                                almacen.palets,
                                almacen.cajas,
                                almacen.productos
                            )

                        }
                    }

                almacenList.clear()

                almacenList.addAll(
                    collectionAlmacen
                )

                almacenAdapter.notifyDataSetChanged()

                recyclerView.scheduleLayoutAnimation()

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error obteniendo almacenes",
                    e
                )

                Toast.makeText(
                    this@LogIn,
                    e.message ?: "Error desconocido",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loginAlmacen(codigo: Int) {

        lifecycleScope.launch {
            try {
                val response = almacenRepository.loginAlmacen(codigo)

                val intent = Intent(
                    this@LogIn,
                    AlmacenVirtualMainPalets::class.java
                ). apply {
                    putExtra("codigo_almacen", codigo)
                    putExtra("nombre_almacen", response.nombre)
                }
                startActivity(intent)

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error logueando en el almacén $codigo",
                    e
                )

                Toast.makeText(
                    this@LogIn,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validarUsuari(
        key: String,
        dialog: AlertDialog
    ) {
        lifecycleScope.launch {
            try {

                val response = authRepository.validarUsuari(key)
                val user = response.user

                getSharedPreferences(
                    "app_prefs",
                    MODE_PRIVATE
                ).edit {
                    putString("api_key_gestock", editTextKey.text.toString())
                        .putString("user", user)
                }

                usuariTextView.text = user
                apiKeyGuardada = true
                usuarioLogeado = user

                dialog.dismiss()
                actualizarEstadoPantalla()


            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error validando usuario",
                    e
                )

                Toast.makeText(
                    this@LogIn,
                    e.message ?: "Error desconocido",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createAlmacen(nombre: String, codigo: Int, dialog: AlertDialog) {

        lifecycleScope.launch {
            try {

                btnCrearAlmacen.isEnabled = false

                val response = almacenRepository.createAlmacen(
                    AlmacenCreateRequest(
                        nombre=nombre,
                        codigo = codigo
                    )
                )

                Toast.makeText(this@LogIn,response.message, Toast.LENGTH_SHORT).show()

                dialog.dismiss()
                getAlmacenes()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error eliminando almacén $codigo",
                    e
                )

                Toast.makeText(
                    this@LogIn,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteAlmacen(
        codigo: Int,
        dialog: AlertDialog
    ) {
        lifecycleScope.launch {
            try {
                btnEliminarAlmacen.isEnabled = false

                val response = almacenRepository.deleteAlmacen(
                    AlmacenDeleteRequest(
                        codigo = codigo
                    )
                )

                Toast.makeText(
                    this@LogIn,
                    response.message,
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
                getAlmacenes()

            } catch (e: Exception) {
                Log.e(
                    "GESTOCK_ERROR",
                    "Error eliminando almacén $codigo",
                    e
                )

                Toast.makeText(
                    this@LogIn,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()

                btnEliminarAlmacen.isEnabled = true
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

    override fun onItemClick(almacenVirtual: Almacen) {
        loginAlmacen(
            codigo = almacenVirtual.codigo.toInt()
        )
    }

    private fun actualizarEstadoPantalla() {
        if (!apiKeyGuardada || usuarioLogeado == "anonim") {
            textViewValdiarUsuario.visibility = View.VISIBLE
            linearLayoutRecycler.visibility = View.GONE
            botonOpcionesAlmacenes.callOnClick()
        } else {
            textViewValdiarUsuario.visibility = View.GONE
            linearLayoutRecycler.visibility = View.VISIBLE
            getAlmacenes()
        }
    }

}