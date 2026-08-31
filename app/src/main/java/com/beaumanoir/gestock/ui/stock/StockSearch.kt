package com.beaumanoir.gestock.ui.stock

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.beaumanoir.gestock.GestockApp
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportItem
import com.beaumanoir.gestock.data.remote.dto.stock.StockExportRequest
import com.beaumanoir.gestock.data.repository.FamiliaRepository
import com.beaumanoir.gestock.data.repository.StockRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StockSearch : AppCompatActivity() {
    private lateinit var autoCompleteFamilia: AutoCompleteTextView
    private var codigoAlmacen: Int = 0
    private lateinit var nombreAlmacen: String
    private lateinit var colorEntrado: EditText
    private lateinit var eanEntrado: EditText
    private lateinit var nombreEntrado: EditText
    private lateinit var tallaEntrada: EditText
    private lateinit var temporadaEntrada: EditText

    private val familiaRepository: FamiliaRepository by lazy {
        (application as GestockApp).familiaRepository
    }

    private val stockRepository: StockRepository by lazy {
        (application as GestockApp).stockRepository
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

            buscarProductesFiltrats(
                ean = ean,
                talla = talla,
                nombre = nombre,
                familia = familias,
                color = color,
                temporada = temporada
            )
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

    private fun getFamilias() {

        lifecycleScope.launch {
            try {

                val response = familiaRepository.getFamilias()

                val nombres = response.familias.map { it }
                val adapter = ArrayAdapter(this@StockSearch, android.R.layout.simple_dropdown_item_1line, nombres)
                autoCompleteFamilia.setAdapter(adapter)
                autoCompleteFamilia.threshold = 1

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error creando palets del almacén $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@StockSearch,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buscarProductesFiltrats(
        ean: String,
        talla: String,
        nombre: String,
        familia: String,
        color: String,
        temporada: String
    ) {

        lifecycleScope.launch {
            try {

                val response = stockRepository.exportStock(
                    almacen = codigoAlmacen,
                    ean = ean,
                    talla = talla,
                    nombre = nombre,
                    familia = familia,
                    color = color,
                    temporada = temporada
                )

                exportarBusquedaCSV(response.items)

            } catch (e: Exception) {

                Log.e(
                    "GESTOCK_ERROR",
                    "Error creando palets del almacén $codigoAlmacen",
                    e
                )

                Toast.makeText(
                    this@StockSearch,
                    obtenerMensajeError(e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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

    private fun exportarBusquedaCSV(productos: List<StockExportItem>) {
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

}
