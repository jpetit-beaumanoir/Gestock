package com.beaumanoir.gestock.data.sqlite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.Almacenes

class AlmacenAdapter(
    private val almacenVirtualList: List<Almacenes>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<AlmacenAdapter.AlmacenViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtual: Almacenes)
    }

    inner class AlmacenViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val nombreAlmacenTextView: TextView = itemView.findViewById(R.id.nombre_almacen_adapter)
        val codigoAlmacenTextView: TextView = itemView.findViewById(R.id.codigo_almacen_adapter)
        val paletsAlmacenTextView: TextView = itemView.findViewById(R.id.palet_almacen_adapter)
        val cajasAlmacenTextView: TextView = itemView.findViewById(R.id.caja_almacen_adapter)
        val cantidadAlmacenTextView: TextView = itemView.findViewById(R.id.cantidad_almacen_adapter)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(almacenVirtualList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlmacenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_almacen, parent, false)
        return AlmacenViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlmacenViewHolder, position: Int) {
        val almacen = almacenVirtualList[position]
        holder.nombreAlmacenTextView.text = almacen.nombre.uppercase()
        holder.codigoAlmacenTextView.text = almacen.codigo.toString().padStart(4, '0')
        holder.paletsAlmacenTextView.text = almacen.palets.toString()
        holder.cajasAlmacenTextView.text = almacen.cajas.toString()
        holder.cantidadAlmacenTextView.text = almacen.cantidad.toString()

        if (holder.cantidadAlmacenTextView.length() > 5 ||
            holder.paletsAlmacenTextView.length() > 3 ||
            holder.cajasAlmacenTextView.length() > 4
        ) {
            holder.cantidadAlmacenTextView.textSize = 24.0f
            holder.cajasAlmacenTextView.textSize = 24.0f
            holder.paletsAlmacenTextView.textSize = 24.0f
        }
        setAnimation(holder.itemView)
    }

    override fun getItemCount(): Int = almacenVirtualList.size

    private fun setAnimation(view: View) {
        view.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.item_animation))
    }
}
