package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentChartsBinding
import com.simplycarfleet.functions.FunctionsAndValues

class ChartsFragment : FunctionsAndValues() {
    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Usunięcie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.hide()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

        // Rysowanie odpowiedniego wykresu w zależności od otrzymanej wartości
        when (preferences.getString("chartTitleToRef", " ").toString()) {
            getString(R.string.statistics_fragment_chart_title_entry_cost) ->
                createChart(
                    preferences.getString("chartTitleTotalCost", " ").toString(),
                    preferences.getString("chartSubtitleTotalCost", " ").toString(),
                    preferences.getString("chartYAxisTitleTotalCost", " ").toString(),
                    preferences.getString("nameOfSeriesTotalCost", " ").toString(),
                    loadArrayFromSharedPreferences(requireContext(), "totalCostList")
                )
            getString(R.string.statistics_fragment_chart_title_total_fuel_cost) ->
                createChart(
                    preferences.getString("chartTitleTotalFuelCost", " ").toString(),
                    preferences.getString("chartSubtitleTotalFuelCost", " ").toString(),
                    preferences.getString("chartYAxisTitleTotalFuelCost", " ").toString(),
                    preferences.getString("nameOfSeriesTotalFuelCost", " ").toString(),
                    loadArrayFromSharedPreferences(requireContext(), "totalFuelCostList")
                )
            getString(R.string.statistics_fragment_chart_title_fuel_amount) ->
                createChart(
                    preferences.getString("chartTitleTotalFuelAmount", " ").toString(),
                    preferences.getString("chartSubtitleTotalFuelAmount", " ").toString(),
                    preferences.getString("chartYAxisTitleTotalFuelAmount", " ").toString(),
                    preferences.getString("nameOfSeriesTotalFuelAmount", " ").toString(),
                    loadArrayFromSharedPreferences(requireContext(), "totalFuelAmountList")
                )
            getString(R.string.statistics_fragment_chart_title_fuel_price_per_unit) ->
                createChart(
                    preferences.getString("chartTitleAverageFuelPrice", " ").toString(),
                    preferences.getString("chartSubtitleAverageFuelPrice", " ").toString(),
                    preferences.getString("chartYAxisTitleAverageFuelPrice", " ").toString(),
                    preferences.getString("nameOfSeriesAverageFuelPrice", " ").toString(),
                    loadArrayFromSharedPreferences(
                        requireContext(),
                        "averageFuelPriceForAmountList"
                    )
                )
            getString(R.string.statistics_fragment_chart_title_average_distance_between_refueling) ->
                createChart(
                    preferences.getString("chartTitleDistanceBetweenRefueling", " ").toString(),
                    preferences.getString("chartSubtitleDistanceBetweenRefueling", " ").toString(),
                    preferences.getString("chartYAxisTitleDistanceBetweenRefueling", " ")
                        .toString(),
                    preferences.getString("nameOfSeriesDistanceBetweenRefueling", " ").toString(),
                    loadArrayFromSharedPreferences(
                        requireContext(),
                        "averageDistanceBetweenRefuelingList"
                    )
                )
            getString(R.string.statistics_fragment_chart_title_service_cost) ->
                createChart(
                    preferences.getString("chartTitleServiceCost", " ").toString(),
                    preferences.getString("chartSubtitleServiceCost", " ").toString(),
                    preferences.getString("chartYAxisTitleServiceCost", " ").toString(),
                    preferences.getString("nameOfSeriesServiceCost", " ").toString(),
                    loadArrayFromSharedPreferences(requireContext(), "totalServiceCostList")
                )
            getString(R.string.statistics_fragment_chart_title_expenditure_cost) ->
                createChart(
                    preferences.getString("chartTitleExpenditureCost", " ").toString(),
                    preferences.getString("chartSubtitleExpenditureCost", " ").toString(),
                    preferences.getString("chartYAxisTitleExpenditureCost", " ").toString(),
                    preferences.getString("nameOfSeriesExpenditureCost", " ").toString(),
                    loadArrayFromSharedPreferences(requireContext(), "totalExpenditureCostList")
                )
        }
        binding.buttonCancelChartsFragment.setOnClickListener {
            replaceFragment(StatisticsFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        // W przypadku kliknięcia przycisku wstecz przekieruj do poprzedniego fragmentu
        requireView().setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                replaceFragment(StatisticsFragment())
                true
            } else false
        }
    }

    private fun createChart(
        title: String,
        subtitle: String,
        yAxisTitle: String,
        nameOfSeries: String,
        list: MutableList<Float>?
    ) {
        val aaChartView = binding.aaChartView
        val aaChartModel: AAChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .title(title)
            .subtitle(subtitle)
            .backgroundColor("#2D3940")
            .dataLabelsEnabled(true)
            .yAxisTitle(yAxisTitle)
            .axesTextColor("#ffffff")
            .colorsTheme(arrayOf("#49BFAA"))
            .series(
                arrayOf(
                    AASeriesElement()
                        .name(nameOfSeries)
                        .data(list!!.toTypedArray())
                )
            )
            .titleStyle(AAStyle.Companion.style("#ffffff"))
            .subtitleStyle(AAStyle.Companion.style("#49BFAA"))

        aaChartView.aa_drawChartWithChartModel(aaChartModel)
    }
}