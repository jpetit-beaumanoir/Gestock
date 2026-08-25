package com.beaumanoir.gestock.data

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.GestockApiFactory
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.sqlite.ProductoAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AñadirProductoCaja : AppCompatActivity(), ProductoAdapter.OnItemClickListener {

    private var codigoAlmacen: Int = 0
    private var idCaja: Int = 0
    private var idPalet: Int = 0
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var productAddAdapter: ProductoAdapter
    private lateinit var recyclerView: RecyclerView
    private val productAddAdapterList: MutableList<AlmacenVirtualProducto> = ArrayList()
    private val eansAfegirList: MutableList<String> = ArrayList()
    private lateinit var botonConfirmar: AppCompatButton
    private lateinit var botonCancelar: AppCompatButton

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(result: ProductValues)
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualProducto) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.anadir_producto_caja)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)

        val textView = findViewById<TextView>(R.id.total_productos_anadir)
        codigoAlmacen = intent.getIntExtra("almacen", 0)
        idPalet = intent.getIntExtra("palet", 0)
        idCaja = intent.getIntExtra("caja", 0)
        findViewById<TextView>(R.id.titulo_pantalla_anadir).text =
            "AÑADIR ARTÍCULOS PALET $idPalet CAJA $idCaja"

        recyclerView = findViewById(R.id.mostrar_productos_anadir)
        recyclerView.layoutManager = LinearLayoutManager(this)

        botonCancelar = findViewById(R.id.cancelar_adicion_productos)
        botonConfirmar = findViewById(R.id.confirmar_adicion_productos)

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, 12) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                productAddAdapterList.removeAt(position)
                eansAfegirList.removeAt(position)
                productAddAdapter.notifyItemRemoved(position)
                findViewById<TextView>(R.id.total_productos_anadir).text =
                    "TOTAL: ${eansAfegirList.size}"
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        val editText = findViewById<EditText>(R.id.ean_producto_anadir)
        editText.requestFocus()
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 0 || actionId == 4 || actionId == 6) {
                editText.text.clear()
                editText.requestFocus()
                true
            } else {
                false
            }
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                editText.requestFocus()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length == 13) {
                    getValuesfromProductoAPI(s.toString(), object : APIResponseCallback {
                        override fun onSuccess(result: ProductValues) {
                            eansAfegirList.add(result.ean)
                            productAddAdapterList.add(
                                AlmacenVirtualProducto(
                                    result.ean,
                                    ArrayList(),
                                    result.color,
                                    result.talla,
                                    result.nombre,
                                    result.temporada,
                                    0
                                )
                            )
                            if (::productAddAdapter.isInitialized) {
                                recyclerView.layoutParams.height =
                                    if (productAddAdapterList.size > 4) 960 else -2
                                productAddAdapter.notifyDataSetChanged()
                            } else {
                                productAddAdapter =
                                    ProductoAdapter(productAddAdapterList, this@AñadirProductoCaja)
                                recyclerView.adapter = productAddAdapter
                            }
                            recyclerView.layoutParams.height =
                                if (productAddAdapterList.size <= 4) -2 else 960
                            editText.text.clear()
                            textView.text = "TOTAL: ${eansAfegirList.size}"
                        }

                        override fun onError(errorMessage: String) {
                            editText.text.clear()
                            mediaPlayer.start()
                            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                editText.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
                editText.requestFocus()
            }
        })

        botonCancelar.setOnClickListener {
            if (eansAfegirList.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("¿SEGURO QUE QUIERES CANCELAR?")
                    .setCancelable(false)
                    .setPositiveButton("Sí") { _, _ -> finish() }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            } else {
                finish()
            }
        }

        botonConfirmar.setOnClickListener {
            if (eansAfegirList.isNotEmpty()) {
                addStockAPI(eansAfegirList)
            }
            finish()
        }
    }

    private fun getValuesfromProductoAPI(ean: String, callback: APIResponseCallback) {
        GestockApiFactory.getApi(this).getProductValues(ean, codigoAlmacen)
            .enqueue(object : Callback<ProductValues> {
                override fun onResponse(call: Call<ProductValues>, response: Response<ProductValues>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            callback.onSuccess(body)
                        } else {
                            callback.onError("No se ha encontrado información de este producto")
                        }
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        callback.onError(JSONObject(errorString).getString("detail"))
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

    private fun addStockAPI(eans: List<String>) {
        GestockApiFactory.getApi(this)
            .addStock(AddStockRequest(codigoAlmacen, idPalet, idCaja, eans))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        updateCantidadCajaAPI()
                        Toast.makeText(
                            applicationContext,
                            "${eansAfegirList.size} Productos añadidos",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(
                            this@AñadirProductoCaja,
                            "Error ${response.code()}: $errorString",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AñadirProductoCaja, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AñadirProductoCaja, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun updateCantidadCajaAPI() {
        GestockApiFactory.getApi(this)
            .updateCantidadCaja(codigoAlmacen, idPalet, idCaja)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        val errorString = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(
                            this@AñadirProductoCaja,
                            "Error ${response.code()}: $errorString",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    val message = t.message?.lowercase(Locale.ROOT)
                    if (message != null && message.contains("unable to resolve host")) {
                        Toast.makeText(this@AñadirProductoCaja, "No tienes conexión a internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AñadirProductoCaja, "Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    override fun onBackPressed() {

        botonCancelar.callOnClick()

        super.onBackPressed()
    }

}
