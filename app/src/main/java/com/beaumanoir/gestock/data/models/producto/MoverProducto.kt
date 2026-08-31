package com.beaumanoir.gestock.data.models.producto

data class MoverProducto(
    val id: Int,
    val ean: String,
    val color: String,
    val talla: String,
    val nombre: String,
    var cantidad: Int,
    var vecesBeepeado: Int,
    val temporada: String
)
