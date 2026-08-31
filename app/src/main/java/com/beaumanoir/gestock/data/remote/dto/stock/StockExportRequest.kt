package com.beaumanoir.gestock.data.remote.dto.stock

data class StockExportRequest(
    val almacen: Int,
    val ean: String? = null,
    val talla: String? = null,
    val nombre: String? = null,
    val familia: String? = null,
    val color: String? = null,
    val temporada: String? = null
)