package com.beaumanoir.gestock.data.schemas.caja

data class CajaUpdateCantidadRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)