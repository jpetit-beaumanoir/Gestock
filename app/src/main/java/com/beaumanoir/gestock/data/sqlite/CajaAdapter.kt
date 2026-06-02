package com.beaumanoir.gestock.data.sqlite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.AlmacenVirtualCaja

class CajaAdapter(
    private val almacenVirtualList: List<AlmacenVirtualCaja>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<CajaAdapter.PaletViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtual: AlmacenVirtualCaja)
    }

    inner class PaletViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val idCajaTextView: TextView = itemView.findViewById(R.id.id_caja)
        val temporadaTextView: TextView = itemView.findViewById(R.id.textview_temporada)
        val descripcionTextView: TextView = itemView.findViewById(R.id.textview_descripcion)
        val cantidadTextView: TextView = itemView.findViewById(R.id.cantidadTextView)

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_cajas, parent, false)
        return PaletViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaletViewHolder, position: Int) {
        val caja = almacenVirtualList[position]
        holder.idCajaTextView.text = caja.caja.toString()
        holder.temporadaTextView.text = caja.temporada
        holder.descripcionTextView.text = caja.descripcion
        holder.cantidadTextView.text = caja.cantidad.toString()
        setAnimation(holder.itemView, position)
    }

    override fun getItemCount(): Int = almacenVirtualList.size

    private fun setAnimation(view: View, position: Int) {
        view.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.item_animation))
    }
}
