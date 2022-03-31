package com.simplycarfleet.nav_menu

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.data.Reminders
import com.simplycarfleet.databinding.FragmentRemindersBinding
import com.simplycarfleet.functions.FunctionsAndValues
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RemindersFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!


    // Inne zmienne
    private lateinit var remindersList: ArrayList<Reminders>
    private lateinit var remindersAdapter: RemindersAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Recycler View
        val recyclerView: RecyclerView = binding.recyclerViewReminders
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        // Adapter
        remindersList = arrayListOf()
        remindersAdapter = RemindersAdapter(requireContext(), remindersList)
        recyclerView.adapter = remindersAdapter


        remindersAdapter.setOnItemClickListener(
            object : RemindersAdapter.OnItemClickListener {
                override fun onItemClick(position: Int, reminders: Reminders) {
                    //remindersShowMoreInfo()
                }
            },
        )

        remindersAdapter.setOnReminderMoreOptionsClickListener(
            object : RemindersAdapter.OnRemindersMoreOptionsClickListener {
                override fun onItemClick(position: Int, reminders: Reminders) {
                    val preferences: SharedPreferences =
                        requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
                    val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)

                    val newView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                    //popupMenus()

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
        binding.spinnerReminders.adapter = arrayAdapter
        binding.spinnerReminders.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    //Czyszczenie listy przed wybraniem nowej pozycji ze spinnera
                    remindersList.clear()
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
                val preferences: SharedPreferences =
                    requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
                val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)
                //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinner
                binding.spinnerReminders.setSelection(spinnerPosition)

                // Jeżeli lista pojazdów jest pusta, ukryj kontrolki odnośnie przypomnień natomiast wyświetl informację o braku pojazdów
                if (carIdList.size == 0) {
                    binding.spinnerRemindersBackground.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.remindersFloatingButton.visibility = View.GONE

                    binding.fragmentRemindersNoCarsCard.visibility = View.VISIBLE
                } else {
                    binding.spinnerRemindersBackground.visibility = View.VISIBLE
                    binding.linearLayout.visibility = View.VISIBLE
                    binding.remindersFloatingButton.visibility = View.VISIBLE

                    binding.fragmentRemindersNoCarsCard.visibility = View.GONE
                }
            }
            onComplete(result)
        }

        remindersAdapter.setOnReminderMoreOptionsClickListener(
            object : RemindersAdapter.OnRemindersMoreOptionsClickListener {
                override fun onItemClick(position: Int, reminders: Reminders) {
                    val preferences: SharedPreferences =
                        requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
                    val spinnerPosition = preferences.getInt("SPINNER POSITION", 0)

                    val newView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                    popupMenus(
                        newView!!,
                        carIdList[spinnerPosition],
                        reminders.id.toString()
                    )
                }
            },
        )

        binding.remindersFloatingButton.setOnClickListener {
            addReminder()
        }
    }

    private fun realTimeUpdates(carId: String) {
        val cars = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId)
            .collection("Car_reminders")
            .orderBy("id", Query.Direction.ASCENDING)

        cars.addSnapshotListener { querySnapshot, error ->
            error?.let {
                Log.w(ContentValues.TAG, "Listen failed", error)
                remindersList.clear()
                return@addSnapshotListener
            }

            for (dc: DocumentChange in querySnapshot?.documentChanges!!) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    remindersList.add(dc.document.toObject(Reminders::class.java))
                }

                if (dc.type == DocumentChange.Type.REMOVED) {
                    remindersList.remove(dc.document.toObject(Reminders::class.java))
                }

                if (dc.type == DocumentChange.Type.MODIFIED) {
                    if (dc.oldIndex == dc.newIndex) {
                        remindersList.removeAt(dc.oldIndex)
                        remindersList.add(
                            dc.oldIndex,
                            dc.document.toObject(Reminders::class.java)
                        )
                        remindersAdapter.notifyItemMoved(dc.oldIndex, dc.newIndex)
                    }
                }
            }
            remindersAdapter.notifyDataSetChanged()
            // Jeżeli lista przypomnień jest równa 0 pokaż stosowny komunikat
            if (remindersList.size == 0) binding.fragmentRemindersNoEntriesCard.visibility =
                View.VISIBLE
            // W przeciwnym wypadku pokaż wpisy i ukryj komunikat
            else binding.fragmentRemindersNoEntriesCard.visibility = View.GONE
        }
    }

    private fun addReminder() {
        // Odczyt ID pojazdu przez ViewModel w zależnosci od pozycji Spinnera w HomeFragment
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val carId = preferences.getString("CAR ID", "")

        val inflateCar = LayoutInflater.from(requireContext())
        val v = inflateCar.inflate(R.layout.add_reminder_layout, null)
        val typeOfReminder =
            v.findViewById<EditText>(R.id.reminders_type_of_reminder_edittext_dialog)
        val reminderDescription =
            v.findViewById<EditText>(R.id.reminders_description_edittext_dialog)
        val mileageValue = v.findViewById<EditText>(R.id.reminders_mileage_value_edittext_dialog)
        val numberOfDays = v.findViewById<EditText>(R.id.reminders_date_value_edittext_dialog)
        val remindersRadioGroup = v.findViewById<RadioGroup>(R.id.reminders_radio_group_dialog)
        val id = System.currentTimeMillis()
            .toString() // Generowanie losowego ID pojazdu jako aktualny czas podany w milisekundach

        // DATEPICKER
        val todayDateInMillis = MaterialDatePicker.todayInUtcMilliseconds().toString()

        val addDialog = AlertDialog.Builder(requireContext())

        remindersRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_button_mileage -> {
                    mileageValue.isEnabled = true
                    numberOfDays.isEnabled = false
                    // Czyszczenie pola data
                    numberOfDays.setText("")
                }
                R.id.radio_button_date -> {
                    mileageValue.isEnabled = false
                    numberOfDays.isEnabled = true
                    // Czyszczenie pola przebieg
                    mileageValue.setText("")
                }
            }
        }

        fun addReminderWithCarId(carMileage: String, numberOfDays: String) {
            var lastCarMileage = "0"
            var carMileageToUpdate = 0

            val cloudCarCollectionReference = cloud.collection("users")
                .document(uid.toString())
                .collection("cars")
                .document(carId.toString())
                .collection("Car_statistics")
                .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                .limit(1)

            cloudCarCollectionReference.get().addOnCompleteListener { result ->
                fun onComplete(task: Task<QuerySnapshot?>) {
                    if (task.isSuccessful) {
                        for (document in task.result!!) {
                            lastCarMileage = document.getString("carMileage").toString()
                        }
                        if (carMileage.isNotEmpty()) {
                            carMileageToUpdate = lastCarMileage.toInt() + carMileage.toInt()
                        }

                        if (carId != null) {
                            val reminder: MutableMap<String, Any> = HashMap()
                            reminder["typeOfReminder"] = typeOfReminder.text.toString()
                            reminder["reminderDescription"] = reminderDescription.text.toString()
                            reminder["carMileage"] = carMileage
                            reminder["carMileageToUpdate"] = carMileageToUpdate.toString()
                            reminder["numberOfDays"] = numberOfDays
                            reminder["dateToRemindInMillis"] = todayDateInMillis
                            reminder["id"] = id

                            // Dodanie wpisu do bazy danych
                            cloud.collection("users").document(uid.toString()).collection("cars")
                                .document(carId).collection("Car_reminders").document(id)
                                .set(reminder)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        activity,
                                        "Przypomnienie dodane pomyślnie!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Czyszczenie listy i odświeżenie layoutu po dodaniu danych
                                    remindersList.clear()
                                    realTimeUpdates(carId)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(activity, "Nie udało się", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    }
                }
                onComplete(result)
            }
        }

        addDialog.setView(v)
        addDialog.setPositiveButton(getString(R.string.reminders_dialog_add)) { dialog, _ ->
            if (typeOfReminder.text.toString().isNotEmpty() &&
                reminderDescription.text.toString().isNotEmpty() &&
                (remindersRadioGroup.checkedRadioButtonId == R.id.radio_button_mileage ||
                        remindersRadioGroup.checkedRadioButtonId == R.id.radio_button_date)
            ) {
                when (remindersRadioGroup.checkedRadioButtonId) {
                    R.id.radio_button_mileage -> {
                        addReminderWithCarId(mileageValue.text.toString(), "")
                    }
                    R.id.radio_button_date -> {
                        addReminderWithCarId("", numberOfDays.text.toString())
                    }
                }
            } else {
                Toast.makeText(activity, "Wypełnij wszystkie pola!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton(getString(R.string.reminders_dialog_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        // Czyszczenie listy i odświeżenie layoutu po stworzeniu dialogu
        /*remindersList.clear()
        realTimeUpdates(carId.toString())*/

        addDialog.create()
        addDialog.show()
    }

    private fun deleteReminder(carId: String, reminderId: String) {
        val car: MutableMap<String, Any> = java.util.HashMap()
        car["id"] = carId
        val reminder: MutableMap<String, Any> = java.util.HashMap()
        reminder["id"] = reminderId

        // Usunięcie przypomnienia o podanym ID
        cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId)
            .collection("Car_reminders")
            .document(reminderId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    getString(R.string.reminders_data_delete_reminder_successfull),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.reminders_data_delete_reminder_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun popupMenus(
        v: View,
        carId: String,
        statisticId: String,
    ) {
        val popupMenus = PopupMenu(context, v, Gravity.END, R.attr.actionOverflowMenuStyle, 0)
        popupMenus.inflate(R.menu.menu_reminders)
        popupMenus.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.del_reminder -> {
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.reminders_delete_reminder_warning_title))
                        .setIcon(R.drawable.ic_baseline_warning_24)
                        .setMessage(getString(R.string.reminders_delete_reminder_warning_message))
                        .setPositiveButton(getString(R.string.reminders_delete_reminder_warning_positive_button)) { dialog, _ ->
                            deleteReminder(carId, statisticId)
                            Toast.makeText(
                                context,
                                getString(R.string.reminders_delete_reminder_warning_success),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.reminders_delete_reminder_warning_negative_button)) { dialog, _ ->
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
}
