package com.beaumanoir.gestock.data.remote.dto.stock

data class StockMoveRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val ids: List<Int>
)