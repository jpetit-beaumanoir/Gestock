package com.beaumanoir.gestock.data.remote.dto.stock

data class StockAddRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val eans: List<String>
)