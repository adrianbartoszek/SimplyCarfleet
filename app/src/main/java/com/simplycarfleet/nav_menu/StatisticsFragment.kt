package com.simplycarfleet.nav_menu

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.util.Pair
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentStatisticsBinding
import com.simplycarfleet.functions.FunctionsAndValues

class StatisticsFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pokazanie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.show()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)


        // Listy ID samochodów, marek
        val carBrandList: MutableList<String> = ArrayList()
        val carIdList: MutableList<String> = ArrayList()

        // Spinner do wyboru pojazdu
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, carBrandList)
        binding.spinnerStatistics.adapter = arrayAdapter
        binding.spinnerStatistics.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    // Zapisywanie ID wybranego pojazdu w celu przekazania go do innego fragmentu dodającej tankowania/serwisy itd.
                    sharedViewModel.saveCarId(carIdList[position])
                    // Zapisywanie pozycji spinnera w celu przekazania go do innego fragmentu
                    sharedViewModel.saveSpinnerPosition(position)
                    // Zapisanie ID wybranego pojazdu
                    saveDataToSharedPreferences(requireContext(), carIdList[position], "CAR ID")
                    // Zapisanie pozycji spinnera do SharedPreferences
                    saveDataIntToSharedPreferences(requireContext(), position, "SPINNER POSITION")
                    // Wywołanie funkcji obliczającej statystyki
                    // Pobranie z bazy daty pierwszego i ostatniego wpisu odnośnie wybranego pojazdu
                    getFirstAndLastDateFromFirebase()
                    makeStatistics()
                    binding.statisticsFilterValueTextview.text =
                        getString(R.string.fragment_statistics_filter_value)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        // Zapisanie danych do listy ID oraz listy marek
        cloudCarCollectionReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val carBrand = document.getString("brand")
                        val carId = document.getString("id")
                        //Dodawanie wszystkich marek samochodow do listy carBrandList
                        // oraz wszystkich ID do listy carIdList
                        carBrandList.add(carBrand.toString())
                        carIdList.add(carId.toString())
                    }
                    arrayAdapter.notifyDataSetChanged()
                }
                //Zapamietywanie pozycji spinnera

                val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)
                //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinner
                binding.spinnerStatistics.setSelection(spinnerPosition)
            }
            onComplete(result)
        }

        // Wywołanie funkji obliczającej statystyki po wejściu do fragmentu
        makeStatistics()

        binding.tabsStatistics.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> replaceFragment(HomeFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        // Ustawienie domyślnej pozycji tabs na Statystyki
        val tabDefault = binding.tabsStatistics.getTabAt(1)
        tabDefault?.select()

        // Listener na przycisk odnośnie filtrowania
        binding.statisticsFilterValueTextview.setOnClickListener {
            makeDialogForDataStatisticsFiltering()
        }

        binding.statisticsFilterButtonTextview.setOnClickListener {
            makeDialogForDataStatisticsFiltering()
        }

        binding.statsGeneralTotalCostLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_entry_cost),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.fuelTotalCostLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_total_fuel_cost),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.fuelTotalAmountLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_fuel_amount),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.averageFuelPriceLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_fuel_price_per_unit),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.averageDistanceBetweenRefuelingLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_average_distance_between_refueling),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.averageServiceCostLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_service_cost),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }

        binding.averageExpenditureCostLayout.setOnClickListener {
            saveDataToSharedPreferences(
                requireContext(),
                getString(R.string.statistics_fragment_chart_title_expenditure_cost),
                "chartTitleToRef"
            )
            replaceFragment(ChartsFragment())
        }
    }

    private fun makeStatistics() {

        var lastCarMileage = 0
        var firstCarMileage = 0
        var sumOfTotalFuelCost: Float
        val totalCostList: MutableList<Float> = ArrayList()
        val sumOfRefuelList: MutableList<String> = ArrayList()
        val sumOfServiceList: MutableList<String> = ArrayList()
        val sumOfExpenditureList: MutableList<String> = ArrayList()
        val sumOfNotesList: MutableList<String> = ArrayList()
        val totalFuelAmountList: MutableList<Float> = ArrayList()
        val totalFuelCostList: MutableList<Float> = ArrayList()
        val averageFuelPriceForAmountList: MutableList<Float> = ArrayList()
        val totalServiceCostList: MutableList<Float> = ArrayList()
        val totalExpenditureCostList: MutableList<Float> = ArrayList()
        val averageDistanceBetweenRefuelingList: MutableList<Int> = ArrayList()

        // Czyszczenie list przy każdym wywołaniu aby
        // uniknąć mnożenia wartości przy wejściu do fragmentu
        totalCostList.clear()
        sumOfRefuelList.clear()
        sumOfServiceList.clear()
        sumOfExpenditureList.clear()
        sumOfNotesList.clear()
        totalFuelAmountList.clear()
        totalFuelCostList.clear()
        averageFuelPriceForAmountList.clear()
        totalServiceCostList.clear()
        totalExpenditureCostList.clear()
        averageDistanceBetweenRefuelingList.clear()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val carId = preferences.getString("CAR ID", "")
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        var firstDateInMillis = ""
        var lastDateInMillis = ""

        // Odwołanie do kolekcji Car_statistics wybranego pojazdu
        val cloudStatisticsCollectionReference = cloudCarCollectionReference
            .document(carId.toString())
            .collection("Car_statistics")

        //Odwołanie do pierwszej daty
        val firstDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.ASCENDING
            )
                .limit(1)

        //Odwołanie do ostatniej daty
        val lastDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.DESCENDING
            )
                .limit(1)


        //Odwołanie do pierwszego przebiegu
        val firstCarMileageReference =
            cloudStatisticsCollectionReference
                .orderBy("statisticDateInMillis", Query.Direction.ASCENDING)
                .limit(1)

        //Odwołanie do ostatniego przebiegu
        val lastCarMileageReference =
            cloudStatisticsCollectionReference
                .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                .limit(1)

        //Odwołanie do tankowań
        val refuelReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Tankowanie")

        //Odwołanie do serwisów
        val serviceReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Serwis")

        //Odwołanie do wydatków
        val expenditureReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Wydatek")

        //Odwołanie do notatek
        val notesReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Notatka")

        //Pobranie pierwszej daty w milisekundach
        firstDateReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        firstDateInMillis = document.getString("statisticDateInMillis").toString()
                    }
                }
                //Pobranie ostatniej daty w milisekundach
                lastDateReference.get().addOnCompleteListener { result ->
                    fun onComplete(task: Task<QuerySnapshot?>) {
                        if (task.isSuccessful) {
                            for (document in task.result!!) {
                                lastDateInMillis =
                                    System.currentTimeMillis().toString()
                            }
                            //Zapisanie sumy kosztów serwisów do listy totalServiceCostList
                            // oraz dziennego kosztu serwisu oraz średniego kosztu
                            serviceReference.get().addOnCompleteListener { result ->
                                fun onComplete(task: Task<QuerySnapshot?>) {
                                    if (task.isSuccessful) {
                                        for (document in task.result!!) {
                                            val totalServiceCostAmount =
                                                document.getString("totalCost").toString()
                                            if (totalServiceCostAmount != "null") {
                                                totalServiceCostList.add(totalServiceCostAmount.toFloat())
                                            }
                                        }

                                        // Zapisanie danych na potrzeby wykresu cen serwisów
                                        saveArrayToSharedPreferences(
                                            requireContext(),
                                            totalServiceCostList,
                                            "totalServiceCostList"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_service_cost),
                                            "chartTitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_whole_history),
                                            "chartSubtitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                            "chartYAxisTitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_service),
                                            "nameOfSeriesServiceCost"
                                        )

                                        val sumOfTotalServiceCost = totalServiceCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT SERWISÓW > 0
                                        if (sumOfTotalServiceCost > 0) {
                                            binding.statsServiceTotalCostValueTextview.text =
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalServiceCost
                                                )
                                                    .replace(
                                                        ',',
                                                        '.'
                                                    ) + " [${unitTotalCost?.trim()}]"
                                        } else {
                                            binding.statsServiceTotalCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                        // DZIENNY KOSZT SERWISU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                (lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong())).div(
                                                        dayInMillis.toLong()
                                                    )
                                            if (dailyServiceCostDayDifference != 0L) {
                                                val dailyServiceCost =
                                                    sumOfTotalServiceCost.toInt().div(
                                                        dailyServiceCostDayDifference
                                                    )

                                                // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                                if (dailyServiceCost > 0) {
                                                    binding.statsServiceDailyCostValueTextview.text =
                                                        formatToDisplayTwoDecimalPlaces.format(
                                                            dailyServiceCost
                                                        ) + " [${unitTotalCost?.trim()}]"
                                                } else {
                                                    binding.statsServiceDailyCostValueTextview.text =
                                                        getString(R.string.statistic_no_data)
                                                }
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis == lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                (lastDateInMillis.toLong()).div(
                                                    dayInMillis.toLong()
                                                )
                                            val dailyServiceCost =
                                                sumOfTotalServiceCost.toInt().div(
                                                    dailyServiceCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                            if (dailyServiceCost > 0 && totalServiceCostList.size > 1) {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (sumOfTotalServiceCost < 1 || sumOfTotalServiceCost.isNaN()) {
                                            binding.statsServiceDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsServiceAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        } else {
                                            binding.statsServiceDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsServiceAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                    }
                                }
                                onComplete(result)
                            }

                            //Zapisanie sumy kosztów wydatków do listy totalExpenditureCostList
                            // oraz dziennego kosztu wydatku oraz średniego kosztu
                            expenditureReference.get().addOnCompleteListener { result ->
                                @SuppressLint("SetTextI18n")
                                fun onComplete(task: Task<QuerySnapshot?>) {
                                    if (task.isSuccessful) {
                                        for (document in task.result!!) {
                                            val totalExpenditureCostAmount =
                                                document.getString("totalCost").toString()
                                            if (totalExpenditureCostAmount != "null") {
                                                totalExpenditureCostList.add(
                                                    totalExpenditureCostAmount.toFloat()
                                                )
                                            }
                                        }

                                        // Zapisanie danych na potrzeby wykresu cen wydatków
                                        saveArrayToSharedPreferences(
                                            requireContext(),
                                            totalExpenditureCostList,
                                            "totalExpenditureCostList"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_expenditure_cost),
                                            "chartTitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_whole_history),
                                            "chartSubtitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                            "chartYAxisTitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_expenditure),
                                            "nameOfSeriesExpenditureCost"
                                        )

                                        val sumOfTotalExpenditureCost =
                                            totalExpenditureCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT WYDATKÓW > 0
                                        if (sumOfTotalExpenditureCost > 0) {
                                            binding.statsExpenditureTotalCostValueTextview.text =
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalExpenditureCost
                                                )
                                                    .replace(
                                                        ',',
                                                        '.'
                                                    ) + " [${unitTotalCost?.trim()}]"
                                        } else {
                                            binding.statsExpenditureTotalCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                        // DZIENNY KOSZT WYDATKU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        Log.d("DateTest", firstDateInMillis)
                                        Log.d("DateTest", lastDateInMillis)
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyExpenditureCostDayDifference =
                                                (lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong())).div(
                                                        dayInMillis.toLong()
                                                    )
                                            if (dailyExpenditureCostDayDifference != 0L) {
                                                val dailyExpenditureCost =
                                                    sumOfTotalExpenditureCost.toInt().div(
                                                        dailyExpenditureCostDayDifference
                                                    )
                                                // SPRAWDZENIE CZY DZIENNY KOSZT WYDATKU > 0
                                                if (dailyExpenditureCost > 0 && totalExpenditureCostList.size > 1) {
                                                    binding.statsExpenditureDailyCostValueTextview.text =
                                                        formatToDisplayTwoDecimalPlaces.format(
                                                            dailyExpenditureCost
                                                        )
                                                            .replace(
                                                                ',',
                                                                '.'
                                                            ) + " [${unitTotalCost?.trim()}]"

                                                } else {
                                                    binding.statsExpenditureDailyCostValueTextview.text =
                                                        getString(R.string.statistic_no_data)
                                                }
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis == lastDateInMillis) {
                                            val dailyExpenditureCostDayDifference =
                                                (lastDateInMillis.toLong()).div(
                                                    dayInMillis.toLong()
                                                )
                                            val dailyExpenditureCost =
                                                sumOfTotalExpenditureCost.toInt().div(
                                                    dailyExpenditureCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT WYDATKU > 0
                                            if (dailyExpenditureCost > 0) {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (sumOfTotalExpenditureCost < 1 || sumOfTotalExpenditureCost.isNaN()) {
                                            binding.statsExpenditureDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsExpenditureAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        } else {
                                            binding.statsExpenditureDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsExpenditureAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }
                                    }
                                }
                                onComplete(result)
                            }
                        }
                    }
                    onComplete(result)
                }
            }
            onComplete(result)
        }

        // POBRANIE OSTATNIEGO PRZEBIEGU Z POMINIĘCIEM WYBRANYCH DAT
        val lastCarMileageSetTextValue = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId.toString())
            .collection("Car_statistics")
            .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
            .limit(1)

        // POBRANIE PIERWSZEGO PRZEBIEGU
        firstCarMileageReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        firstCarMileage = document.getString("carMileage").toString().toInt()
                    }
                    saveDataIntToSharedPreferences(
                        requireContext(),
                        firstCarMileage,
                        "firstCarMileage"
                    )
                    // POBRANIE OSTATNIEGO PRZEBIEGU
                    lastCarMileageReference.get().addOnCompleteListener { result ->
                        fun onComplete(task: Task<QuerySnapshot?>) {
                            if (task.isSuccessful) {
                                for (document in task.result!!) {
                                    lastCarMileage =
                                        document.getString("carMileage").toString().toInt()
                                }
                                saveDataIntToSharedPreferences(
                                    requireContext(),
                                    lastCarMileage,
                                    "lastCarMileage"
                                )

                                // ZAPISANIE CAŁKOWITEGO KOSZTU TANKOWAŃ DO LISTY totalFuelCostList
                                // OBLICZENIE KOSZTÓW PALIWA NA KILOMETR ORAZ PRZEJECHANEGO DYSTANSU
                                refuelReference.get().addOnCompleteListener { result ->
                                    fun onComplete(task: Task<QuerySnapshot?>) {
                                        if (task.isSuccessful) {
                                            for (document in task.result!!) {
                                                val totalFuelCostAmount =
                                                    document.getString("totalCost").toString()
                                                if (totalFuelCostAmount != "null") {
                                                    totalFuelCostList.add(totalFuelCostAmount.toFloat())
                                                }
                                            }
                                            sumOfTotalFuelCost = totalFuelCostList.sum()

                                            saveArrayToSharedPreferences(
                                                requireContext(),
                                                totalFuelCostList,
                                                "totalFuelCostList"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_refueling_cost),
                                                "chartTitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_whole_history),
                                                "chartSubtitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                                "chartYAxisTitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_refueling),
                                                "nameOfSeriesTotalFuelCost"
                                            )

                                            val firstCarMileageSP =
                                                preferences.getInt("firstCarMileage", 0)
                                            val lastCarMileageSP =
                                                preferences.getInt("lastCarMileage", 0)
                                            val totalCarDistanceBetweenCarMileages =
                                                (lastCarMileageSP - firstCarMileageSP)

                                            val costPerDistance =
                                                sumOfTotalFuelCost.div(
                                                    totalCarDistanceBetweenCarMileages.toFloat()
                                                )

                                            // SPRAWDZENIE CZY KOSZT PALIWA NA KILOMETR > 0
                                            // ORAZ CZY LICZBA TANKOWAN > 1
                                            if (costPerDistance > 0 && totalFuelCostList.size > 1) {
                                                binding.statsRefuelingFuelCostPerKilometreValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        costPerDistance
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"
                                            } else {
                                                binding.statsRefuelingFuelCostPerKilometreValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // SPRAWDZENIE CZY CAŁKOWITY KOSZT TANKOWAŃ > 0
                                            if (sumOfTotalFuelCost > 0) {
                                                binding.statsRefuelingTotalCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        sumOfTotalFuelCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"
                                            } else {
                                                binding.statsRefuelingTotalCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }
                                            // SPRAWDZENIE CZY PRZEJECHANY DYSTANS > 0
                                            if (totalCarDistanceBetweenCarMileages > 0) {
                                                binding.statsGeneralCarMileageInApplicationValueTextview.text =
                                                    totalCarDistanceBetweenCarMileages.toString() + " [${unitOfDistance?.trim()}]"
                                            } else {
                                                binding.statsGeneralCarMileageInApplicationValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }
                                        }
                                    }
                                    onComplete(result)
                                }

                            }
                        }
                        onComplete(result)
                    }
                }
            }
            onComplete(result)
        }


        // POBRANIE OSTATNIEGO PRZEBIEGU Z POMINIĘCIEM DAT
        // I WPISANIE GO DO CAŁKOWITEGO PRZEBIEGU
        lastCarMileageSetTextValue.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                var lastCarMileageText = 0
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        lastCarMileageText = document.getString("carMileage").toString().toInt()
                    }
                    if (lastCarMileageText > 0) {
                        binding.statsGeneralCarMileageValueTextview.text =
                            lastCarMileageText.toString() + " [${unitOfDistance?.trim()}]"
                    } else {
                        binding.statsGeneralCarMileageValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE SUMY KOSZTÓW WSZYSTKICH STATYSTYK DO LISTY totalCostList
        cloudStatisticsCollectionReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalCost = document.getString("totalCost").toString()
                        if (totalCost != "null") {
                            totalCostList.add(totalCost.toFloat())
                        }
                    }

                    // Zapisanie danych na potrzeby wykresu cen wszystkich statystyk
                    saveArrayToSharedPreferences(
                        requireContext(),
                        totalCostList,
                        "totalCostList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_cost_refueling_expenditure_service),
                        "chartTitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_whole_history),
                        "chartSubtitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                        "chartYAxisTitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_entry_refueling_expenditure_service),
                        "nameOfSeriesTotalCost"
                    )

                    val sumOfTotalCost = totalCostList.sum()
                    if (sumOfTotalCost > 0) {
                        binding.statsGeneralTotalCostValueTextview.text =
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalCost)
                                .replace(',', '.') + " [${unitTotalCost?.trim()}]"
                    } else {
                        binding.statsGeneralTotalCostValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY TANKOWAŃ
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfRefuel = document.getString("typeOfStatistic").toString()
                        if (sumOfRefuel != "null") {
                            sumOfRefuelList.add(sumOfRefuel)
                        }
                    }
                    val sumOfRefueling = sumOfRefuelList.size
                    binding.statsGeneralRefuelingAmountValueTextview.text =
                        sumOfRefueling.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY SERWISÓW
        serviceReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfService = document.getString("typeOfStatistic").toString()
                        if (sumOfService != "null") {
                            sumOfServiceList.add(sumOfService)
                        }
                    }
                    val sumOfService = sumOfServiceList.size
                    binding.statsGeneralServiceAmountValueTextview.text = sumOfService.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY WYDATKÓW
        expenditureReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfExpenditure = document.getString("typeOfStatistic").toString()
                        if (sumOfExpenditure != "null") {
                            sumOfExpenditureList.add(sumOfExpenditure)
                        }
                    }
                    val sumOfExpenditure = sumOfExpenditureList.size
                    binding.statsGeneralExpenditureAmountValueTextview.text =
                        sumOfExpenditure.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY NOTATEK
        notesReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfNotes = document.getString("typeOfStatistic").toString()
                        if (sumOfNotes != "null") {
                            sumOfNotesList.add(sumOfNotes)
                        }
                    }
                    val sumOfNotes = sumOfNotesList.size
                    binding.statsGeneralNoteAmountValueTextview.text = sumOfNotes.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE ILOŚĆI ZATANKOWANEGO PALIWA DO LISTY totalFuelAmountList
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalFuelAmount = document.getString("fuelAmount").toString()
                        if (totalFuelAmount != "null") {
                            totalFuelAmountList.add(totalFuelAmount.toFloat())
                        }
                    }
                    val sumOfTotalFuelAmount = totalFuelAmountList.sum()

                    saveArrayToSharedPreferences(
                        requireContext(),
                        totalFuelAmountList,
                        "totalFuelAmountList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_fuel_amount),
                        "chartTitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_whole_history),
                        "chartSubtitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value_litres) + " [${unitOfVolume?.trim()}]",
                        "chartYAxisTitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesTotalFuelAmount"
                    )

                    if (sumOfTotalFuelAmount > 0) {
                        binding.statsRefuelingFuelAmountValueTextview.text =
                            sumOfTotalFuelAmount.toString() + " [${unitOfVolume?.trim()}]"
                    } else {
                        binding.statsRefuelingFuelAmountValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE ŚREDNIEJ CENY ZA LITR DO LISTY averageFuelPriceForAmountList
        // ORAZ OBLICZENIE ŚREDNIEGO DYSTANSU POMIĘDZY TANKOWANIAMI
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalCost = document.getString("totalCost").toString()
                        val fuelAmount = document.getString("fuelAmount").toString()
                        val distanceTravelled =
                            document.getString("distanceTravelledSinceRefueling").toString()
                        val averagePriceForDocument = totalCost.toFloat() / fuelAmount.toFloat()

                        if (totalCost != "null" && fuelAmount != "null" && distanceTravelled != "null") {
                            averageFuelPriceForAmountList.add(averagePriceForDocument)
                            averageDistanceBetweenRefuelingList.add(distanceTravelled.toInt())
                        }
                    }

                    // Zapisanie danych na potrzeby wykresu odnośnie średniej ceny za litr
                    saveArrayToSharedPreferences(
                        requireContext(),
                        averageFuelPriceForAmountList,
                        "averageFuelPriceForAmountList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_title_fuel_price_per_unit),
                        "chartTitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_whole_history),
                        "chartSubtitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                        "chartYAxisTitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesAverageFuelPrice"
                    )

                    // Zapisanie danych na potrzeby wykresu odnośnie średniego dystansu pomiędzy tankowaniami
                    saveArrayOfIntToSharedPreferences(
                        requireContext(),
                        averageDistanceBetweenRefuelingList,
                        "averageDistanceBetweenRefuelingList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_title_average_distance_between_refueling),
                        "chartTitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_whole_history),
                        "chartSubtitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value_avg_distance_unit) + " [${unitOfDistance?.trim()}]",
                        "chartYAxisTitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesDistanceBetweenRefueling"
                    )

                    val sumOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.sum()
                    val sizeOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.size
                    val averagePriceOfFuel =
                        sumOfAverageFuelPriceForAmountList / sizeOfAverageFuelPriceForAmountList

                    if (averagePriceOfFuel > 0) {
                        binding.statsRefuelingAveragePricePerUnitValueTextview.text =
                            formatToDisplayTwoDecimalPlaces.format(averagePriceOfFuel)
                                .replace(',', '.') + " [${unitTotalCost?.trim()}]"
                    } else {
                        binding.statsRefuelingAveragePricePerUnitValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }

                    if (averageDistanceBetweenRefuelingList.size > 0) {
                        val distanceTravelledBetweenRefueling =
                            averageDistanceBetweenRefuelingList.sum()
                                .div(averageDistanceBetweenRefuelingList.size)

                        binding.statsRefuelingAverageDistanceBetweenRefuelingValueTextview.text =
                            distanceTravelledBetweenRefueling.toString() + " [${unitOfDistance?.trim()}]"
                    } else {
                        binding.statsRefuelingAverageDistanceBetweenRefuelingValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }
    }

    private fun makeStatisticsWithDataFiltering(firstDate: String, lastDate: String) {
        var lastCarMileage = 0
        var firstCarMileage = 0
        var sumOfTotalFuelCost: Float
        val totalCostList: MutableList<Float> = ArrayList()
        val sumOfRefuelList: MutableList<String> = ArrayList()
        val sumOfServiceList: MutableList<String> = ArrayList()
        val sumOfExpenditureList: MutableList<String> = ArrayList()
        val sumOfNotesList: MutableList<String> = ArrayList()
        val totalFuelAmountList: MutableList<Float> = ArrayList()
        val totalFuelCostList: MutableList<Float> = ArrayList()
        val averageFuelPriceForAmountList: MutableList<Float> = ArrayList()
        val totalServiceCostList: MutableList<Float> = ArrayList()
        val totalExpenditureCostList: MutableList<Float> = ArrayList()
        val averageDistanceBetweenRefuelingList: MutableList<Int> = ArrayList()

        // Czyszczenie list przy każdym wywołaniu aby
        // uniknąć mnożenia wartości przy wejściu do fragmentu
        totalCostList.clear()
        sumOfRefuelList.clear()
        sumOfServiceList.clear()
        sumOfExpenditureList.clear()
        sumOfNotesList.clear()
        totalFuelAmountList.clear()
        totalFuelCostList.clear()
        averageFuelPriceForAmountList.clear()
        totalServiceCostList.clear()
        totalExpenditureCostList.clear()
        averageDistanceBetweenRefuelingList.clear()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val carId = preferences.getString("CAR ID", "")
        val selectedDate = preferences.getString("dateSelectedStatistics", "0").toString()
        val unitTotalCost = preferences.getString("currency", " ")
        val unitOfVolume = preferences.getString("unit_of_volume", " ")
        val unitOfDistance = preferences.getString("unit_of_distance", " ")

        var firstDateInMillis = ""
        var lastDateInMillis = ""

        // Odwołanie do kolekcji Car_statistics wybranego pojazdu
        val cloudStatisticsCollectionReference = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId.toString())
            .collection("Car_statistics")
            .whereGreaterThanOrEqualTo("statisticDateInMillis", firstDate)
            .whereLessThanOrEqualTo("statisticDateInMillis", lastDate)

        //Odwołanie do pierwszej daty
        val firstDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.ASCENDING
            )
                .limit(1)

        //Odwołanie do ostatniej daty
        val lastDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.DESCENDING
            )
                .limit(1)


        //Odwołanie do pierwszego przebiegu
        val firstCarMileageReference =
            cloudStatisticsCollectionReference
                .orderBy("statisticDateInMillis", Query.Direction.ASCENDING)
                .limit(1)

        //Odwołanie do ostatniego przebiegu
        val lastCarMileageReference =
            cloudStatisticsCollectionReference
                .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                .limit(1)

        //Odwołanie do tankowań
        val refuelReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Tankowanie")

        //Odwołanie do serwisów
        val serviceReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Serwis")

        //Odwołanie do wydatków
        val expenditureReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Wydatek")

        //Odwołanie do notatek
        val notesReference =
            cloudStatisticsCollectionReference.whereEqualTo("typeOfStatistic", "Notatka")

        //Pobranie pierwszej daty w milisekundach
        firstDateReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        firstDateInMillis = document.getString("statisticDateInMillis").toString()
                    }
                }
                //Pobranie ostatniej daty w milisekundach
                lastDateReference.get().addOnCompleteListener { result ->
                    fun onComplete(task: Task<QuerySnapshot?>) {
                        if (task.isSuccessful) {
                            for (document in task.result!!) {
                                lastDateInMillis =
                                    document.getString("statisticDateInMillis").toString()
                            }
                            //Zapisanie sumy kosztów serwisów do listy totalServiceCostList
                            // oraz dziennego kosztu serwisu oraz średniego kosztu
                            serviceReference.get().addOnCompleteListener { result ->
                                fun onComplete(task: Task<QuerySnapshot?>) {
                                    if (task.isSuccessful) {
                                        for (document in task.result!!) {
                                            val totalServiceCostAmount =
                                                document.getString("totalCost").toString()
                                            if (totalServiceCostAmount != "null") {
                                                totalServiceCostList.add(totalServiceCostAmount.toFloat())
                                            }
                                        }

                                        // Zapisanie danych na potrzeby wykresu cen serwisów
                                        saveArrayToSharedPreferences(
                                            requireContext(),
                                            totalServiceCostList,
                                            "totalServiceCostList"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_title_service_cost),
                                            "chartTitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            selectedDate,
                                            "chartSubtitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                            "chartYAxisTitleServiceCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_service),
                                            "nameOfSeriesServiceCost"
                                        )

                                        val sumOfTotalServiceCost = totalServiceCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT SERWISÓW > 0
                                        if (sumOfTotalServiceCost > 0) {
                                            binding.statsServiceTotalCostValueTextview.text =
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalServiceCost
                                                )
                                                    .replace(
                                                        ',',
                                                        '.'
                                                    ) + " [${unitTotalCost?.trim()}]"
                                        } else {
                                            binding.statsServiceTotalCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                        // DZIENNY KOSZT SERWISU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                (lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong())).div(
                                                        dayInMillis.toLong()
                                                    )
                                            val dailyServiceCost =
                                                sumOfTotalServiceCost.toInt().div(
                                                    dailyServiceCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                            if (dailyServiceCost > 0) {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis == lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                (lastDateInMillis.toLong()).div(
                                                    dayInMillis.toLong()
                                                )
                                            val dailyServiceCost =
                                                sumOfTotalServiceCost.toInt().div(
                                                    dailyServiceCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                            if (dailyServiceCost > 0 && totalServiceCostList.size > 1) {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsServiceAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (sumOfTotalServiceCost < 1 || sumOfTotalServiceCost.isNaN()) {
                                            binding.statsServiceDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsServiceAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        } else {
                                            binding.statsServiceDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsServiceAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                    }
                                }
                                onComplete(result)
                            }

                            //Zapisanie sumy kosztów wydatków do listy totalExpenditureCostList
                            // oraz dziennego kosztu wydatku oraz średniego kosztu
                            expenditureReference.get().addOnCompleteListener { result ->
                                @SuppressLint("SetTextI18n")
                                fun onComplete(task: Task<QuerySnapshot?>) {
                                    if (task.isSuccessful) {
                                        for (document in task.result!!) {
                                            val totalExpenditureCostAmount =
                                                document.getString("totalCost").toString()
                                            if (totalExpenditureCostAmount != "null") {
                                                totalExpenditureCostList.add(
                                                    totalExpenditureCostAmount.toFloat()
                                                )
                                            }
                                        }

                                        // Zapisanie danych na potrzeby wykresu cen wydatków
                                        saveArrayToSharedPreferences(
                                            requireContext(),
                                            totalExpenditureCostList,
                                            "totalExpenditureCostList"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_title_expenditure_cost),
                                            "chartTitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            selectedDate,
                                            "chartSubtitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                            "chartYAxisTitleExpenditureCost"
                                        )
                                        saveDataToSharedPreferences(
                                            requireContext(),
                                            getString(R.string.statistics_fragment_chart_expenditure),
                                            "nameOfSeriesExpenditureCost"
                                        )

                                        val sumOfTotalExpenditureCost =
                                            totalExpenditureCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT WYDATKÓW > 0
                                        if (sumOfTotalExpenditureCost > 0) {
                                            binding.statsExpenditureTotalCostValueTextview.text =
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalExpenditureCost
                                                )
                                                    .replace(
                                                        ',',
                                                        '.'
                                                    ) + " [${unitTotalCost?.trim()}]"
                                        } else {
                                            binding.statsExpenditureTotalCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }

                                        // DZIENNY KOSZT WYDATKU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        Log.d("DateTest", firstDateInMillis)
                                        Log.d("DateTest", lastDateInMillis)
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyExpenditureCostDayDifference =
                                                (lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong())).div(
                                                        dayInMillis.toLong()
                                                    )
                                            val dailyExpenditureCost =
                                                sumOfTotalExpenditureCost.toInt().div(
                                                    dailyExpenditureCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT WYDATKU > 0
                                            if (dailyExpenditureCost > 0 && totalExpenditureCostList.size > 1) {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis == lastDateInMillis) {
                                            val dailyExpenditureCostDayDifference =
                                                (lastDateInMillis.toLong()).div(
                                                    dayInMillis.toLong()
                                                )
                                            val dailyExpenditureCost =
                                                sumOfTotalExpenditureCost.toInt().div(
                                                    dailyExpenditureCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT WYDATKU > 0
                                            if (dailyExpenditureCost > 0) {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureDailyCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"

                                            } else {
                                                binding.statsExpenditureAverageCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }

                                        } else if (sumOfTotalExpenditureCost < 1 || sumOfTotalExpenditureCost.isNaN()) {
                                            binding.statsExpenditureDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsExpenditureAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        } else {
                                            binding.statsExpenditureDailyCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                            binding.statsExpenditureAverageCostValueTextview.text =
                                                getString(R.string.statistic_no_data)
                                        }
                                    }
                                }
                                onComplete(result)
                            }
                        }
                    }
                    onComplete(result)
                }
            }
            onComplete(result)
        }

        // POBRANIE OSTATNIEGO PRZEBIEGU Z POMINIĘCIEM WYBRANYCH DAT
        val lastCarMileageSetTextValue = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId.toString())
            .collection("Car_statistics")
            .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
            .limit(1)

        // POBRANIE PIERWSZEGO PRZEBIEGU
        firstCarMileageReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        firstCarMileage = document.getString("carMileage").toString().toInt()
                    }
                    saveDataIntToSharedPreferences(
                        requireContext(),
                        firstCarMileage,
                        "firstCarMileage"
                    )
                    // POBRANIE OSTATNIEGO PRZEBIEGU
                    lastCarMileageReference.get().addOnCompleteListener { result ->
                        fun onComplete(task: Task<QuerySnapshot?>) {
                            if (task.isSuccessful) {
                                for (document in task.result!!) {
                                    lastCarMileage =
                                        document.getString("carMileage").toString().toInt()
                                }
                                saveDataIntToSharedPreferences(
                                    requireContext(),
                                    lastCarMileage,
                                    "lastCarMileage"
                                )

                                // ZAPISANIE CAŁKOWITEGO KOSZTU TANKOWAŃ DO LISTY totalFuelCostList
                                // OBLICZENIE KOSZTÓW PALIWA NA KILOMETR ORAZ PRZEJECHANEGO DYSTANSU
                                refuelReference.get().addOnCompleteListener { result ->
                                    fun onComplete(task: Task<QuerySnapshot?>) {
                                        if (task.isSuccessful) {
                                            for (document in task.result!!) {
                                                val totalFuelCostAmount =
                                                    document.getString("totalCost").toString()
                                                if (totalFuelCostAmount != "null") {
                                                    totalFuelCostList.add(totalFuelCostAmount.toFloat())
                                                }
                                            }
                                            sumOfTotalFuelCost = totalFuelCostList.sum()

                                            saveArrayToSharedPreferences(
                                                requireContext(),
                                                totalFuelCostList,
                                                "totalFuelCostList"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_title_total_fuel_cost),
                                                "chartTitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                selectedDate,
                                                "chartSubtitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                                                "chartYAxisTitleTotalFuelCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.statistics_fragment_chart_refueling),
                                                "nameOfSeriesTotalFuelCost"
                                            )

                                            val firstCarMileageSP =
                                                preferences.getInt("firstCarMileage", 0)
                                            val lastCarMileageSP =
                                                preferences.getInt("lastCarMileage", 0)
                                            val totalCarDistanceBetweenCarMileages =
                                                (lastCarMileageSP - firstCarMileageSP)
                                            val costPerDistance =
                                                sumOfTotalFuelCost.div(
                                                    totalCarDistanceBetweenCarMileages.toFloat()
                                                )

                                            // ORAZ CZY LICZBA TANKOWAN > 1
                                            if (costPerDistance > 0 && totalFuelCostList.size > 1) {
                                                binding.statsRefuelingFuelCostPerKilometreValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        costPerDistance
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"
                                            } else {
                                                binding.statsRefuelingFuelCostPerKilometreValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }
                                            // SPRAWDZENIE CZY CAŁKOWITY KOSZT TANKOWAŃ > 0
                                            if (sumOfTotalFuelCost > 0) {
                                                binding.statsRefuelingTotalCostValueTextview.text =
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        sumOfTotalFuelCost
                                                    )
                                                        .replace(
                                                            ',',
                                                            '.'
                                                        ) + " [${unitTotalCost?.trim()}]"
                                            } else {
                                                binding.statsRefuelingTotalCostValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }
                                            // SPRAWDZENIE CZY PRZEJECHANY DYSTANS > 0
                                            if (totalCarDistanceBetweenCarMileages > 0) {
                                                binding.statsGeneralCarMileageInApplicationValueTextview.text =
                                                    totalCarDistanceBetweenCarMileages.toString() + " [${unitOfDistance?.trim()}]"
                                            } else {
                                                binding.statsGeneralCarMileageInApplicationValueTextview.text =
                                                    getString(R.string.statistic_no_data)
                                            }
                                        }
                                    }
                                    onComplete(result)
                                }

                            }
                        }
                        onComplete(result)
                    }
                }
            }
            onComplete(result)
        }


        // POBRANIE OSTATNIEGO PRZEBIEGU Z POMINIĘCIEM DAT
        // I WPISANIE GO DO CAŁKOWITEGO PRZEBIEGU
        lastCarMileageSetTextValue.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                var lastCarMileageText = 0
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        lastCarMileageText = document.getString("carMileage").toString().toInt()
                    }
                    if (lastCarMileageText > 0) {
                        binding.statsGeneralCarMileageValueTextview.text =
                            lastCarMileageText.toString() + " [${unitOfDistance?.trim()}]"
                    } else {
                        binding.statsGeneralCarMileageValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE SUMY KOSZTÓW WSZYSTKICH STATYSTYK DO LISTY totalCostList
        cloudStatisticsCollectionReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalCost = document.getString("totalCost").toString()
                        if (totalCost != "null") {
                            totalCostList.add(totalCost.toFloat())
                        }
                    }

                    // Zapisanie danych na potrzeby wykresu cen wszystkich statystyk
                    saveArrayToSharedPreferences(
                        requireContext(),
                        totalCostList,
                        "totalCostList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_cost_refueling_expenditure_service),
                        "chartTitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        selectedDate,
                        "chartSubtitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                        "chartYAxisTitleTotalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_entry_refueling_expenditure_service),
                        "nameOfSeriesTotalCost"
                    )

                    val sumOfTotalCost = totalCostList.sum()
                    if (sumOfTotalCost > 0) {
                        binding.statsGeneralTotalCostValueTextview.text =
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalCost)
                                .replace(',', '.') + " [${unitTotalCost?.trim()}]"
                    } else {
                        binding.statsGeneralTotalCostValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY TANKOWAŃ
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfRefuel = document.getString("typeOfStatistic").toString()
                        if (sumOfRefuel != "null") {
                            sumOfRefuelList.add(sumOfRefuel)
                        }
                    }
                    val sumOfRefueling = sumOfRefuelList.size
                    binding.statsGeneralRefuelingAmountValueTextview.text =
                        sumOfRefueling.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY SERWISÓW
        serviceReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfService = document.getString("typeOfStatistic").toString()
                        if (sumOfService != "null") {
                            sumOfServiceList.add(sumOfService)
                        }
                    }
                    val sumOfService = sumOfServiceList.size
                    binding.statsGeneralServiceAmountValueTextview.text = sumOfService.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY WYDATKÓW
        expenditureReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfExpenditure = document.getString("typeOfStatistic").toString()
                        if (sumOfExpenditure != "null") {
                            sumOfExpenditureList.add(sumOfExpenditure)
                        }
                    }
                    val sumOfExpenditure = sumOfExpenditureList.size
                    binding.statsGeneralExpenditureAmountValueTextview.text =
                        sumOfExpenditure.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE LICZBY NOTATEK
        notesReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {

                    for (document in task.result!!) {
                        val sumOfNotes = document.getString("typeOfStatistic").toString()
                        if (sumOfNotes != "null") {
                            sumOfNotesList.add(sumOfNotes)
                        }
                    }
                    val sumOfNotes = sumOfNotesList.size
                    binding.statsGeneralNoteAmountValueTextview.text = sumOfNotes.toString()
                }
            }
            onComplete(result)
        }

        // ZAPISANIE ILOŚĆI ZATANKOWANEGO PALIWA DO LISTY totalFuelAmountList
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalFuelAmount = document.getString("fuelAmount").toString()
                        if (totalFuelAmount != "null") {
                            totalFuelAmountList.add(totalFuelAmount.toFloat())
                        }
                    }
                    val sumOfTotalFuelAmount = totalFuelAmountList.sum()

                    saveArrayToSharedPreferences(
                        requireContext(),
                        totalFuelAmountList,
                        "totalFuelAmountList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_title_fuel_amount),
                        "chartTitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        selectedDate,
                        "chartSubtitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value_litres) + " [${unitOfVolume?.trim()}]",
                        "chartYAxisTitleTotalFuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesTotalFuelAmount"
                    )

                    if (sumOfTotalFuelAmount > 0) {
                        binding.statsRefuelingFuelAmountValueTextview.text =
                            sumOfTotalFuelAmount.toString() + " [${unitOfVolume?.trim()}]"
                    } else {
                        binding.statsRefuelingFuelAmountValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }

        // ZAPISANIE ŚREDNIEJ CENY ZA LITR DO LISTY averageFuelPriceForAmountList
        // ORAZ OBLICZENIE ŚREDNIEGO DYSTANSU POMIĘDZY TANKOWANIAMI
        refuelReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val totalCost = document.getString("totalCost").toString()
                        val fuelAmount = document.getString("fuelAmount").toString()
                        val distanceTravelled =
                            document.getString("distanceTravelledSinceRefueling").toString()
                        val averagePriceForDocument = totalCost.toFloat() / fuelAmount.toFloat()

                        if (totalCost != "null" && fuelAmount != "null" && distanceTravelled != "null") {
                            averageFuelPriceForAmountList.add(averagePriceForDocument)
                            averageDistanceBetweenRefuelingList.add(distanceTravelled.toInt())
                        }
                    }

                    // Zapisanie danych na potrzeby wykresu odnośnie średniej ceny za litr
                    saveArrayToSharedPreferences(
                        requireContext(),
                        averageFuelPriceForAmountList,
                        "averageFuelPriceForAmountList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_title_fuel_price_per_unit),
                        "chartTitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        selectedDate,
                        "chartSubtitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value) + " [${unitTotalCost?.trim()}]",
                        "chartYAxisTitleAverageFuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesAverageFuelPrice"
                    )

                    // Zapisanie danych na potrzeby wykresu odnośnie średniego dystansu pomiędzy tankowaniami
                    saveArrayOfIntToSharedPreferences(
                        requireContext(),
                        averageDistanceBetweenRefuelingList,
                        "averageDistanceBetweenRefuelingList"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_title_average_distance_between_refueling),
                        "chartTitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        selectedDate,
                        "chartSubtitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_value_avg_distance_unit) + " [${unitOfDistance?.trim()}]",
                        "chartYAxisTitleDistanceBetweenRefueling"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        getString(R.string.statistics_fragment_chart_refueling),
                        "nameOfSeriesDistanceBetweenRefueling"
                    )

                    val sumOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.sum()
                    val sizeOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.size
                    val averagePriceOfFuel =
                        sumOfAverageFuelPriceForAmountList / sizeOfAverageFuelPriceForAmountList

                    if (averagePriceOfFuel > 0) {
                        binding.statsRefuelingAveragePricePerUnitValueTextview.text =
                            formatToDisplayTwoDecimalPlaces.format(averagePriceOfFuel)
                                .replace(',', '.') + " [${unitTotalCost?.trim()}]"
                    } else {
                        binding.statsRefuelingAveragePricePerUnitValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }

                    if (averageDistanceBetweenRefuelingList.size > 0) {
                        val distanceTravelledBetweenRefueling =
                            averageDistanceBetweenRefuelingList.sum()
                                .div(averageDistanceBetweenRefuelingList.size)

                        binding.statsRefuelingAverageDistanceBetweenRefuelingValueTextview.text =
                            distanceTravelledBetweenRefueling.toString() + " [${unitOfDistance?.trim()}]"
                    } else {
                        binding.statsRefuelingAverageDistanceBetweenRefuelingValueTextview.text =
                            getString(R.string.statistic_no_data)
                    }
                }
            }
            onComplete(result)
        }
    }


    private fun makeDialogForDataStatisticsFiltering() {
        val inflateStatistic = LayoutInflater.from(context)
        val v = inflateStatistic.inflate(R.layout.statistic_data_filtering_dialog, null)
        val addDialog = AlertDialog.Builder(context)
        val pickDateImageView =
            v.findViewById<ImageView>(R.id.pick_date_statistic_data_filtering_dialog_image_view)
        val dateSelectedEditText =
            v.findViewById<EditText>(R.id.date_selected_statistic_data_filtering_dialog_edit_text)
        val statisticRadioGroup = v.findViewById<RadioGroup>(R.id.statistic_radio_group_dialog)
        val todayDateInMillis = System.currentTimeMillis()

        // Ustawienie listenera na ikonkę kalendarza
        pickDateImageView.setOnClickListener {
            // Tworzenie datepickera
            val datePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText(getString(R.string.date_picker_pick_date))
                    .setSelection(
                        Pair(
                            MaterialDatePicker.thisMonthInUtcMilliseconds(),
                            MaterialDatePicker.todayInUtcMilliseconds()
                        )
                    )
                    .build()
            datePicker.show(parentFragmentManager, getString(R.string.date_picker_pick_date))

            // W przypadku wybrania prawidłowego zakresu dat
            datePicker.addOnPositiveButtonClickListener {
                // wpisz wartość tekstową zakresu dat do pola tekstowego
                dateSelectedEditText.setText(datePicker.headerText)
                // oraz zapisz zakres dat do SharedPreferences aby przekazać to do buttona "Filtruj" w dialogu
                val datePickerSelectedRange = datePicker.selection
                val firstDateInMillis = datePickerSelectedRange?.first
                val lastDateInMillis = datePickerSelectedRange?.second
                val firstDate = simpleDateFormat.format(firstDateInMillis)
                val lastDate = simpleDateFormat.format(lastDateInMillis)
                saveDateRangeValuesToSharedPreferences(
                    requireContext(),
                    firstDateInMillis.toString(),
                    lastDateInMillis.toString(),
                    "$firstDate - $lastDate",
                    "startDateStatistics",
                    "endDateStatistics",
                    "dateSelectedStatistics"
                )
            }
        }


        statisticRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_button_last_week -> {
                    val weekInMillis = 604800000
                    val dayWeekAgo = todayDateInMillis - weekInMillis
                    val firstDate = simpleDateFormat.format(dayWeekAgo)
                    val lastDate = simpleDateFormat.format(todayDateInMillis)

                    saveDateRangeValuesToSharedPreferences(
                        requireContext(),
                        dayWeekAgo.toString(),
                        todayDateInMillis.toString(),
                        "$firstDate - $lastDate",
                        "startDateStatistics",
                        "endDateStatistics",
                        "dateSelectedStatistics"
                    )
                }

                R.id.radio_button_last_month -> {
                    val monthInMillis = 2592000000
                    val dayMonthAgo = todayDateInMillis - monthInMillis
                    val firstDate = simpleDateFormat.format(dayMonthAgo)
                    val lastDate = simpleDateFormat.format(todayDateInMillis)

                    saveDateRangeValuesToSharedPreferences(
                        requireContext(),
                        dayMonthAgo.toString(),
                        todayDateInMillis.toString(),
                        "$firstDate - $lastDate",
                        "startDateStatistics",
                        "endDateStatistics",
                        "dateSelectedStatistics"
                    )
                }

                R.id.radio_button_last_year -> {
                    val yearInMillis = 31536000000
                    val dayYearAgo = todayDateInMillis - yearInMillis
                    val firstDate = simpleDateFormat.format(dayYearAgo)
                    val lastDate = simpleDateFormat.format(todayDateInMillis)

                    saveDateRangeValuesToSharedPreferences(
                        requireContext(),
                        dayYearAgo.toString(),
                        todayDateInMillis.toString(),
                        "$firstDate - $lastDate",
                        "startDateStatistics",
                        "endDateStatistics",
                        "dateSelectedStatistics"
                    )


                }

            }

        }

        // Tworzenie dialogu
        addDialog.setView(v)
        addDialog.setPositiveButton(getString(R.string.home_dialog_positive_button_filter)) { _, _ ->
            AlertDialog.Builder(context)

            // Zmienna na potrzeby SharedPreferences
            val preferences: SharedPreferences =
                requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

            // Odczyt wartości zmiennych zapisanych w SharedPreferences
            val startDate = preferences.getString("startDateStatistics", "0").toString()
            val endDate = preferences.getString("endDateStatistics", "0").toString()
            val selectedDate = preferences.getString("dateSelectedStatistics", "0").toString()

            // Dodanie nowych danych na podstawie wybranego zakresu dat oraz typu statystyki
            binding.statisticsFilterValueTextview.text =
                getString(R.string.fragment_statistics_data_filtering_text_view, selectedDate)

            //Wywołanie funkcji wyświetlającej statystyki z podanych zakresów dat
            when (statisticRadioGroup.checkedRadioButtonId) {
                R.id.radio_button_custom_date_range -> {
                    makeStatisticsWithDataFiltering(startDate, endDate)
                }
                R.id.radio_button_last_week -> {
                    makeStatisticsWithDataFiltering(startDate, endDate)
                }
                R.id.radio_button_last_month -> {
                    makeStatisticsWithDataFiltering(startDate, endDate)
                }
                R.id.radio_button_last_year -> {
                    makeStatisticsWithDataFiltering(startDate, endDate)
                }
            }

        }

        addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button)) { dialog, _ ->
            AlertDialog.Builder(context)
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }
}