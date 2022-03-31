package com.simplycarfleet.nav_menu

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.simplycarfleet.databinding.FragmentHomeBinding
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.simplycarfleet.R
import com.simplycarfleet.data.Statistics
import java.util.*
import kotlin.collections.ArrayList
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.nav_menu.HomeAdapter.*
import java.text.SimpleDateFormat
import androidx.core.util.Pair as Pair
import com.simplycarfleet.functions.FunctionsAndValues

class HomeFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Inne zmienne
    private lateinit var statisticsList: ArrayList<Statistics>
    private lateinit var homeAdapter: HomeAdapter

    private var dateSelectedInMillis = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Recycler View
        val recyclerView: RecyclerView = binding.recyclerViewHome
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        // Adapter
        statisticsList = arrayListOf()
        homeAdapter = activity?.let { HomeAdapter(statisticsList, it) }!!
        recyclerView.adapter = homeAdapter

        homeAdapter.setOnItemClickListener(
            object : OnItemClickListener {
                override fun onItemClick(position: Int, statistics: Statistics) {
                    statisticShowMoreInfo(statistics.typeOfStatistic.toString())
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.typeOfStatistic.toString(),
                        "typeOfStatistic"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.statisticDate.toString(),
                        "statisticDate"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.carMileage.toString(),
                        "carMileage"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.gasStation.toString(),
                        "gasStation"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.fuelAmount.toString(),
                        "fuelAmount"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.fuelPrice.toString(),
                        "fuelPrice"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.totalCost.toString(),
                        "totalCost"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.carWorkshop.toString(),
                        "carWorkshop"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.serviceType.toString(),
                        "serviceType"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.expenditureType.toString(),
                        "expenditureType"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.noteDescription.toString(),
                        "noteDescription"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.refuelImageId1.toString(),
                        "refuelImageId1"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.refuelImageId2.toString(),
                        "refuelImageId2"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.refuelImageId3.toString(),
                        "refuelImageId3"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.noteImageId1.toString(),
                        "noteImageId1"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.noteImageId2.toString(),
                        "noteImageId2"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.noteImageId3.toString(),
                        "noteImageId3"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.serviceImageId1.toString(),
                        "serviceImageId1"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.serviceImageId2.toString(),
                        "serviceImageId2"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.serviceImageId3.toString(),
                        "serviceImageId3"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.expenditureImageId1.toString(),
                        "expenditureImageId1"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.expenditureImageId2.toString(),
                        "expenditureImageId2"
                    )
                    saveDataToSharedPreferences(
                        requireContext(),
                        statistics.expenditureImageId3.toString(),
                        "expenditureImageId3"
                    )
                }
            },
        )

        // Odwołanie do kolekcji "cars"
        val cloudCarCollectionReference = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")

        // Lista ID samochodów oraz lista marek
        val carBrandList: MutableList<String> = ArrayList()
        val carIdList: MutableList<String> = ArrayList()

        // Spinner do wyboru pojazdu
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, carBrandList)
        binding.spinner.adapter = arrayAdapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                // Usuwanie TextView do wyświetlania wybranego zakresu dat
                binding.filterContainer.visibility = View.GONE
                //Czyszczenie listy przed wybraniem nowej pozycji ze spinnera
                statisticsList.clear()
                //Wyświetlanie listy tankowań w zależności od wybranego pojazdu
                realTimeUpdates(carIdList[position])
                //Zapisywanie ID wybranego pojazdu w celu przekazania go do innego fragmentu dodającej tankowania/serwisy itd.
                sharedViewModel.saveCarId(carIdList[position])
                //Zapisywanie pozycji spinnera w celu przekazania go do innego fragmentu
                sharedViewModel.saveSpinnerPosition(position)
                //Zapisanie ID wybranego pojazdu
                saveDataToSharedPreferences(requireContext(), carIdList[position], "CAR ID")
                //Zapisanie pozycji spinnera do SharedPreferences
                saveDataIntToSharedPreferences(requireContext(), position, "SPINNER POSITION")
                //Wyświetlenie FAB po zmianie pojazdu aby uniknąc sytuacji, że nie da się wywołać FAB jeżeli jest mało wpisów
                binding.speedDial.show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

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
                val preferences: SharedPreferences =
                    requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
                val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)
                //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinnera
                binding.spinner.setSelection(spinnerPosition)

                // Jeżeli lista pojazdów jest pusta, ukryj kontrolki odnośnie wpisów natomiast wyświetl informację o braku pojazdów
                if (carIdList.size == 0) {
                    binding.tabsHome.visibility = View.GONE
                    binding.spinnerHomeBackground.visibility = View.GONE
                    binding.filterContainer.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.speedDial.visibility = View.GONE
                    binding.fragmentHomeNoCarsCard.visibility = View.VISIBLE
                } else {
                    binding.tabsHome.visibility = View.VISIBLE
                    binding.spinnerHomeBackground.visibility = View.VISIBLE
                    binding.filterContainer.visibility = View.VISIBLE
                    binding.linearLayout.visibility = View.VISIBLE
                    binding.speedDial.visibility = View.VISIBLE
                    binding.fragmentHomeNoCarsCard.visibility = View.GONE
                }
            }
            onComplete(result)
        }
        addCarStatistic(view)

        homeAdapter.setOnStatisticMoreOptionsClickListener(
            object : OnStatisticMoreOptionsClickListener {
                override fun onItemClick(position: Int, statistics: Statistics) {
                    val preferences: SharedPreferences =
                        requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
                    val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)

                    val newView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                    popupMenus(
                        newView!!,
                        carIdList[spinnerPosition],
                        statistics.id.toString(),
                        statistics.typeOfStatistic.toString(),
                        statistics.statisticDate.toString(),
                        statistics.carMileage.toString(),
                        statistics.gasStation.toString(),
                        statistics.fuelAmount.toString(),
                        statistics.fuelPrice.toString(),
                        statistics.totalCost.toString(),
                        statistics.carWorkshop.toString(),
                        statistics.serviceType.toString(),
                        statistics.expenditureType.toString(),
                        statistics.noteDescription.toString(),
                    )
                }
            },
        )
        // Dodanie ActionBara
        (activity as MainActivity?)?.supportActionBar?.show()

        // Zmienna na potrzeby SharedPreferences
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        // Odczyt wartości carId zapisanej w SharedPreferences
        val carId = preferences.getString("CAR ID", "").toString()

        // Po wciśnięciu przycisku wyczyść, usuń widoczność pól, wyczyśc listę i odśwież layout
        binding.buttonCancelDataFiltering.setOnClickListener {
            binding.filterContainer.visibility = View.GONE
            statisticsList.clear()
            realTimeUpdates(carId)
            Snackbar.make(
                requireView(),
                getString(R.string.home_data_filtering_clear),
                Snackbar.LENGTH_LONG
            )
                .show()
        }
        // Możliwość wywołania menu filtrowania poprzez kliknięcie na TextView -> opcja wygodniejsza
        // dla użytkownika jeżeli są już przefiltrowane dane i użytkownik chce coś zmienić
        binding.dataFilteringTextView.setOnClickListener {
            makeDialogForDataFiltering()
        }

        // Ukrywanie FloatinActionButtona podczas przewijania RecyclerView w dół
        // Jest to konieczne, ponieważ FAB zasłania np. ostatni element RV i nie można
        // edytować/usunąć elementu
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.speedDial.visibility == View.VISIBLE) {
                    binding.speedDial.hide()
                } else if (dy < 0 && binding.speedDial.visibility != View.VISIBLE) {
                    binding.speedDial.show()
                }
            }
        })

        binding.tabsHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    1 -> replaceFragment(StatisticsFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.filter_home_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_home_fragment_icon -> makeDialogForDataFiltering()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun makeDialogForDataFiltering() {
        val inflateCar = LayoutInflater.from(context)
        val v = inflateCar.inflate(R.layout.home_data_filtering_dialog, null)
        val addDialog = AlertDialog.Builder(context)
        val pickDateImageView =
            v.findViewById<ImageView>(R.id.pick_date_home_data_filtering_dialog_image_view)
        val dateSelectedEditText =
            v.findViewById<EditText>(R.id.date_selected_home_data_filtering_dialog_edit_text)
        val spinner =
            v.findViewById<Spinner>(R.id.home_data_filtering_dialog_spinner)

        // Spinner do wyboru typu serwisu
        val serviceTypeList =
            arrayOf(
                getString(R.string.home_data_filtering_spinner_all),
                getString(R.string.home_data_filtering_spinner_service),
                getString(R.string.home_data_filtering_spinner_expenditure),
                getString(R.string.home_data_filtering_spinner_note),
                getString(R.string.home_data_filtering_spinner_refueling)
            )
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, serviceTypeList)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                //saveStatisticType(requireContext(), spinner.selectedItem.toString())
                saveDataToSharedPreferences(
                    requireContext(),
                    spinner.selectedItem.toString(),
                    "CAR STATISTIC TYPE"
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

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
                saveDateRangeValuesToSharedPreferences(
                    requireContext(),
                    datePickerSelectedRange?.first.toString(),
                    datePickerSelectedRange?.second.toString(),
                    datePicker.headerText.toString(),
                    "startDateHome",
                    "endDateHome",
                    "dateSelectedHome"
                )
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
            val carId = preferences.getString("CAR ID", "").toString()
            val startDate = preferences.getString("startDateHome", "0").toString()
            val endDate = preferences.getString("endDateHome", "0").toString()
            val selectedDate = preferences.getString("dateSelectedHome", "0").toString()
            val statisticType = preferences.getString("CAR STATISTIC TYPE", "0").toString()

            // Wyczyszczenie listy przed dodaniem nowych "przefiltrowanych" danych
            statisticsList.clear()

            // Dodanie nowych danych na podstawie wybranego zakresu dat oraz typu statystyki
            when (statisticType) {
                getString(R.string.home_data_filtering_spinner_all) -> realTimeUpdatesAfterFiltering(
                    carId,
                    startDate,
                    endDate,
                    listOf("Serwis", "Wydatek", "Notatka", "Tankowanie")
                )
                getString(R.string.home_data_filtering_spinner_service) -> realTimeUpdatesAfterFiltering(
                    carId,
                    startDate,
                    endDate,
                    listOf("Serwis")
                )
                getString(R.string.home_data_filtering_spinner_expenditure) -> realTimeUpdatesAfterFiltering(
                    carId,
                    startDate,
                    endDate,
                    listOf("Wydatek")
                )
                getString(R.string.home_data_filtering_spinner_note) -> realTimeUpdatesAfterFiltering(
                    carId,
                    startDate,
                    endDate,
                    listOf("Notatka")
                )
                getString(R.string.home_data_filtering_spinner_refueling) -> realTimeUpdatesAfterFiltering(
                    carId,
                    startDate,
                    endDate,
                    listOf("Tankowanie")
                )
            }
            binding.dataFilteringTextView.text =
                getString(R.string.home_data_filtering_text_view, statisticType, selectedDate)
            binding.filterContainer.visibility = View.VISIBLE
        }
        addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button)) { dialog, _ ->
            AlertDialog.Builder(context)
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }

    private fun realTimeUpdates(carId: String) {
        val cars = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId)
            .collection("Car_statistics")
            .orderBy("statisticDateInMillis", Query.Direction.ASCENDING)

        cars.addSnapshotListener { querySnapshot, error ->
            error?.let {
                Log.w(ContentValues.TAG, "Listen failed", error)
                statisticsList.clear()

                return@addSnapshotListener
            }

            for (dc: DocumentChange in querySnapshot?.documentChanges!!) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    statisticsList.add(dc.document.toObject(Statistics::class.java))
                }

                if (dc.type == DocumentChange.Type.REMOVED) {
                    statisticsList.remove(dc.document.toObject(Statistics::class.java))
                }

                if (dc.type == DocumentChange.Type.MODIFIED) {
                    if (dc.oldIndex == dc.newIndex) {
                        statisticsList.removeAt(dc.oldIndex)
                        statisticsList.add(
                            dc.oldIndex,
                            dc.document.toObject(Statistics::class.java)
                        )
                        homeAdapter.notifyItemMoved(dc.oldIndex, dc.newIndex)
                    }
                }
            }
            homeAdapter.notifyDataSetChanged()
            // Jeżeli lista wpisów jest równa 0 pokaż stosowny komunikat
            if (statisticsList.size == 0) binding.fragmentHomeNoEntriesCard.visibility =
                View.VISIBLE
            // W przeciwnym wypadku pokaż wpisy i ukryj komunikat
            else binding.fragmentHomeNoEntriesCard.visibility = View.GONE
        }
    }

    private fun realTimeUpdatesAfterFiltering(
        carId: String,
        startDate: String,
        endDate: String,
        typeOfStatistic: List<String>,
    ) {
        val cars = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId)
            .collection("Car_statistics")
            .whereGreaterThanOrEqualTo("statisticDateInMillis", startDate)
            .whereLessThanOrEqualTo("statisticDateInMillis", endDate)
            .whereIn("typeOfStatistic", typeOfStatistic)
            .orderBy("statisticDateInMillis", Query.Direction.ASCENDING)

        // Wyświetla informacje o braku wyników
        cars.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    if (task.result?.isEmpty == true) {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.home_data_filtering_no_results),
                            Snackbar.LENGTH_LONG
                        )
                            .show()
                    } else {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.home_data_filtering_successfull),
                            Snackbar.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
            onComplete(result)
        }

        cars.addSnapshotListener { querySnapshot, error ->
            error?.let {
                Log.w(ContentValues.TAG, "Listen failed", error)
                return@addSnapshotListener
            }

            for (dc: DocumentChange in querySnapshot?.documentChanges!!) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    statisticsList.add(dc.document.toObject(Statistics::class.java))
                }

                if (dc.type == DocumentChange.Type.REMOVED) {
                    statisticsList.remove(dc.document.toObject(Statistics::class.java))
                }

                if (dc.type == DocumentChange.Type.MODIFIED) {
                    if (dc.oldIndex == dc.newIndex) {
                        statisticsList.removeAt(dc.oldIndex)
                        statisticsList.add(
                            dc.oldIndex,
                            dc.document.toObject(Statistics::class.java)
                        )
                        homeAdapter.notifyItemMoved(dc.oldIndex, dc.newIndex)
                    }
                }
            }
            homeAdapter.notifyDataSetChanged()
        }
    }

    private fun addCarStatistic(view: View) {
        // Zmienne początkowe
        val carStatisticView = view.findViewById<SpeedDialView>(R.id.speedDial)
        val addServiceOption = 0
        val addExpenditureOption = 1
        val addNoteOption = 2
        val addRefuelingOption = 3

        // Zmienne do koloru
        val serviceIconColor =
            ContextCompat.getColor(
                requireActivity(),
                R.color.serviceIcon
            )
        val expenditureIconColor =
            ContextCompat.getColor(
                requireActivity(),
                R.color.expenditureIcon
            )
        val noteIconColor =
            ContextCompat.getColor(
                requireActivity(),
                R.color.noteIcon
            )
        val refuelIconColor =
            ContextCompat.getColor(
                requireActivity(),
                R.color.refuelIcon
            )
        val textColor = ContextCompat.getColor(
            requireActivity(),
            R.color.white
        )

        // Serwis
        var drawable =
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_service_24
            )
        carStatisticView.addActionItem(
            SpeedDialActionItem.Builder(addServiceOption, drawable)
                .setLabel(getString(R.string.home_action_button_add_service))
                .setLabelColor(textColor)
                .setLabelBackgroundColor(serviceIconColor)
                .setFabBackgroundColor(serviceIconColor)
                .setFabImageTintColor(textColor)
                .create()
        )
        // Wydatek
        drawable = AppCompatResources.getDrawable(
            requireContext(),
            R.drawable.ic_baseline_monetization_on_24
        )
        carStatisticView.addActionItem(
            SpeedDialActionItem.Builder(addExpenditureOption, drawable)
                .setLabel(getString(R.string.home_action_button_add_expenditure))
                .setLabelColor(textColor)
                .setLabelBackgroundColor(expenditureIconColor)
                .setFabBackgroundColor(expenditureIconColor)
                .setFabImageTintColor(textColor)
                .create()
        )
        // Notatka
        drawable = AppCompatResources.getDrawable(
            requireContext(),
            R.drawable.ic_baseline_notes_24
        )
        carStatisticView.addActionItem(
            SpeedDialActionItem.Builder(addNoteOption, drawable)
                .setLabel(getString(R.string.home_action_button_add_note))
                .setLabelColor(textColor)
                .setLabelBackgroundColor(noteIconColor)
                .setFabBackgroundColor(noteIconColor)
                .setFabImageTintColor(textColor)
                .create()
        )
        // Tankowanie
        drawable =
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_refueling_24
            )
        carStatisticView.addActionItem(
            SpeedDialActionItem.Builder(addRefuelingOption, drawable)
                .setLabel(getString(R.string.home_action_button_add_refuel))
                .setLabelColor(textColor)
                .setLabelBackgroundColor(refuelIconColor)
                .setFabBackgroundColor(refuelIconColor)
                .setFabImageTintColor(textColor)
                .create()
        )

        // Powiązanie kliknięcia ikony z odpowiednią akcją
        carStatisticView.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                addServiceOption -> {
                    // Przenieś do fragmentu AddServiceFragment w celu dodania serwisu
                    replaceFragment(AddServiceFragment())
                    return@OnActionSelectedListener true
                }
                addExpenditureOption -> {
                    // Przenieś do fragmentu AddExpenditureFragment w celu dodania wydatku
                    replaceFragment(AddExpenditureFragment())
                    return@OnActionSelectedListener true
                }
                addNoteOption -> {
                    // Przenieś do fragmentu AddNoteFragment w celu dodania notatki
                    replaceFragment(AddNoteFragment())
                    return@OnActionSelectedListener true
                }
                addRefuelingOption -> {
                    // Przenieś do fragmentu AddRefuelFragment w celu dodania tankowania
                    replaceFragment(AddRefuelFragment())
                    return@OnActionSelectedListener true
                }
            }
            true
        })
    }

    private fun statisticShowMoreInfo(typeOfStatistic: String) {
        when (typeOfStatistic) {
            "Tankowanie" -> {
                replaceFragment(AddRefuelFragmentInfo())
            }
            "Serwis" -> {
                replaceFragment(AddServiceFragmentInfo())
            }
            "Wydatek" -> {
                replaceFragment(AddExpenditureFragmentInfo())
            }
            "Notatka" -> {
                replaceFragment(AddNoteFragmentInfo())
            }
        }
    }

    private fun deleteStatistic(carId: String, statisticId: String) {
        val car: MutableMap<String, Any> = HashMap()
        car["id"] = carId
        val statistic: MutableMap<String, Any> = HashMap()
        statistic["id"] = statisticId

        // Usunięcie statystyki o podanym ID oraz zdjęć w Storage
        cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId)
            .collection("Car_statistics")
            .document(statisticId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    getString(R.string.home_data_delete_statistic_successfull),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.home_data_delete_statistic_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun modifyStatistic(
        carId: String,
        statisticId: String,
        typeOfStatistic: String,
        statisticDate: String,
        carMileage: String,
        gasStation: String,
        fuelAmount: String,
        fuelPrice: String,
        totalCost: String,
        carWorkshop: String,
        serviceType: String,
        expenditureType: String,
        noteDescription: String,
    ) {
        val statistic: MutableMap<String, Any> = HashMap()
        statistic["typeOfStatistic"] = typeOfStatistic
        statistic["statisticDate"] = statisticDate
        statistic["carMileage"] = carMileage
        statistic["gasStation"] = gasStation
        statistic["fuelAmount"] = fuelAmount
        statistic["fuelPrice"] = fuelPrice
        statistic["totalCost"] = totalCost
        statistic["carWorkshop"] = carWorkshop
        statistic["serviceType"] = serviceType
        statistic["expenditureType"] = expenditureType
        statistic["noteDescription"] = noteDescription

        val inflateCar = LayoutInflater.from(context)

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val savedCarId = preferences.getString("CAR ID", "0")
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        when (typeOfStatistic) {
            "Tankowanie" -> {
                val v = inflateCar.inflate(R.layout.modify_refuel_layout, null)
                val addDialog = AlertDialog.Builder(context)

                // Zmienne EditText do wstawienia wartości z bazy do pól
                val modifyFuelAmount =
                    v.findViewById<EditText>(R.id.modify_refuel_fuel_amount_edittext_dialog)
                val modifyFuelPricePerUnit =
                    v.findViewById<EditText>(R.id.modify_refuel_fuel_price_per_unit_edittext_dialog)
                val modifyTotalCost =
                    v.findViewById<EditText>(R.id.modify_refuel_total_cost_edittext_dialog)
                val modifyCarMileage =
                    v.findViewById<EditText>(R.id.modify_refuel_car_mileage_edittext_dialog)
                val modifyGasStation =
                    v.findViewById<EditText>(R.id.modify_refuel_gas_station_edittext_dialog)
                val modifyRefuelDateSelected =
                    v.findViewById<EditText>(R.id.modify_refuel_date_selected_edittext_dialog)
                val modifyRefuelButtonPickDate =
                    v.findViewById<Button>(R.id.modify_refuel_button_pick_date)

                // Zmienne TextInputLayout do dodania wybranych jednostek do Hintów
                val modifyFuelAmountHint =
                    v.findViewById<TextInputLayout>(R.id.modify_refuel_fuel_amount_textfield_dialog)
                val modifyFuelPricePerUnitHint =
                    v.findViewById<TextInputLayout>(R.id.modify_refuel_fuel_price_per_unit_textfield_dialog)
                val modifyTotalCostHint =
                    v.findViewById<TextInputLayout>(R.id.modify_refuel_total_cost_textfield_dialog)
                val modifyCarMileageHint =
                    v.findViewById<TextInputLayout>(R.id.modify_refuel_car_mileage_textfield_dialog)

                // Pobranie wartości z bazy danych i wstawienie ich do podglądu
                modifyFuelAmount.setText(fuelAmount)
                modifyFuelPricePerUnit.setText(fuelPrice)
                modifyTotalCost.setText(totalCost)
                modifyCarMileage.setText(carMileage)
                modifyGasStation.setText(gasStation)
                modifyRefuelDateSelected.setText(statisticDate)

                // Ustawienie odpowiedniego Hintu
                modifyFuelAmountHint.hint =
                    getString(R.string.home_refuel_fuel_amount) + " [${unitOfVolume?.trim()}]"
                modifyFuelPricePerUnitHint.hint =
                    getString(R.string.home_refuel_fuel_price_per_unit) + " [${unitTotalCost?.trim()}]"
                modifyTotalCostHint.hint =
                    getString(R.string.home_refuel_total_cost) + " [${unitTotalCost?.trim()}]"
                modifyCarMileageHint.hint =
                    getString(R.string.home_car_mileage) + " [${unitOfDistance?.trim()}]"
                // Funkcja obsługująca wybranie daty
                pickDate(modifyRefuelButtonPickDate, modifyRefuelDateSelected, statisticDate)

                // Tworzenie dialogu
                addDialog.setView(v)
                addDialog.setPositiveButton(getString(R.string.home_dialog_positive_button_edit)) { dialog, _ ->

                    if (modifyFuelAmount.text.toString().isNotEmpty()
                        && modifyFuelPricePerUnit.text.toString().isNotEmpty()
                        && modifyTotalCost.text.toString().isNotEmpty()
                        && modifyCarMileage.text.toString().isNotEmpty()
                        && modifyGasStation.text.toString().isNotEmpty()
                        && modifyRefuelDateSelected.text.toString().isNotEmpty()
                    ) {
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(statisticId)
                            .update(
                                mapOf(
                                    "fuelAmount" to modifyFuelAmount.text.toString(),
                                    "fuelPrice" to modifyFuelPricePerUnit.text.toString(),
                                    "totalCost" to modifyTotalCost.text.toString(),
                                    "carMileage" to modifyCarMileage.text.toString(),
                                    "gasStation" to modifyGasStation.text.toString(),
                                    "statisticDate" to modifyRefuelDateSelected.text.toString(),
                                    "statisticDateInMillis" to dateSelectedInMillis
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_successfull),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                statisticsList.clear()
                                realTimeUpdates(savedCarId.toString())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_failure),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.home_modify_statistic_fill_all_fields),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    dialog.dismiss()
                }
                addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button_cancel)) { dialog, _ ->
                    AlertDialog.Builder(context)
                    dialog.dismiss()
                }
                addDialog.create()
                addDialog.show()
            }
            "Serwis" -> {
                val v = inflateCar.inflate(R.layout.modify_service_layout, null)
                val addDialog = AlertDialog.Builder(context)

                // Zmienne EditText do wstawienia wartości z bazy do pól
                val modifyServiceType =
                    v.findViewById<EditText>(R.id.modify_service_type_edittext_dialog)
                val modifyServiceTotalCost =
                    v.findViewById<EditText>(R.id.modify_service_total_cost_edittext_dialog)
                val modifyServiceCarMileage =
                    v.findViewById<EditText>(R.id.modify_service_car_mileage_edittext_dialog)
                val modifyServiceCarWorkshop =
                    v.findViewById<EditText>(R.id.modify_service_car_workshop_edittext_dialog)
                val modifyServiceDate =
                    v.findViewById<EditText>(R.id.modify_service_date_selected_edittext_dialog)
                val modifyServiceButtonPickDate =
                    v.findViewById<Button>(R.id.modify_service_button_pick_date)

                // Zmienne TextInputLayout do dodania wybranych jednostek do Hintów
                val modifyServiceTotalCostHint =
                    v.findViewById<TextInputLayout>(R.id.modify_service_total_cost_textfield_dialog)
                val modifyServiceCarMileageHint =
                    v.findViewById<TextInputLayout>(R.id.modify_service_car_mileage_textfield_dialog)


                // Pobranie wartości z bazy danych i wstawienie ich do podglądu
                modifyServiceType.setText(serviceType)
                modifyServiceTotalCost.setText(totalCost)
                modifyServiceCarMileage.setText(carMileage)
                modifyServiceCarWorkshop.setText(carWorkshop)
                modifyServiceDate.setText(statisticDate)

                // Ustawienie odpowiedniego Hintu
                modifyServiceTotalCostHint.hint =
                    getString(R.string.home_expenditure_total_cost) + " [${unitTotalCost?.trim()}]"
                modifyServiceCarMileageHint.hint =
                    getString(R.string.home_car_mileage) + " [${unitOfDistance?.trim()}]"

                // Funkcja obsługująca wybranie daty
                pickDate(modifyServiceButtonPickDate, modifyServiceDate, statisticDate)

                // Tworzenie dialogu
                addDialog.setView(v)
                addDialog.setPositiveButton(getString(R.string.home_dialog_positive_button_edit)) { dialog, _ ->

                    if (modifyServiceType.text.toString().isNotEmpty()
                        && modifyServiceTotalCost.text.toString().isNotEmpty()
                        && modifyServiceCarMileage.text.toString().isNotEmpty()
                        && modifyServiceCarWorkshop.text.toString().isNotEmpty()
                        && modifyServiceDate.text.toString().isNotEmpty()
                    ) {
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(statisticId)
                            .update(
                                mapOf(
                                    "serviceType" to modifyServiceType.text.toString(),
                                    "totalCost" to modifyServiceTotalCost.text.toString(),
                                    "carMileage" to modifyServiceCarMileage.text.toString(),
                                    "carWorkshop" to modifyServiceCarWorkshop.text.toString(),
                                    "statisticDate" to modifyServiceDate.text.toString(),
                                    "statisticDateInMillis" to dateSelectedInMillis
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_successfull),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                statisticsList.clear()
                                realTimeUpdates(savedCarId.toString())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_failure),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.home_modify_statistic_fill_all_fields),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    dialog.dismiss()
                }
                addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button_cancel)) { dialog, _ ->
                    AlertDialog.Builder(context)
                    dialog.dismiss()
                }
                addDialog.create()
                addDialog.show()
            }
            "Wydatek" -> {
                val v = inflateCar.inflate(R.layout.modify_expenditure_layout, null)
                val addDialog = AlertDialog.Builder(context)
                // Zmienne EditText do wstawienia wartości z bazy do pól
                val modifyExpenditureType =
                    v.findViewById<EditText>(R.id.modify_expenditure_type_edittext_dialog)
                val modifyExpenditureTotalCost =
                    v.findViewById<EditText>(R.id.modify_expenditure_total_cost_edittext_dialog)
                val modifyExpenditureCarMileage =
                    v.findViewById<EditText>(R.id.modify_expenditure_car_mileage_edittext_dialog)
                val modifyExpenditureDate =
                    v.findViewById<EditText>(R.id.modify_expenditure_date_selected_edittext_dialog)
                val modifyExpenditureButtonPickDate =
                    v.findViewById<Button>(R.id.modify_expenditure_button_pick_date)

                // Zmienne TextInputLayout do dodania wybranych jednostek do Hintów
                val modifyExpenditureTotalCostHint =
                    v.findViewById<TextInputLayout>(R.id.modify_expenditure_total_cost_textfield_dialog)
                val modifyExpenditureCarMileageHint =
                    v.findViewById<TextInputLayout>(R.id.modify_expenditure_car_mileage_textfield_dialog)

                // Pobranie wartości z bazy danych i wstawienie ich do podglądu
                modifyExpenditureType.setText(expenditureType)
                modifyExpenditureTotalCost.setText(totalCost)
                modifyExpenditureCarMileage.setText(carMileage)
                modifyExpenditureDate.setText(statisticDate)

                // Ustawienie odpowiedniego Hintu
                modifyExpenditureTotalCostHint.hint =
                    getString(R.string.home_expenditure_total_cost) + " [${unitTotalCost?.trim()}]"
                modifyExpenditureCarMileageHint.hint =
                    getString(R.string.home_car_mileage) + " [${unitOfDistance?.trim()}]"

                // Funkcja obsługująca wybranie daty
                pickDate(modifyExpenditureButtonPickDate, modifyExpenditureDate, statisticDate)

                // Tworzenie dialogu
                addDialog.setView(v)
                addDialog.setPositiveButton(getString(R.string.home_dialog_positive_button_edit)) { dialog, _ ->

                    if (modifyExpenditureType.text.toString().isNotEmpty()
                        && modifyExpenditureTotalCost.text.toString().isNotEmpty()
                        && modifyExpenditureCarMileage.text.toString().isNotEmpty()
                        && modifyExpenditureDate.text.toString().isNotEmpty()
                    ) {
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(statisticId)
                            .update(
                                mapOf(
                                    "expenditureType" to modifyExpenditureType.text.toString(),
                                    "totalCost" to modifyExpenditureTotalCost.text.toString(),
                                    "carMileage" to modifyExpenditureCarMileage.text.toString(),
                                    "statisticDate" to modifyExpenditureDate.text.toString(),
                                    "statisticDateInMillis" to dateSelectedInMillis
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_successfull),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                statisticsList.clear()
                                realTimeUpdates(savedCarId.toString())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_failure),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.home_modify_statistic_fill_all_fields),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    dialog.dismiss()
                }
                addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button_cancel)) { dialog, _ ->
                    AlertDialog.Builder(context)
                    dialog.dismiss()
                }
                addDialog.create()
                addDialog.show()

            }
            "Notatka" -> {
                val v = inflateCar.inflate(R.layout.modify_note_layout, null)
                val addDialog = AlertDialog.Builder(context)
                // Zmienne EditText do wstawienia wartości z bazy do pól
                val modifyNoteDescription =
                    v.findViewById<EditText>(R.id.modify_note_description_edittext_dialog)
                val modifyNoteCarMileage =
                    v.findViewById<EditText>(R.id.modify_note_car_mileage_edittext_dialog)
                val modifyNoteDateSelected =
                    v.findViewById<EditText>(R.id.modify_note_date_selected_edittext_dialog)
                val modifyNoteButtonPickDate =
                    v.findViewById<Button>(R.id.modify_note_button_pick_date)
                // Zmienne TextInputLayout do dodania wybranych jednostek do Hintów
                val modifyNoteCarMileageHint =
                    v.findViewById<TextInputLayout>(R.id.modify_note_car_mileage_textfield_dialog)

                // Pobranie wartości z bazy danych i wstawienie ich do podglądu
                modifyNoteDescription.setText(noteDescription)
                modifyNoteCarMileage.setText(carMileage)
                modifyNoteDateSelected.setText(statisticDate)
                // Ustawienie odpowiedniego Hintu
                modifyNoteCarMileageHint.hint =
                    getString(R.string.home_car_mileage) + " [${unitOfDistance?.trim()}]"

                // Funkcja obsługująca wybranie daty
                pickDate(modifyNoteButtonPickDate, modifyNoteDateSelected, statisticDate)

                // Tworzenie dialogu
                addDialog.setView(v)
                addDialog.setPositiveButton(getString(R.string.home_dialog_positive_button_edit)) { dialog, _ ->

                    if (modifyNoteDescription.text.toString().isNotEmpty()
                        && modifyNoteCarMileage.text.toString().isNotEmpty()
                        && modifyNoteDateSelected.text.toString().isNotEmpty()
                    ) {
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(statisticId)
                            .update(
                                mapOf(
                                    "noteDescription" to modifyNoteDescription.text.toString(),
                                    "carMileage" to modifyNoteCarMileage.text.toString(),
                                    "statisticDate" to modifyNoteDateSelected.text.toString(),
                                    "statisticDateInMillis" to dateSelectedInMillis
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_successfull),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                statisticsList.clear()
                                realTimeUpdates(savedCarId.toString())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    getString(R.string.home_modify_statistic_failure),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.home_modify_statistic_fill_all_fields),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    dialog.dismiss()
                }
                addDialog.setNegativeButton(getString(R.string.home_dialog_negative_button_cancel)) { dialog, _ ->
                    AlertDialog.Builder(context)
                    dialog.dismiss()
                }
                addDialog.create()
                addDialog.show()
            }
        }
    }

    private fun popupMenus(
        v: View,
        carId: String,
        statisticId: String,
        typeOfStatistic: String,
        statisticDate: String,
        carMileage: String,
        gasStation: String,
        fuelAmount: String,
        fuelPrice: String,
        totalCost: String,
        carWorkshop: String,
        serviceType: String,
        expenditureType: String,
        noteDescription: String,
    ) {
        val popupMenus = PopupMenu(context, v, Gravity.END, R.attr.actionOverflowMenuStyle, 0)
        popupMenus.inflate(R.menu.menu_statistics)
        popupMenus.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.modify_statistic -> {
                    modifyStatistic(
                        carId,
                        statisticId,
                        typeOfStatistic,
                        statisticDate,
                        carMileage,
                        gasStation,
                        fuelAmount,
                        fuelPrice,
                        totalCost,
                        carWorkshop,
                        serviceType,
                        expenditureType,
                        noteDescription
                    )
                    true
                }
                R.id.del_statistic -> {
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.home_delete_statistic_warning_title))
                        .setIcon(R.drawable.ic_baseline_warning_24)
                        .setMessage(getString(R.string.home_delete_statistic_warning_message))
                        .setPositiveButton(getString(R.string.home_delete_statistic_warning_positive_button)) { dialog, _ ->
                            deleteStatistic(carId, statisticId)
                            Toast.makeText(
                                context,
                                getString(R.string.home_delete_statistic_warning_success),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.home_delete_statistic_warning_negative_button)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    true
                }

                else -> true
            }
        }
        popupMenus.show()
    }

    private fun pickDate(button: Button, editText: EditText, savedDate: String) {
        // DATEPICKER
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_pick_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        // W przypadku kliknięcia ikony kalendarza otwórz okno wybierania daty
        button.setOnClickListener {
            datePicker.show(parentFragmentManager, getString(R.string.date_picker_pick_date))
        }
        // Ustawienie daty w milisekundach aby w przypadku braku wybrania daty wstawic poprzednia wartosc
        // zeby nie wstawic nulla do bazy
        val simpleDateFormatDefault = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val mDateDefault: Date? = simpleDateFormatDefault.parse(savedDate)
        dateSelectedInMillis = mDateDefault?.time.toString()
        // W przypadku prawidłowego wybrania daty wpisz w pole datę o podanym formacie
        // Oraz zamień datę na milisekundy
        datePicker.addOnPositiveButtonClickListener {
            editText.setText(datePicker.headerText)
            val simpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val mDate: Date? = simpleDateFormat.parse(datePicker.headerText)
            //Konwersja daty na milisekundy
            dateSelectedInMillis = mDate?.time.toString()
        }
    }
}