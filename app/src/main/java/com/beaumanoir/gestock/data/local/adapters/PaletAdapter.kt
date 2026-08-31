package com.beaumanoir.gestock.data.local.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beaumanoir.gestock.R
import com.beaumanoir.gestock.data.models.palet.Palet

class PaletAdapter(
    private val almacenVirtualList: List<Palet>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PaletAdapter.PaletViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(almacenVirtual: Palet)
    }

    inner class PaletViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val idPaletTextView: TextView = itemView.findViewById(R.id.id_palet)
        val idCajaTextView: TextView = itemView.findViewById(R.id.id_caja)
        val idCantidadTextView: TextView = itemView.findViewById(R.id.cantidad)

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
            .inflate(R.layout.adapter_palet, parent, false)
        return PaletViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaletViewHolder, position: Int) {
        val palet = almacenVirtualList[position]
        holder.idPaletTextView.text = palet.palet.toString()
        holder.idCajaTextView.text = palet.cajas.toString()
        holder.idCantidadTextView.text = palet.cantidad.toString()
        setAnimation(holder.itemView, position)
    }

    override fun getItemCount(): Int = almacenVirtualList.size

    private fun setAnimation(view: View, position: Int) {
        view.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.item_animation))
    }
}
