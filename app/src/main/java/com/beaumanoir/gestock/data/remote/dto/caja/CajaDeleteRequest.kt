package com.beaumanoir.gestock.data.remote.dto.caja

data class CajaDeleteRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)