package com.simplycarfleet.nav_menu

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.util.Pair
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.databinding.FragmentReportsBinding
import com.simplycarfleet.functions.FunctionsAndValues
import java.io.IOException
import java.io.OutputStream
import kotlin.collections.ArrayList

class ReportsFragment : FunctionsAndValues() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(WRITE_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED
        )
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(READ_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED
        )

        // Listy ID samochodów, marek
        val carBrandList: MutableList<String> = ArrayList()
        val carIdList: MutableList<String> = ArrayList()

        // Listener na przycisk odnośnie filtrowania
        binding.reportsFilterValueTextview.setOnClickListener {
            makeDialogForCreateFilteringPdf()
        }

        binding.reportsFilterButtonTextview.setOnClickListener {
            makeDialogForCreateFilteringPdf()
        }

        // Spinner do wyboru pojazdu
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, carBrandList)
        binding.spinnerReports.adapter = arrayAdapter
        binding.spinnerReports.onItemSelectedListener =
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
                    // Wywołanie funkcji tworzącej pdf ze statystykami
                    // Pobranie z bazy daty pierwszego i ostatniego wpisu odnośnie wybranego pojazdu
                    getFirstAndLastDateFromFirebase()
                    saveDataStatistic()
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
                binding.spinnerReports.setSelection(spinnerPosition)

                // Jeżeli lista pojazdów jest pusta, ukryj kontrolki odnośnie przypomnień natomiast wyświetl informację o braku pojazdów
                if (carIdList.size == 0) {
                    binding.spinnerReportsBackground.visibility = View.GONE
                    binding.fragmentReportsInfoCard.visibility = View.GONE
                    binding.buttonCreatePdf.visibility = View.GONE

                    binding.fragmentReportsNoCarsCard.visibility = View.VISIBLE
                } else {
                    binding.spinnerReportsBackground.visibility = View.VISIBLE
                    binding.fragmentReportsInfoCard.visibility = View.VISIBLE
                    binding.buttonCreatePdf.visibility = View.VISIBLE

                    binding.fragmentReportsNoCarsCard.visibility = View.GONE
                }
            }
            onComplete(result)
        }
        saveDataStatistic()

        binding.buttonCreatePdf.setOnClickListener {
            createFile()
        }
    }

    // Utwórz plik PDF
    private fun createFile() {
        // Kiedy tworzy się dokument, trzeba wybrać akcję ACTION_CREATE_DOCUMENT
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        // Wyświetlenie tylko folderów, które mogą zostać otwarte
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // Utwórz plik typu PDF
        intent.type = "application/pdf"

        // Domyślna nazwa pliku
        intent.putExtra(Intent.EXTRA_TITLE, "Raport.pdf")

        // Uruchomienie intentu
        startActivityForPdfResult.launch(intent)

    }

    private val startActivityForPdfResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        val outputStream: OutputStream
                        try {
                            outputStream = context?.contentResolver?.openOutputStream(data.data!!)!!
                            createPdf(outputStream)

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }


    private fun createPdf(outputStream: OutputStream) {
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        // Odczyt wartości carId zapisanej w SharedPreferences
        val carId = preferences.getString("CAR ID", "").toString()
        val currentCarReference = cloudCarCollectionReference.whereEqualTo("id", carId).limit(1)
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        currentCarReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val carBrand = document.getString("brand")
                        val carModel = document.getString("model")
                        val carYearOfProduction = document.getString("yearOfProduction")
                        var firstDateInMillis = preferences.getString("firstDateInMillis", "")
                        var lastDateInMillis = System.currentTimeMillis()
                        val startDate = preferences.getString("startDateStatistics", "0").toString()
                        val endDate = preferences.getString("endDateStatistics", "0").toString()

                        //Jeśli zakresy zostały przekazane to następuje nadpis domyślnych zakresów
                        if (startDate.toLong() > 0 && endDate.toLong() > 0) {
                            firstDateInMillis = startDate
                            lastDateInMillis = endDate.toLong()
                        }

                        val firstDate = simpleDateFormat.format(firstDateInMillis?.toLong())
                        val lastDate = simpleDateFormat.format(lastDateInMillis)

                        val lastCarMileage = preferences.getInt("lastCarMileage", 0)
                        val costPerDistance = preferences.getString("costPerDistance", "")
                        val sumOfTotalFuelCost = preferences.getString("sumOfTotalFuelCost", "")
                        val totalCarDistanceBetweenCarMileages =
                            preferences.getString("totalCarDistanceBetweenCarMileages", "")
                        val sumOfRefueling = preferences.getString("sumOfRefueling", "")
                        val sumOfService = preferences.getString("sumOfService", "")
                        val sumOfNotes = preferences.getString("sumOfNotes", "")
                        val sumOfExpenditure = preferences.getString("sumOfExpenditure", "")
                        val sumOfTotalFuelAmount = preferences.getString("sumOfTotalFuelAmount", "")
                        val averagePriceOfFuel = preferences.getString("averagePriceOfFuel", "")
                        val sumOfTotalServiceCost =
                            preferences.getString("sumOfTotalServiceCost", "")
                        val dailyServiceCost = preferences.getString("dailyServiceCost", "")
                        val averageServiceCost = preferences.getString("averageServiceCost", "")
                        val sumOfTotalExpenditureCost =
                            preferences.getString("sumOfTotalExpenditureCost", "")
                        val dailyExpenditureCost = preferences.getString("dailyExpenditureCost", "")
                        val averageExpenditureCost =
                            preferences.getString("averageExpenditureCost", "")

                        // Utworzenie obiektu PdfDocument
                        val pdfDocument = PdfDocument()

                        // Paint jest używany do rysowania kształtów, Title do dodawania tekstu
                        val paint = Paint()
                        val title = Paint()
                        val subTitle = Paint()
                        val normalText = Paint()
                        val normalTextTitle = Paint()

                        // Utworzenie dokumentu z podanymi parametrami
                        val mypageInfo = PdfDocument.PageInfo.Builder(400, 900, 1)
                            .create()

                        // Ustawienie startowej strony
                        val myPage = pdfDocument.startPage(mypageInfo)

                        // Utworzenie zmiennej Canvas dla dokumentu
                        val canvas: Canvas = myPage.canvas

                        // Zmienna na potrzeby wyśrodkowania tekstu
                        val xPosCenter = canvas.width / 2

                        // Wyśrodkowanie tekstu i ustalenie wielkości tekstu
                        title.textAlign = Paint.Align.CENTER
                        title.textSize = 16F
                        // Pogrubienie tekstu dla tytułu dokumentu
                        title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                        // Wyśrodkowanie tekstu i ustalenie wielkości tekstu
                        subTitle.textAlign = Paint.Align.CENTER
                        subTitle.textSize = 12F

                        normalText.textSize = 12F

                        normalTextTitle.textSize = 14F
                        normalTextTitle.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                        // TYTUŁ
                        canvas.drawText(
                            getString(R.string.reports_fragment_report_entire_date_range),
                            xPosCenter.toFloat(),
                            20F,
                            title
                        )

                        // TYTUŁ
                        canvas.drawText(
                            "($firstDate - $lastDate)",
                            xPosCenter.toFloat(),
                            40F,
                            title
                        )

                        // PODTYTUŁ
                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_subtitle_pdf,
                                carBrand,
                                carModel,
                                carYearOfProduction
                            ),
                            xPosCenter.toFloat(),
                            60F,
                            subTitle
                        )

                        canvas.drawLine(20F, 65F, 380F, 66F, paint)

                        // STATYSTYKI OGÓLNE
                        canvas.drawText(
                            getString(R.string.reports_fragment_general_statistics),
                            20F,
                            90F,
                            normalTextTitle
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_total_mileage,
                                lastCarMileage.toString()
                            ) + " [${unitOfDistance?.trim()}]",
                            20F,
                            110F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_distance_travelled,
                                totalCarDistanceBetweenCarMileages
                            ) + " [${unitOfDistance?.trim()}]",
                            20F,
                            130F,
                            normalText
                        )

                        canvas.drawText(
                            getString(R.string.reports_fragment_refueling_count, sumOfRefueling),
                            20F,
                            150F,
                            normalText
                        )

                        canvas.drawText(
                            getString(R.string.reports_fragment_services_count, sumOfService),
                            20F,
                            170F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_expenditures_count,
                                sumOfExpenditure
                            ),
                            20F,
                            190F,
                            normalText
                        )

                        canvas.drawText(
                            getString(R.string.reports_fragment_notes_count, sumOfNotes),
                            20F,
                            210F,
                            normalText
                        )

                        canvas.drawLine(20F, 215F, 380F, 216F, paint)

                        // STATYSTYKI TANKOWAŃ
                        canvas.drawText(
                            getString(R.string.reports_fragment_refueling_statistic),
                            20F,
                            240F,
                            normalTextTitle
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_sum_of_total_fuel_amount,
                                sumOfTotalFuelAmount
                            ) + " [${unitOfVolume?.trim()}]",
                            20F,
                            260F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_sum_of_total_fuel_cost,
                                sumOfTotalFuelCost
                            ) + " [${unitTotalCost?.trim()}]",
                            20F,
                            280F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_average_price_of_fuel,
                                averagePriceOfFuel
                            ) + " [${unitTotalCost?.trim()}]",

                            20F,
                            300F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_cost_per_distance,
                                costPerDistance
                            ) + " [${unitTotalCost?.trim()}]",

                            20F,
                            320F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_distance_travelled_between_refuling,
                                costPerDistance
                            ) + " [${unitOfDistance?.trim()}]",

                            20F,
                            340F,
                            normalText
                        )

                        canvas.drawLine(20F, 345F, 380F, 346F, paint)

                        // STATYSTYKI SERWISÓW
                        canvas.drawText(
                            getString(R.string.reports_fragment_services_statistics),
                            20F,
                            370F,
                            normalTextTitle
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_sum_of_total_service_cost,
                                sumOfTotalServiceCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            390F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_average_service_cost,
                                averageServiceCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            410F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_daily_service_cost,
                                dailyServiceCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            430F,
                            normalText
                        )

                        canvas.drawLine(20F, 435F, 380F, 436F, paint)

                        // STATYSTYKI WYDATKÓW

                        canvas.drawText(
                            getString(R.string.reports_fragment_expenditures_statistics),
                            20F,
                            470F,
                            normalTextTitle
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_sum_of_total_expenditure_cost,
                                sumOfTotalExpenditureCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            490F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_average_expenditure_cost,
                                averageExpenditureCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            510F,
                            normalText
                        )

                        canvas.drawText(
                            getString(
                                R.string.reports_fragment_daily_expenditure_cost,
                                dailyExpenditureCost
                            )
                                    + " [${unitTotalCost?.trim()}]",
                            20F,
                            530F,
                            normalText
                        )

                        // Po dodaniu całej zawartości dokumentu dokument jest finiszowany
                        pdfDocument.finishPage(myPage)

                        //Zapis pliku do podanej ścieżki
                        pdfDocument.writeTo(outputStream)

                        //Zamknięcie dokumentu
                        pdfDocument.close()

                        Toast.makeText(
                            activity,
                            getString(R.string.reports_fragment_create_raport_info),
                            Toast.LENGTH_SHORT
                        )
                            .show()

                    }
                }
            }
            onComplete(result)
        }
    }

    private fun saveDataStatistic() {
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        firstDateInMillis,
                        "firstDateInMillis"
                    )
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

                                        val sumOfTotalServiceCost = totalServiceCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT SERWISÓW > 0
                                        if (sumOfTotalServiceCost > 0) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalServiceCost
                                                ).replace(',', '.'),
                                                "sumOfTotalServiceCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "sumOfTotalServiceCost"
                                            )
                                        }

                                        // DZIENNY KOSZT SERWISU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong()).div(
                                                        dayInMillis.toLong()
                                                    )
                                            val dailyServiceCost =
                                                sumOfTotalServiceCost.toInt().div(
                                                    dailyServiceCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                            if (dailyServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    ).replace(',', '.'),
                                                    "dailyServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyServiceCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    ).replace(',', '.'),
                                                    "averageServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageServiceCost"
                                                )
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
                                            if (dailyServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    ).replace(',', '.'),
                                                    "dailyServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyServiceCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    ).replace(',', '.'),
                                                    "averageServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageServiceCost"
                                                )
                                            }

                                        } else if (sumOfTotalServiceCost < 1 || sumOfTotalServiceCost.isNaN()) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageServiceCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyServiceCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageServiceCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyServiceCost"
                                            )
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

                                        val sumOfTotalExpenditureCost =
                                            totalExpenditureCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT WYDATKÓW > 0
                                        if (sumOfTotalExpenditureCost > 0) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalExpenditureCost
                                                ).replace(',', '.'),
                                                "sumOfTotalExpenditureCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "sumOfTotalExpenditureCost"
                                            )
                                        }

                                        // DZIENNY KOSZT WYDATKU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
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
                                            if (dailyExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    ).replace(',', '.'),
                                                    "dailyExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyExpenditureCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    ).replace(',', '.'),
                                                    "averageExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageExpenditureCost"
                                                )
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
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    ).replace(',', '.'),
                                                    "dailyExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyExpenditureCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    ).replace(',', '.'),
                                                    "averageExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageExpenditureCost"
                                                )
                                            }

                                        } else if (sumOfTotalExpenditureCost < 1 || sumOfTotalExpenditureCost.isNaN()) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageExpenditureCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyExpenditureCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageExpenditureCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyExpenditureCost"
                                            )
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
                                            if (costPerDistance > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        costPerDistance
                                                    ).replace(',', '.'),
                                                    "costPerDistance"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "costPerDistance"
                                                )
                                            }
                                            // SPRAWDZENIE CZY CAŁKOWITY KOSZT TANKOWAŃ > 0
                                            if (sumOfTotalFuelCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        sumOfTotalFuelCost
                                                    ).replace(',', '.'),
                                                    "sumOfTotalFuelCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "sumOfTotalFuelCost"
                                                )
                                            }
                                            // SPRAWDZENIE CZY PRZEJECHANY DYSTANS > 0
                                            when {
                                                totalCarDistanceBetweenCarMileages > 0 -> {
                                                    saveDataToSharedPreferences(
                                                        requireContext(),
                                                        totalCarDistanceBetweenCarMileages.toString(),
                                                        "totalCarDistanceBetweenCarMileages"
                                                    )
                                                }
                                                totalCarDistanceBetweenCarMileages == 0 -> {
                                                    saveDataToSharedPreferences(
                                                        requireContext(),
                                                        totalCarDistanceBetweenCarMileages.toString(),
                                                        "totalCarDistanceBetweenCarMileages"
                                                    )
                                                }
                                                else -> {
                                                    saveDataToSharedPreferences(
                                                        requireContext(),
                                                        getString(R.string.reports_fragment_no_data),
                                                        "totalCarDistanceBetweenCarMileages"
                                                    )
                                                }
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
                        saveDataToSharedPreferences(
                            requireContext(),
                            lastCarMileageText.toString(),
                            "lastCarMileageText"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "lastCarMileageText"
                        )
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

                    val sumOfTotalCost = totalCostList.sum()
                    if (sumOfTotalCost > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalCost)
                                .replace(',', '.'),
                            "sumOfTotalCost"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "sumOfTotalCost"
                        )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfRefueling.toString(),
                        "sumOfRefueling"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfService.toString(),
                        "sumOfService"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfExpenditure.toString(),
                        "sumOfExpenditure"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfNotes.toString(),
                        "sumOfNotes"
                    )
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

                    if (sumOfTotalFuelAmount > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalFuelAmount)
                                .replace(',', '.'),
                            "sumOfTotalFuelAmount"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "sumOfTotalFuelAmount"
                        )
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

                    val sumOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.sum()
                    val sizeOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.size
                    val averagePriceOfFuel =
                        sumOfAverageFuelPriceForAmountList / sizeOfAverageFuelPriceForAmountList

                    if (averagePriceOfFuel > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(averagePriceOfFuel)
                                .replace(',', '.'),
                            "averagePriceOfFuel"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "averagePriceOfFuel"
                        )
                    }

                    if (averageDistanceBetweenRefuelingList.size > 0) {
                        val distanceTravelledBetweenRefueling =
                            averageDistanceBetweenRefuelingList.sum()
                                .div(averageDistanceBetweenRefuelingList.size)

                        saveDataToSharedPreferences(
                            requireContext(),
                            distanceTravelledBetweenRefueling.toString(),
                            "distanceTravelledBetweenRefueling"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "distanceTravelledBetweenRefueling"
                        )
                    }
                }
            }
            onComplete(result)
        }
    }

    private fun saveDataStatisticWithDataRange(firstDate: String, lastDate: String) {
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

        var firstDateInMillis = ""
        var lastDateInMillis = ""

        // Odwołanie do kolekcji Car_statistics wybranego pojazdu
        val cloudStatisticsCollectionReference = cloudCarCollectionReference
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        firstDateInMillis,
                        "firstDateInMillis"
                    )
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

                                        val sumOfTotalServiceCost = totalServiceCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT SERWISÓW > 0
                                        if (sumOfTotalServiceCost > 0) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalServiceCost
                                                ).replace(',', '.'),
                                                "sumOfTotalServiceCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "sumOfTotalServiceCost"
                                            )
                                        }

                                        // DZIENNY KOSZT SERWISU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
                                        if (firstDateInMillis != "" && lastDateInMillis != "" && firstDateInMillis != lastDateInMillis) {
                                            val dailyServiceCostDayDifference =
                                                lastDateInMillis.toLong()
                                                    .minus(firstDateInMillis.toLong()).div(
                                                        dayInMillis.toLong()
                                                    )
                                            val dailyServiceCost =
                                                sumOfTotalServiceCost.toInt().div(
                                                    dailyServiceCostDayDifference
                                                )
                                            // SPRAWDZENIE CZY DZIENNY KOSZT SERWISU > 0
                                            if (dailyServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    ).replace(',', '.'),
                                                    "dailyServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyServiceCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    ).replace(',', '.'),
                                                    "averageServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageServiceCost"
                                                )
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
                                            if (dailyServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyServiceCost
                                                    ).replace(',', '.'),
                                                    "dailyServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyServiceCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT SERWISU
                                            val averageServiceCost =
                                                sumOfTotalServiceCost.div(totalServiceCostList.size)

                                            // SPRAWDZENIE CZY ŚREDNI KOSZT SERWISU > 0
                                            if (averageServiceCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageServiceCost
                                                    ).replace(',', '.'),
                                                    "averageServiceCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageServiceCost"
                                                )
                                            }

                                        } else if (sumOfTotalServiceCost < 1 || sumOfTotalServiceCost.isNaN()) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageServiceCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyServiceCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageServiceCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyServiceCost"
                                            )
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

                                        val sumOfTotalExpenditureCost =
                                            totalExpenditureCostList.sum()

                                        // SPRAWDZENIE CZY CAŁKOWITY KOSZT WYDATKÓW > 0
                                        if (sumOfTotalExpenditureCost > 0) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                formatToDisplayTwoDecimalPlaces.format(
                                                    sumOfTotalExpenditureCost
                                                ).replace(',', '.'),
                                                "sumOfTotalExpenditureCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "sumOfTotalExpenditureCost"
                                            )
                                        }

                                        // DZIENNY KOSZT WYDATKU
                                        // Wartosc jednego dnia w milisekundach
                                        val dayInMillis = 86400000
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
                                            if (dailyExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    ).replace(',', '.'),
                                                    "dailyExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyExpenditureCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    ).replace(',', '.'),
                                                    "averageExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageExpenditureCost"
                                                )
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
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        dailyExpenditureCost
                                                    ).replace(',', '.'),
                                                    "dailyExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "dailyExpenditureCost"
                                                )
                                            }

                                            // ŚREDNI KOSZT WYDATKU
                                            val averageExpenditureCost =
                                                sumOfTotalExpenditureCost.div(
                                                    totalExpenditureCostList.size
                                                )
                                            // SPRAWDZENIE CZY ŚREDNI KOSZT WYDATKU > 0
                                            if (averageExpenditureCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        averageExpenditureCost
                                                    ).replace(',', '.'),
                                                    "averageExpenditureCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "averageExpenditureCost"
                                                )
                                            }

                                        } else if (sumOfTotalExpenditureCost < 1 || sumOfTotalExpenditureCost.isNaN()) {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageExpenditureCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyExpenditureCost"
                                            )
                                        } else {
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "averageExpenditureCost"
                                            )
                                            saveDataToSharedPreferences(
                                                requireContext(),
                                                getString(R.string.reports_fragment_no_data),
                                                "dailyExpenditureCost"
                                            )
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
                                            if (costPerDistance > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        costPerDistance
                                                    ).replace(',', '.'),
                                                    "costPerDistance"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "costPerDistance"
                                                )
                                            }
                                            // SPRAWDZENIE CZY CAŁKOWITY KOSZT TANKOWAŃ > 0
                                            if (sumOfTotalFuelCost > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    formatToDisplayTwoDecimalPlaces.format(
                                                        sumOfTotalFuelCost
                                                    ).replace(',', '.'),
                                                    "sumOfTotalFuelCost"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "sumOfTotalFuelCost"
                                                )
                                            }
                                            // SPRAWDZENIE CZY PRZEJECHANY DYSTANS > 0
                                            if (totalCarDistanceBetweenCarMileages > 0) {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    totalCarDistanceBetweenCarMileages.toString(),
                                                    "totalCarDistanceBetweenCarMileages"
                                                )
                                            } else {
                                                saveDataToSharedPreferences(
                                                    requireContext(),
                                                    getString(R.string.reports_fragment_no_data),
                                                    "totalCarDistanceBetweenCarMileages"
                                                )
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
                        saveDataToSharedPreferences(
                            requireContext(),
                            lastCarMileageText.toString(),
                            "lastCarMileageText"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "lastCarMileageText"
                        )
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

                    val sumOfTotalCost = totalCostList.sum()
                    if (sumOfTotalCost > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalCost)
                                .replace(',', '.'),
                            "sumOfTotalCost"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "sumOfTotalCost"
                        )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfRefueling.toString(),
                        "sumOfRefueling"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfService.toString(),
                        "sumOfService"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfExpenditure.toString(),
                        "sumOfExpenditure"
                    )
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
                    saveDataToSharedPreferences(
                        requireContext(),
                        sumOfNotes.toString(),
                        "sumOfNotes"
                    )
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

                    if (sumOfTotalFuelAmount > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(sumOfTotalFuelAmount)
                                .replace(',', '.'),
                            "sumOfTotalFuelAmount"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "sumOfTotalFuelAmount"
                        )
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

                    val sumOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.sum()
                    val sizeOfAverageFuelPriceForAmountList = averageFuelPriceForAmountList.size
                    val averagePriceOfFuel =
                        sumOfAverageFuelPriceForAmountList / sizeOfAverageFuelPriceForAmountList

                    if (averagePriceOfFuel > 0) {
                        saveDataToSharedPreferences(
                            requireContext(),
                            formatToDisplayTwoDecimalPlaces.format(averagePriceOfFuel)
                                .replace(',', '.'),
                            "averagePriceOfFuel"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "averagePriceOfFuel"
                        )
                    }

                    if (averageDistanceBetweenRefuelingList.size > 0) {
                        val distanceTravelledBetweenRefueling =
                            averageDistanceBetweenRefuelingList.sum()
                                .div(averageDistanceBetweenRefuelingList.size)

                        saveDataToSharedPreferences(
                            requireContext(),
                            distanceTravelledBetweenRefueling.toString(),
                            "distanceTravelledBetweenRefueling"
                        )
                    } else {
                        saveDataToSharedPreferences(
                            requireContext(),
                            getString(R.string.reports_fragment_no_data),
                            "distanceTravelledBetweenRefueling"
                        )
                    }
                }
            }
            onComplete(result)
        }
    }

    private fun makeDialogForCreateFilteringPdf() {
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
        addDialog.setPositiveButton(getString(R.string.reports_fragment_dialog_filter)) { _, _ ->
            AlertDialog.Builder(context)

            // Zmienna na potrzeby SharedPreferences
            val preferences: SharedPreferences =
                requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

            // Odczyt wartości zmiennych zapisanych w SharedPreferences
            val startDate = preferences.getString("startDateStatistics", "0").toString()
            val endDate = preferences.getString("endDateStatistics", "0").toString()
            val selectedDate = preferences.getString("dateSelectedStatistics", "0").toString()

            // Dodanie nowych danych na podstawie wybranego zakresu dat oraz typu statystyki
            binding.reportsFilterValueTextview.text =
                getString(R.string.fragment_statistics_data_filtering_text_view, selectedDate)


            // Wywołanie funkcji tworzącej pdf z statystykami z podanego zakresu dat
            when (statisticRadioGroup.checkedRadioButtonId) {
                R.id.radio_button_custom_date_range -> {
                    saveDataStatisticWithDataRange(startDate, endDate)
                }
                R.id.radio_button_last_week -> {
                    saveDataStatisticWithDataRange(startDate, endDate)
                }
                R.id.radio_button_last_month -> {
                    saveDataStatisticWithDataRange(startDate, endDate)
                }
                R.id.radio_button_last_year -> {
                    saveDataStatisticWithDataRange(startDate, endDate)
                }
            }
        }

        addDialog.setNegativeButton(getString(R.string.reports_fragment_dialog_cancel)) { dialog, _ ->
            AlertDialog.Builder(context)
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }
}