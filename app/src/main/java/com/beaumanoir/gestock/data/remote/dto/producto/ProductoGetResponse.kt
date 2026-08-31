package com.beaumanoir.gestock.data.remote.dto.producto

data class ProductoGetResponse(
    val ean: String,
    val nombre: String,
    val familia: String,
    val subfamilia: String,
    val color: String,
    val talla: String,
    val pvp: Double,
    val prmp: Double,
    val temporada: String,
    val marca: String
)