package com.beaumanoir.gestock.data.schemas.caja

data class CajaGetDescTempRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)