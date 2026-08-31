package com.beaumanoir.gestock.data.local.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.models.producto.MoverProducto

class MoverProductoAdapter(
    private val items: List<MoverProducto>,
    private val listener: OnItemClickListener
): RecyclerView.Adapter<MoverProductoAdapter.MyViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtualMover: MoverProducto)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nombreProducto: TextView = itemView.findViewById(R.id.nombre_producto)
        private val colorProducto: TextView = itemView.findViewById(R.id.color_producto)
        private val tallaProducto: TextView = itemView.findViewById(R.id.talla_producto)
        private val eanProducto: TextView = itemView.findViewById(R.id.ean_producto)
        private val stockBeepeado: TextView = itemView.findViewById(R.id.stock_beepeado)
        private val productosTotales: TextView = itemView.findViewById(R.id.productos_totales)
        private val temporadaProducto: TextView = itemView.findViewById(R.id.temporada_producto)

        fun bind(almacenVirtualMoverProducto: MoverProducto) {
            nombreProducto.text = almacenVirtualMoverProducto.nombre
            colorProducto.text = almacenVirtualMoverProducto.color
            tallaProducto.text = almacenVirtualMoverProducto.talla
            eanProducto.text = almacenVirtualMoverProducto.ean
            stockBeepeado.text = almacenVirtualMoverProducto.cantidad.toString()
            productosTotales.text = almacenVirtualMoverProducto.cantidad.toString()
            temporadaProducto.text = almacenVirtualMoverProducto.temporada
            itemView.setOnClickListener {
                listener.onItemClick(almacenVirtualMoverProducto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_producto, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}