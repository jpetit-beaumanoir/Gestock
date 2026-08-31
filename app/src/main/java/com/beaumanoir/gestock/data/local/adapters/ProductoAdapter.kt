package com.beaumanoir.gestock.data.local.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.models.producto.Producto

class ProductoAdapter(
    private val almacenVirtualList: List<Producto>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtual: Producto)
    }

    inner class ProductoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val eanTextView: TextView = itemView.findViewById(R.id.ean_producto)
        val colorTextView: TextView = itemView.findViewById(R.id.color_producto)
        val tallaTextView: TextView = itemView.findViewById(R.id.talla_producto)
        val nombreTextView: TextView = itemView.findViewById(R.id.nombre_producto)
        val temporadaTextView: TextView = itemView.findViewById(R.id.temporada_producto)
        val cantidadTextView: TextView = itemView.findViewById(R.id.cantidad_producto)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(almacenVirtualList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = almacenVirtualList[position]
        holder.eanTextView.text = producto.ean
        holder.colorTextView.text = producto.color
        holder.tallaTextView.text = producto.talla
        holder.nombreTextView.text = producto.nombre
        holder.temporadaTextView.text = producto.temporada
        holder.cantidadTextView.text = producto.cantidad.toString()
        if (holder.cantidadTextView.text == "0") {
            holder.cantidadTextView.visibility = View.GONE
        }
        holder.itemView.isClickable = false
        holder.eanTextView.isClickable = false
        holder.colorTextView.isClickable = false
        holder.tallaTextView.isClickable = false
        holder.nombreTextView.isClickable = false
        holder.temporadaTextView.isClickable = false
        holder.cantidadTextView.isClickable = false
        holder.itemView.isFocusable = false
        holder.eanTextView.isFocusable = false
        holder.colorTextView.isFocusable = false
        holder.tallaTextView.isFocusable = false
        holder.nombreTextView.isFocusable = false
        holder.temporadaTextView.isFocusable = false
        holder.cantidadTextView.isFocusable = false
        setAnimation(holder.itemView, position)
    }

    override fun getItemCount(): Int = almacenVirtualList.size

    private fun setAnimation(view: View, position: Int) {
        view.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.item_animation))
    }
}
