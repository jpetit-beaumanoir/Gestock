package com.beaumanoir.gestock.data.remote.dto.stock

data class StockInfo(
    val id: Int,
    val nombre: String,
    val color: String,
    val talla: String,
    val temporada: String,
    val cantidad: Int
)