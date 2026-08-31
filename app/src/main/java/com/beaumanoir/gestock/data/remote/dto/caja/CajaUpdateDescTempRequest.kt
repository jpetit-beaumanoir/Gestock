package com.beaumanoir.gestock.data.remote.dto.caja

data class CajaUpdateDescTempRequest(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val descripcion: String,
    val temporada: String
)