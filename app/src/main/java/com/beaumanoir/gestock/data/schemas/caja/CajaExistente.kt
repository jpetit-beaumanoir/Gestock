package com.beaumanoir.gestock.data.schemas.caja

data class CajaExistente(
    val caja: Int,
    val palet: Int,
    val almacen: Int,
    val temporada: String,
    val descripcion: String,
    val cantidad: Int
)