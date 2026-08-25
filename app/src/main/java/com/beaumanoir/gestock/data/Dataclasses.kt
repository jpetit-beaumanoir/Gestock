package com.beaumanoir.gestock.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable


SEGUIR CREANT LES CARPETAS SCHEMA PER CADA ELEMENT
data class Almacenes(
    val codigo: Int,
    val nombre: String,
    val palets: Int,
    val cajas: Int,
    val cantidad: Int
)

data class AlmacenVirtualPalets(
    val palet: Int,
    val cajas: Int,
    val cantidad: Int
)

data class AlmacenVirtualCaja(
    val caja: Int,
    val temporada: String,
    val descripcion: String,
    val cantidad: Int
)

data class AlmacenVirtualProducto(
    val ean: String,
    val id: List<Int>,
    val color: String,
    val talla: String,
    val nombre: String,
    val temporada: String,
    var cantidad: Int,
    var cantidadEscaneada: Int = 0
) : Serializable

data class AlmacenVirtualEliminarProducto(
    val id: List<Int>,
    val ean: String,
    val color: String,
    val talla: String,
    val nombre: String,
    var cantidad: Int,
    var cantidadEscaneada: Int,
    val temporada: String
)

data class ResultadoProductos(
    val ean: String,
    val palet: Int,
    val caja: Int,
    val almacen: Int,
    val talla: String,
    val nombre: String,
    val familia: String,
    val subfamilia: String,
    val color: String,
    val temporada: String,
    val pvp: Float,
    val prmp: Float,
    val marca: String
)

data class ExportarStock(
    val ean: String,
    val palet: Int,
    val caja: Int,
    val descCaja: String,
    val tempCaja: String,
    val almacen: Int,
    val talla: String,
    val nombre: String,
    val familia: String,
    val subfamilia: String,
    val color: String,
    val temporada: String,
    val pvp: Float,
    val prmp: Float,
    val marca: String
)

data class Almacen(
    val nombre: String,
    val palets: Int,
    val cajas: Int,
    val productos: Int
)

data class AlmacenResponse(
    val almacenes: Map<Int, Almacen>
)

data class AlmacenCreateRequest(
    val name: String,
    val code: Int
)

data class Palet(
    val cajas: Int,
    val cantidad: Int
)

data class PaletsResponse(
    val palets: Map<Int, Palet>,
    val total: Int
)

data class Caja(
    val cantidad: Int,
    val descripcion: String,
    val temporada: String
)

data class CajasResponse(
    val cajas: Map<Int, Caja>,
    val total: Int
)

data class CajaExiste(
    val detail: String
)

data class DescTempCajaResponse(
    val descripcion: String,
    val temporada: String
)

data class Familia(
    val nombre: String
)

data class Stock(
    val id: List<Int>,
    val nombre: String,
    val color: String,
    val talla: String,
    val temporada: String,
    val cantidad: Int
)

data class StockCajaResponse(
    val stock: Map<String, Stock>,
    val total: Int
)

data class ProductValues(
    val id: Int?,
    val ean: String,
    val nombre: String,
    val familia: String,
    val subfamilia: String,
    val color: String,
    val talla: String,
    val pvp: String,
    val prmp: String,
    val temporada: String,
    val marca: String
)

data class FilteredSearch(
    val ean: String,
    val palet: Int,
    val caja: Int,
    @SerializedName("desc_caja") val descCaja: String?,
    @SerializedName("temp_caja") val tempCaja: String?,
    val almacen: Int,
    val talla: String?,
    val nombre: String?,
    val familia: String?,
    val subfamilia: String?,
    val color: String?,
    val temporada: String?,
    val pvp: Float,
    val prmp: Float,
    val marca: String?
)

data class AddStockRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val eans: List<String>
)

data class DeleteStockRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val ids: List<Int>
)

data class MoveStockRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val ids: List<Int>
)
