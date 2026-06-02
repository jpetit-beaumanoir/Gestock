package com.beaumanoir.gestock.data

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class LogIn : AppCompatActivity() {

    private lateinit var codigoAlmacen: EditText
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.textview_version).text =
            packageManager.getPackageInfo(packageName, 0).versionName

        codigoAlmacen = findViewById(R.id.codigo_almacen)
        val acceder = findViewById<AppCompatButton>(R.id.acceder)

        codigoAlmacen.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId != 6) {
                false
            } else {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)
                accedirMagatzem(codigoAlmacen)
                true
            }
        }

        acceder.setOnClickListener {
            accedirMagatzem(codigoAlmacen)
        }
    }

    private fun accedirMagatzem(codigoAlmacen: EditText) {
        if (codigoAlmacen.text.isNotEmpty()) {
            if (RetrofitClient.isConnectedToInternet(this)) {
                login(object : LoginCallback {
                    override fun onLoginSuccess(nombre: String) {
                        val intent = Intent(this@LogIn, StockSearch::class.java)
                        intent.putExtra("codigo_almacen", codigoAlmacen.text.toString().toInt())
                        intent.putExtra("nombre_almacen", nombre)
                        startActivity(intent)
                    }

                    override fun onLoginFailure(errorMessage: String) {
                        codigoAlmacen.setText("")
                        Toast.makeText(this@LogIn, errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
            }
        } else {
            if (RetrofitClient.isConnectedToInternet(this)) {
                startActivity(Intent(this, CrearAlmacen::class.java))
            } else {
                Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun login(callback: LoginCallback) {
        val apiService = RetrofitClient.getApiService(this)
        apiService.loginAlmacen(codigoAlmacen.text.toString().toInt())
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.code() == 204) {
                        callback.onLoginFailure("No existe un almacén con este código")
                        return
                    }

                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string() ?: return
                        try {
                            val nombre = JSONObject(bodyString).getString("nombre")
                            callback.onLoginSuccess(nombre)
                        } catch (e: Exception) {
                            callback.onLoginFailure("Error al procesar la respuesta: $e")
                        }
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
}
