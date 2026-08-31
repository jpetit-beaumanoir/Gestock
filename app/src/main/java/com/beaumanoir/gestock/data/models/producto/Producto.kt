package com.beaumanoir.gestock.data.models.producto

data class Producto (
    val ean: String,
    val color: String,
    val talla: String,
    val nombre: String,
    val temporada: String,
    val cantidad: Int
)