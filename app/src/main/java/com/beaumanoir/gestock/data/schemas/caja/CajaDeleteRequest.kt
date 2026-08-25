package com.beaumanoir.gestock.data.schemas.caja

data class CajaDeleteRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)