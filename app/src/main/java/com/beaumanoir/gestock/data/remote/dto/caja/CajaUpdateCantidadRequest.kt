package com.beaumanoir.gestock.data.remote.dto.caja

data class CajaUpdateCantidadRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)