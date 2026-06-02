package com.beaumanoir.gestock.data

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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

class CrearAlmacen : AppCompatActivity(), AlmacenAdapter.OnItemClickListener {

    private lateinit var almacenAdapter: AlmacenAdapter
    private val almacenList: MutableList<Almacenes> = ArrayList()
    private var codigoAlmacen: Int = 0
    private lateinit var collectionAlmacen: List<Almacenes>
    private lateinit var nombreAlmacen: String
    private lateinit var recyclerView: RecyclerView

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)
    }

    interface LoginCallback {
        fun onLoginFailure(errorMessage: String)
        fun onLoginSuccess(nombre: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_almacen)

        recyclerView = findViewById(R.id.mostrar_almacenes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (RetrofitClient.isConnectedToInternet(this)) {
            getAlmacenesAPI()
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }

        recyclerView.layoutParams.height = if (almacenList.size > 5) 1000 else -2

        findViewById<AppCompatButton>(R.id.boton_crear_almacen).setOnClickListener {
            val codigo = findViewById<EditText>(R.id.codigo_almacen).text.toString()
            var nombre = findViewById<EditText>(R.id.nombre_almacen).text.toString()
            if (nombre.isNotEmpty()) {
                nombre = nombre.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
            if (codigo.isNotEmpty() && nombre.isNotEmpty()) {
                if (RetrofitClient.isConnectedToInternet(this)) {
                    createAlmacenesAPI(nombre, codigo.toInt(), object : APIResponseCallback {
                        override fun onSuccess(response: String) {
                            Toast.makeText(this@CrearAlmacen, response, Toast.LENGTH_LONG).show()
                            onItemClick(
                                Almacenes(codigo.toInt(), nombre.uppercase(Locale.ROOT), 0, 0, 0)
                            )
                        }

                        override fun onError(errorMessage: String) {
                            Toast.makeText(this@CrearAlmacen, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Especifica nombre y codigo antes de crear", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<AppCompatButton>(R.id.boton_eliminar_almacen).setOnClickListener {
            val editText = findViewById<EditText>(R.id.codigo_almacen)
            if (editText.text.toString().isNotEmpty()) {
                if (RetrofitClient.isConnectedToInternet(this)) {
                    deleteAlmacenesAPI(editText.text.toString().toInt(), object : APIResponseCallback {
                        override fun onSuccess(response: String) {
                            Toast.makeText(this@CrearAlmacen, response, Toast.LENGTH_LONG).show()
                            editText.setText("")
                            getAlmacenesAPI()
                        }

                        override fun onError(errorMessage: String) {
                            Toast.makeText(this@CrearAlmacen, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Especifica el codigo del almacen a eliminar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onItemClick(almacenVirtual: Almacenes) {
        nombreAlmacen = almacenVirtual.nombre
        codigoAlmacen = almacenVirtual.codigo
        if (RetrofitClient.isConnectedToInternet(this)) {
            login(object : LoginCallback {
                override fun onLoginSuccess(nombre: String) {
                    val intent = Intent(this@CrearAlmacen, StockSearch::class.java)
                    intent.putExtra("codigo_almacen", codigoAlmacen)
                    intent.putExtra("nombre_almacen", nombre)
                    startActivity(intent)
                }

                override fun onLoginFailure(errorMessage: String) {
                    Toast.makeText(this@CrearAlmacen, errorMessage, Toast.LENGTH_LONG).show()
                }
            })
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAlmacenesAPI() {
        RetrofitClient.getApiService(this).getAlmacenes().enqueue(object : Callback<AlmacenResponse> {
            override fun onResponse(call: Call<AlmacenResponse>, response: Response<AlmacenResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val almacenes = body?.almacenes
                    collectionAlmacen = if (almacenes.isNullOrEmpty()) {
                        Toast.makeText(this@CrearAlmacen, "No se encontraron almacenes", Toast.LENGTH_LONG).show()
                        emptyList()
                    } else {
                        almacenes.map { (codigo, almacen) ->
                            Almacenes(codigo, almacen.nombre, almacen.palets, almacen.cajas, almacen.productos)
                        }
                    }
                    almacenList.clear()
                    almacenList.addAll(collectionAlmacen)
                    almacenAdapter = AlmacenAdapter(almacenList, this@CrearAlmacen)
                    recyclerView.adapter = almacenAdapter
                    recyclerView.scheduleLayoutAnimation()
                } else {
                    Toast.makeText(this@CrearAlmacen, "Error al obtener los almacenes", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AlmacenResponse>, t: Throwable) {
                val message = t.message?.lowercase(Locale.ROOT)
                if (message != null && message.contains("unable to resolve host")) {
                    Toast.makeText(this@CrearAlmacen, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@CrearAlmacen, "Error " + t.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun login(callback: LoginCallback) {
        RetrofitClient.getApiService(this).loginAlmacen(codigoAlmacen).enqueue(object : Callback<ResponseBody> {
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

    private fun createAlmacenesAPI(nombre: String, codigo: Int, callback: APIResponseCallback) {
        RetrofitClient.getApiService(this).createAlmacen(AlmacenCreateRequest(nombre, codigo))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string() ?: "Operación exitosa"
                        val msg = JSONObject(bodyString).getString("msg")
                        callback.onSuccess(msg)
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(
                            this@CrearAlmacen,
                            JSONObject(errorString).getString("detail"),
                            Toast.LENGTH_SHORT
                        ).show()
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
        RetrofitClient.getApiService(this).deleteAlmacen(codigo).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
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
