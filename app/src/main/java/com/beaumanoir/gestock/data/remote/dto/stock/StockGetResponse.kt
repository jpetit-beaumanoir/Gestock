package com.beaumanoir.gestock.data.remote.dto.stock

data class StockGetResponse(
    val stock: Map<String, StockInfo>,
    val total: Int
)