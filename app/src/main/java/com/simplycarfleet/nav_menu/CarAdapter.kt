package com.simplycarfleet.nav_menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simplycarfleet.R
import com.simplycarfleet.data.Car
import kotlin.collections.ArrayList

class CarAdapter(private val carList: ArrayList<Car>) :
    RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    // Inne zmienne
    private lateinit var mListener: OnItemClickListener
    private lateinit var mListener2: OnCarMoreOptionsClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int, car: Car)
    }

    interface OnCarMoreOptionsClickListener {
        fun onItemClick(position: Int, car: Car)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnCarMoreOptionsClickListener(listener: OnCarMoreOptionsClickListener) {
        mListener2 = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_row, parent, false)
        return CarViewHolder(view, mListener, mListener2)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car: Car = carList[position]
        holder.carBrand.text = car.brand + " " + car.model
        Glide.with(holder.itemView)
            .load(carList[holder.adapterPosition].carImageId)
            .into(holder.carPhoto)
    }

    override fun getItemCount(): Int {
        return carList.size
    }

    inner class CarViewHolder(
        view: View,
        listener: OnItemClickListener,
        listener2: OnCarMoreOptionsClickListener
    ) :
        RecyclerView.ViewHolder(view) {
        val carBrand: TextView = view.findViewById(R.id.car_brand)
        private val carMoreOptions: ImageView = view.findViewById(R.id.car_more_option)
        val carPhoto: ImageView = view.findViewById(R.id.car_photo)
        val linearLayout: LinearLayout = view.findViewById(R.id.linearLayoutCar)

        init {
            view.setOnClickListener {
                listener.onItemClick(adapterPosition, carList[adapterPosition])
            }
            carMoreOptions.setOnClickListener {
                listener2.onItemClick(adapterPosition, carList[adapterPosition])
            }

        }
    }
}