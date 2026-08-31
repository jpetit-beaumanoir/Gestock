package com.beaumanoir.gestock.data.remote.dto.producto

data class ProductoGetRequest(
    val ean: String,
    val codemag: Int
)
