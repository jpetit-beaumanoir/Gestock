package com.beaumanoir.gestock.data

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.AlmacenAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class LogIn : AppCompatActivity(), AlmacenAdapter.OnItemClickListener {


    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    private lateinit var recyclerView: RecyclerView

    private lateinit var botonOpcionesAlmacenes: AppCompatButton
    private lateinit var nombreAlmacen: String
    private lateinit var codigoAlmacen: String
    private lateinit var collectionAlmacen: List<Almacenes>
    private lateinit var almacenAdapter: AlmacenAdapter
    private val almacenList: MutableList<Almacenes> = ArrayList()

    private lateinit var editTextNombreAlmacen: EditText
    private lateinit var editTextCodigoAlmacen: EditText
    private lateinit var btnCrearAlmacen: AppCompatButton
    private lateinit var btnEliminarAlmacen: AppCompatButton

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)

    }

    CAMBIAR LA TAULA ALMACENES PERQUE EL CODI SIGUI UNA STRING EN COMPTES DE UN INT PER CASOS DE 0000 MOSTRI ELS 4 DIGITS I NO NOMES 0

    PRIMER MODIFICAR LA TAULA SQL PER QUE SIGUI VARCHAR(4)

    interface LoginCallback {
        fun onLoginFailure(errorMessage: String)
        fun onLoginSuccess(nombre: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContentView(R.layout.log_in)

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_almacenes)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(refreshButton, "rotation", 0.0f, 360.0f).apply {
                duration = 700L
                start()
            }
            getAlmacenesAPI()
        }

        botonOpcionesAlmacenes = findViewById(R.id.boton_opciones_login)
        botonOpcionesAlmacenes.setOnClickListener {

            val view = layoutInflater.inflate(R.layout.alert_crear_eliminar_almacen, null)

            val dialog = AlertDialog.Builder(this)
                .setView(view)
                .create()

            editTextNombreAlmacen = view.findViewById<EditText>(R.id.nombre_almacen)
            editTextCodigoAlmacen = view.findViewById<EditText>(R.id.codigo_almacen)

            btnCrearAlmacen = view.findViewById(R.id.boton_crear_almacen)
            btnEliminarAlmacen = view.findViewById(R.id.boton_eliminar_almacen)

            editTextCodigoAlmacen.addTextChangedListener {
                actualizarBotones()
            }

            editTextNombreAlmacen.addTextChangedListener {
                actualizarBotones()
            }

            btnCrearAlmacen.setOnClickListener {
                val codigoAlmacenCrear = editTextCodigoAlmacen.text.toString()
                val nombreAlmacenCrear = editTextNombreAlmacen.text.toString()

                if (codigoAlmacenCrear.isEmpty() || nombreAlmacenCrear.isEmpty()) {
                    Toast.makeText(this, "Especifica nombre y codigo antes de crear", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!RetrofitClient.isConnectedToInternet(this)) {
                    Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                createAlmacenesAPI(nombreAlmacenCrear, codigoAlmacenCrear,object : APIResponseCallback {
                    override fun onSuccess(response: String) {
                        Toast.makeText(this@LogIn, response, Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        onItemClick(
                            Almacenes(codigoAlmacenCrear, nombreAlmacenCrear.uppercase(Locale.ROOT), 0, 0, 0)
                        )
                    }

                    override fun onError(errorMessage: String) {
                        Toast.makeText(this@LogIn, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
            }

            btnEliminarAlmacen.setOnClickListener {
                val codigoAlmacenEliminar = editTextCodigoAlmacen.text.toString()

                if (codigoAlmacenEliminar.isEmpty()) {
                    Toast.makeText(this, "Especifica el codigo del almacen a eliminar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!RetrofitClient.isConnectedToInternet(this)) {
                    Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                deleteAlmacenesAPI(codigoAlmacenEliminar.toInt(), object :
                    APIResponseCallback {
                    override fun onSuccess(response: String) {
                        Toast.makeText(this@LogIn, response, Toast.LENGTH_LONG).show()
                        getAlmacenesAPI()
                        dialog.dismiss()
                    }

                    override fun onError(errorMessage: String) {
                        Toast.makeText(this@LogIn, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })

            }

            dialog.show()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        }

        recyclerView = findViewById(R.id.mostrar_almacenes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.textview_version).text =
            packageManager.getPackageInfo(packageName, 0).versionName

    }

    override fun onResume() {

        if (RetrofitClient.isConnectedToInternet(this)) {
            getAlmacenesAPI()
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }

        super.onResume()
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
    private fun getAlmacenesAPI() {
        RetrofitClient.getApiService().getAlmacenes().enqueue(object : Callback<AlmacenResponse> {
            override fun onResponse(call: Call<AlmacenResponse>, response: Response<AlmacenResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val almacenes = body?.almacenes
                    collectionAlmacen = if (almacenes.isNullOrEmpty()) {
                        Toast.makeText(this@LogIn, "No se encontraron almacenes", Toast.LENGTH_LONG).show()
                        emptyList()
                    } else {
                        almacenes.map { (codigo, almacen) ->
                            Almacenes(codigo, almacen.nombre, almacen.palets, almacen.cajas, almacen.productos)
                        }
                    }
                    almacenList.clear()
                    almacenList.addAll(collectionAlmacen)
                    almacenAdapter = AlmacenAdapter(almacenList, this@LogIn)
                    recyclerView.adapter = almacenAdapter
                    recyclerView.scheduleLayoutAnimation()
                } else {
                    Toast.makeText(this@LogIn, "Error al obtener los almacenes", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AlmacenResponse>, t: Throwable) {
                val message = t.message?.lowercase(Locale.ROOT)
                Log.d("ERROR CONEXION SERVIDOR", message.toString())

                if (message != null && message.contains("unable to resolve host")) {
                    Toast.makeText(this@LogIn, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LogIn, "Error " + t.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    override fun onItemClick(almacenVirtual: Almacenes) {
        nombreAlmacen = almacenVirtual.nombre
        codigoAlmacen = almacenVirtual.codigo
        if (RetrofitClient.isConnectedToInternet(this)) {
            login(object : LoginCallback {
                override fun onLoginSuccess(nombre: String) {
                    val intent = Intent(this@LogIn, AlmacenVirtualMainPalets::class.java)
                    intent.putExtra("codigo_almacen", codigoAlmacen)
                    intent.putExtra("nombre_almacen", nombre)
                    startActivity(intent)
                }

                override fun onLoginFailure(errorMessage: String) {
                    Toast.makeText(this@LogIn, errorMessage, Toast.LENGTH_LONG).show()
                }
            })
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }
    }

    private fun login(callback: LoginCallback) {
        RetrofitClient.getApiService().loginAlmacen(codigoAlmacen).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    callback.onLoginSuccess(nombreAlmacen)
                } else {
                    try {
                        val detail = JSONObject(response.errorBody()?.string().toString()).getString("detail")
                        callback.onLoginFailure(detail)
                    } catch (e: Exception) {
                        callback.onLoginFailure("Error desconocido: " + e.localizedMessage)
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                val message = t.message?.lowercase(Locale.ROOT)
                if (message != null && message.contains("unable to resolve host")) {
                    callback.onLoginFailure("No tienes conexión a internet")
                } else {
                    callback.onLoginFailure("Error de conexión: " + t.localizedMessage)
                }
            }
        })
    }

    private fun createAlmacenesAPI(nombre: String, codigo: String, callback: APIResponseCallback) {
        RetrofitClient.getApiService().createAlmacen(AlmacenCreateRequest(nombre, codigo))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string() ?: "Operación exitosa"
                        val msg = JSONObject(bodyString).getString("msg")
                        callback.onSuccess(msg)
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"

                        try {
                            val detail = JSONObject(errorString).getString("detail")
                            callback.onError(detail)
                        } catch (e: Exception) {
                            callback.onError(errorString)
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        callback.onError("No tienes conexión a internet")
                    } else {
                        callback.onError("Error: " + t.message)
                        Log.d("ERROR CREATE ALMACEN", t.message.toString())
                    }
                }
            })
    }

    private fun deleteAlmacenesAPI(codigo: Int, callback: APIResponseCallback) {
        RetrofitClient.getApiService().deleteAlmacen(codigo).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("Response API", response.toString())
                    val bodyString = response.body()?.string() ?: "Operación exitosa"
                    val msg = JSONObject(bodyString).getString("msg")
                    callback.onSuccess(msg)
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
