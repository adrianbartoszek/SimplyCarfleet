package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.common.base.Functions
import com.simplycarfleet.R
import com.simplycarfleet.data.Statistics
import com.simplycarfleet.functions.FunctionsAndValues

class HomeAdapter(
    private val statisticsList: ArrayList<Statistics>, c: Context
) :
    RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {
    // Inne zmienne
    private lateinit var mListener: OnItemClickListener
    private lateinit var mListener2: OnStatisticMoreOptionsClickListener
    private val preferences: SharedPreferences =
        c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)

    interface OnItemClickListener {
        fun onItemClick(position: Int, statistics: Statistics)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    interface OnStatisticMoreOptionsClickListener {
        fun onItemClick(position: Int, statistics: Statistics)
    }

    fun setOnStatisticMoreOptionsClickListener(listener: OnStatisticMoreOptionsClickListener) {
        mListener2 = listener
    }

    fun saveDataToSharedPreferences(
        c: Context,
        value: String,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putString(valueIdToReference, value)
        }.apply()
    }

    inner class HomeViewHolder(
        view: View,
        listener: OnItemClickListener,
        listener2: OnStatisticMoreOptionsClickListener
    ) :
        RecyclerView.ViewHolder(view) {
        val statisticImageView: ImageView = view.findViewById(R.id.statistic_image_view)
        val statisticDateTextView: TextView = view.findViewById(R.id.statistic_date_text_view)
        val statisticCarMileageTextView: TextView =
            view.findViewById(R.id.statistic_car_mileage_text_view)
        val statisticFuelAmountOrWorkshopTextView: TextView =
            view.findViewById(R.id.statistic_fuel_amount_or_workshop_text_view)
        val statisticUnitFuelAmountOrWorkshopTextView: TextView =
            view.findViewById(R.id.statistic_unit_fuel_amount_or_workshop_text_view)
        val statisticDistanceTravelledOrServiceTypeTextView: TextView =
            view.findViewById(R.id.statistic_distance_travelled_or_service_type_text_view)
        val statisticUnitDistanceTravelledOrServiceTypeTextView: TextView =
            view.findViewById(R.id.statistic_unit_distance_travelled_or_service_type_text_view)
        val statisticTotalCostTextView: TextView =
            view.findViewById(R.id.statistic_total_cost_text_view)
        val statisticUnitTotalCostTextView: TextView =
            view.findViewById(R.id.statistic_unit_total_cost_text_view)
        val statisticMoreOptions: ImageView = view.findViewById(R.id.statistic_more_option)

        init {
            view.setOnClickListener {
                listener.onItemClick(adapterPosition, statisticsList[adapterPosition])
            }
            statisticMoreOptions.setOnClickListener {
                listener2.onItemClick(adapterPosition, statisticsList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_row_home, parent, false)

        return HomeViewHolder(view, mListener, mListener2)
    }


    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val statistics: Statistics = statisticsList[position]
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")
        // Wspólne pola
        holder.statisticDateTextView.text = statistics.statisticDate
        holder.statisticCarMileageTextView.text = holder.itemView.context.getString(
            R.string.home_adapter_statistic_car_mileage,
            statistics.carMileage
        )
        holder.statisticTotalCostTextView.text = statistics.totalCost
        holder.statisticUnitTotalCostTextView.text =
            unitTotalCost

        when (statistics.typeOfStatistic) {
            "Serwis" -> {
                holder.statisticImageView.setImageResource(R.drawable.ic_baseline_service_24)
                holder.statisticImageView.setColorFilter(
                    ContextCompat.getColor(
                        holder.statisticImageView.context,
                        R.color.serviceIcon
                    )
                )
                holder.statisticFuelAmountOrWorkshopTextView.text = statistics.carWorkshop
                holder.statisticDistanceTravelledOrServiceTypeTextView.text = statistics.serviceType
                // Ustawianie View dla każdego typu statystyki, aby pola nie znikały
                holder.statisticUnitFuelAmountOrWorkshopTextView.visibility = View.GONE
                holder.statisticUnitDistanceTravelledOrServiceTypeTextView.visibility = View.GONE
                holder.statisticDistanceTravelledOrServiceTypeTextView.visibility = View.VISIBLE
                holder.statisticUnitTotalCostTextView.visibility = View.VISIBLE
            }
            "Tankowanie" -> {
                holder.statisticImageView.setImageResource(R.drawable.ic_baseline_refueling_24)
                holder.statisticImageView.setColorFilter(
                    ContextCompat.getColor(
                        holder.statisticImageView.context,
                        R.color.refuelIcon
                    )
                )
                holder.statisticFuelAmountOrWorkshopTextView.text = statistics.fuelAmount
                holder.statisticUnitFuelAmountOrWorkshopTextView.text = unitOfVolume
                holder.statisticDistanceTravelledOrServiceTypeTextView.text =
                    holder.itemView.context.getString(
                        R.string.home_adapter_statistic_distance_travelled,
                        statistics.distanceTravelledSinceRefueling
                    )
                holder.statisticUnitDistanceTravelledOrServiceTypeTextView.text =
                    unitOfDistance
                // Ustawianie View dla każdego typu statystyki, aby pola nie znikały
                holder.statisticUnitFuelAmountOrWorkshopTextView.visibility = View.VISIBLE
                holder.statisticUnitDistanceTravelledOrServiceTypeTextView.visibility = View.VISIBLE
                holder.statisticDistanceTravelledOrServiceTypeTextView.visibility = View.VISIBLE
                holder.statisticUnitTotalCostTextView.visibility = View.VISIBLE
            }
            "Wydatek" -> {
                holder.statisticImageView.setImageResource(R.drawable.ic_baseline_monetization_on_24)
                holder.statisticImageView.setColorFilter(
                    ContextCompat.getColor(
                        holder.statisticImageView.context,
                        R.color.expenditureIcon
                    )
                )
                holder.statisticFuelAmountOrWorkshopTextView.text = statistics.expenditureType
                // Ustawianie View dla każdego typu statystyki, aby pola nie znikały
                holder.statisticUnitFuelAmountOrWorkshopTextView.visibility = View.GONE
                holder.statisticUnitDistanceTravelledOrServiceTypeTextView.visibility = View.GONE
                holder.statisticDistanceTravelledOrServiceTypeTextView.visibility = View.GONE
                holder.statisticUnitTotalCostTextView.visibility = View.VISIBLE

            }
            "Notatka" -> {
                holder.statisticImageView.setImageResource(R.drawable.ic_baseline_notes_24)
                holder.statisticImageView.setColorFilter(
                    ContextCompat.getColor(
                        holder.statisticImageView.context,
                        R.color.noteIcon
                    )
                )
                holder.statisticFuelAmountOrWorkshopTextView.text = statistics.typeOfStatistic
                // Ustawianie View dla każdego typu statystyki, aby pola nie znikały
                holder.statisticUnitFuelAmountOrWorkshopTextView.visibility = View.GONE
                holder.statisticUnitDistanceTravelledOrServiceTypeTextView.visibility = View.GONE
                holder.statisticDistanceTravelledOrServiceTypeTextView.visibility = View.GONE
                holder.statisticUnitTotalCostTextView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return statisticsList.size
    }
}
