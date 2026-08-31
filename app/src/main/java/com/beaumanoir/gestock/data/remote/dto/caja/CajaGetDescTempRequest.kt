package com.beaumanoir.gestock.data.remote.dto.caja

data class CajaGetDescTempRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int
)