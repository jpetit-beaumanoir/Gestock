package com.beaumanoir.gestock.data

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.API.RetrofitClient
import com.beaumanoir.gestock.data.sqlite.PaletAdapter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AlmacenVirtualMainPalets : AppCompatActivity(), PaletAdapter.OnItemClickListener {

    private lateinit var arrow: TextView
    private lateinit var arrowBackground: TextView
    private var codigoAlmacen: Int = 0
    private lateinit var eanCajaEntrat: EditText
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var paletAdapter: PaletAdapter
    private lateinit var recyclerView: RecyclerView
    private var paletList: MutableList<AlmacenVirtualPalets> = ArrayList()
    private var nombreAlmacen: String = ""

    interface APIResponseCallback {
        fun onError(errorMessage: String)
        fun onSuccess(response: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.almacen_virtual_main_palets)

        mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
        arrowBackground = findViewById(R.id.arrow_background)
        arrow = findViewById(R.id.arrow)

        codigoAlmacen = intent.getIntExtra("codigo_almacen", 0)
        nombreAlmacen = intent.getStringExtra("nombre_almacen").toString()

        val refreshButton = findViewById<AppCompatButton>(R.id.refresh_button_palets)
        refreshButton.setOnClickListener {
            ObjectAnimator.ofFloat(refreshButton, "rotation", 0.0f, 360.0f).apply {
                duration = 700L
                start()
            }
            getPaletsAPI()
        }

        findViewById<TextView>(R.id.almacen_nombre).text = "$codigoAlmacen $nombreAlmacen"

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
                adjustRecyclerViewHeight()
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
            adjustRecyclerViewHeight()
        }
    }

    private fun adjustRecyclerViewHeight() {
        val height: Int
        if (paletList.size > 5) {
            arrowBackground.visibility = TextView.VISIBLE
            arrow.visibility = TextView.VISIBLE
            height = 1020
        } else {
            arrowBackground.visibility = TextView.GONE
            arrow.visibility = TextView.GONE
            height = -2
        }
        recyclerView.layoutParams.height = height
    }

    override fun onItemClick(almacenVirtual: AlmacenVirtualPalets) {
        val intent = Intent(this, AlmacenVirtualMainCajas::class.java)
        intent.putExtra("palet", almacenVirtual.palet)
        intent.putExtra("almacen", codigoAlmacen)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (RetrofitClient.isConnectedToInternet(this)) {
            getPaletsAPI()
        } else {
            Toast.makeText(this, "NO TIENES CONEXIÓN A INTERNET", Toast.LENGTH_LONG).show()
        }
        paletAdapter.notifyDataSetChanged()
        adjustRecyclerViewHeight()
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
                            adjustRecyclerViewHeight()
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
}
