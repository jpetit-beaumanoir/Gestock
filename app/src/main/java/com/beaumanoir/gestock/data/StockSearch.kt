package com.beaumanoir.gestock.data

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.beaumanoir.gestock.GestockApiFactory
import com.beaumanoir.gestock.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StockSearch : AppCompatActivity() {
    private lateinit var autoCompleteFamilia: AutoCompleteTextView
    private var codigoAlmacen: Int = 0
    private lateinit var nombreAlmacen: String
    private lateinit var colorEntrado: EditText
    private lateinit var eanEntrado: EditText
    private lateinit var nombreEntrado: EditText
    private lateinit var tallaEntrada: EditText
    private lateinit var temporadaEntrada: EditText

    interface APIResponseCallback<T> {
        fun onError(errorMessage: String)
        fun onSuccess(response: List<T>)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stock_search)

        codigoAlmacen = intent.getIntExtra("codigo_almacen", 0)
        nombreAlmacen = intent.getStringExtra("nombre_almacen")?.uppercase() ?: ""

        findViewById<TextView>(R.id.nombre_almacen).text = nombreAlmacen

        eanEntrado = findViewById(R.id.ean_entrado)
        tallaEntrada = findViewById(R.id.talla_entrada)
        colorEntrado = findViewById(R.id.color_entrado)
        temporadaEntrada = findViewById(R.id.temporada_entrada)
        nombreEntrado = findViewById(R.id.nombre_entrado)
        autoCompleteFamilia = findViewById(R.id.auto_complete_familia)
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_familia)

        if (GestockApiFactory.isConnectedToInternet(this)) {
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

        findViewById<AppCompatButton>(R.id.boton_exportar_resultados).setOnClickListener {
            val ean = eanEntrado.text.toString()
            val talla = tallaEntrada.text.toString()
            val color = colorEntrado.text.toString()
            val temporada = temporadaEntrada.text.toString()
            val nombre = nombreEntrado.text.toString()
            val familias = selectedFamilias.joinToString(",")
            if (GestockApiFactory.isConnectedToInternet(this)) {
                buscarProductesFiltratsAPI(
                    codigoAlmacen, ean, talla, nombre, familias, color, temporada,
                    object : APIResponseCallback<FilteredSearch> {
                        override fun onSuccess(response: List<FilteredSearch>) {
                            if (response.isNotEmpty()) {
                                exportarBusquedaCSV(response)
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
        super.onBackPressed()
    }

    private fun getFamiliasAPI() {
        GestockApiFactory.getApi(this).getFamilias(codigoAlmacen)
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

    private fun buscarProductesFiltratsAPI(
        almacen: Int, ean: String, talla: String, nombre: String,
        familia: String, color: String, temporada: String,
        callback: APIResponseCallback<FilteredSearch>
    ) {
        GestockApiFactory.getApi(this)
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

    private fun exportarBusquedaCSV(productos: List<FilteredSearch>) {
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

}
