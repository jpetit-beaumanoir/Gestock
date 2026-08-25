package com.beaumanoir.gestock.data.schemas.caja

data class CajaUpdateDescTempResponse(
    val almacen: Int,
    val palet: Int,
    val caja: Int,
    val descripcion: String,
    val temporada: String
)