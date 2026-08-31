package com.beaumanoir.gestock.data.models.almacen

data class Almacen(
    val codigo: String,
    val nombre: String,
    val palets: Int,
    val cajas: Int,
    val cantidad: Int
)
