package com.beaumanoir.gestock.data.models.producto

data class EliminarProducto(
    val id: Int,
    val ean: String,
    val color: String,
    val talla: String,
    val nombre: String,
    val cantidad: Int,
    var vecesBeepeado: Int,
    val temporada: String
)
