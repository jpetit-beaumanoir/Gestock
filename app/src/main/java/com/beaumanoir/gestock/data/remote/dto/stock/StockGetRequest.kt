package com.beaumanoir.gestock.data.remote.dto.stock

data class StockGetRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)