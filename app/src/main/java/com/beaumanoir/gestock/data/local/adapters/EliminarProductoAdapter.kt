package com.beaumanoir.gestock.data.local.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.models.producto.EliminarProducto

class EliminarProductoAdapter(
    private val items: List<EliminarProducto>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<EliminarProductoAdapter.MyViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtualEliminar: EliminarProducto)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nombreProducto: TextView = itemView.findViewById(R.id.nombre_producto)
        private val colorProducto: TextView = itemView.findViewById(R.id.color_producto)
        private val tallaProducto: TextView = itemView.findViewById(R.id.talla_producto)
        private val eanProducto: TextView = itemView.findViewById(R.id.ean_producto)
        private val stockBeepeado: TextView = itemView.findViewById(R.id.stock_beepeado)
        private val productosTotales: TextView = itemView.findViewById(R.id.productos_totales)
        private val temporadaProducto: TextView = itemView.findViewById(R.id.temporada_producto)

        fun bind(almacenVirtualEliminarProducto: EliminarProducto) {
            nombreProducto.text = almacenVirtualEliminarProducto.nombre
            colorProducto.text = almacenVirtualEliminarProducto.color
            tallaProducto.text = almacenVirtualEliminarProducto.talla
            eanProducto.text = almacenVirtualEliminarProducto.ean
            stockBeepeado.text = almacenVirtualEliminarProducto.cantidad.toString()
            productosTotales.text = almacenVirtualEliminarProducto.cantidad.toString()
            temporadaProducto.text = almacenVirtualEliminarProducto.temporada
            itemView.setOnClickListener {
                listener.onItemClick(almacenVirtualEliminarProducto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_eliminar_producto, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun actualizarCantidadEscaneada(ean: String) {
        val producto = items.firstOrNull { it.ean == ean }
        if (producto != null && producto.vecesBeepeado < producto.cantidad) {
            producto.vecesBeepeado += 1
            notifyDataSetChanged()
        }
    }
}
