package com.beaumanoir.gestock.data.remote.dto.stock

data class StockExportItem(
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

    val pvp: Double,
    val prmp: Double,

    val marca :String

)