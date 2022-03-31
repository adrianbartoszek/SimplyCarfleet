package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.simplycarfleet.R
import com.simplycarfleet.databinding.FragmentSettingsBinding
import com.simplycarfleet.functions.FunctionsAndValues

class SettingsFragment : FunctionsAndValues() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

        //Lista walut
        val currencyList = resources.getStringArray(R.array.currency_array)
        val currencyListShortcut = resources.getStringArray(R.array.currency_array_shortcut)

        //Lista jednostek objętości
        val unitOfVolumeList = resources.getStringArray(R.array.unit_of_volume_array)
        val unitOfVolumeListShortcut =
            resources.getStringArray(R.array.unit_of_volume_array_shortcut)

        //Lista jednostek dystansu
        val unitOfDistanceList = resources.getStringArray(R.array.unit_of_distance_array)
        val unitOfDistanceListShortcut =
            resources.getStringArray(R.array.unit_of_distance_array_shortcut)

        // Spinner do wyboru waluty
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, currencyList)
        binding.spinnerSettingsCurrency.adapter = arrayAdapter
        binding.spinnerSettingsCurrency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    when (position) {
                        0 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    currencyListShortcut[0],
                                    "currency"
                                )
                            }
                        1 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    currencyListShortcut[1],
                                    "currency"
                                )
                            }
                        2 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    currencyListShortcut[2],
                                    "currency"
                                )
                            }
                        3 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    currencyListShortcut[3],
                                    "currency"
                                )
                            }
                        4 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    currencyListShortcut[4],
                                    "currency"
                                )
                            }
                    }

                    // Zapisanie pozycji spinnera do SharedPreferences
                    saveDataIntToSharedPreferences(
                        requireContext(),
                        position,
                        "SPINNER CURRENCY POSITION"
                    )
                    Snackbar.make(
                        requireView(),
                        getString(
                            R.string.settings_fragment_change_units_message,
                            preferences.getString("currency", " zł"),
                            preferences.getString("unit_of_volume", " L"),
                            preferences.getString("unit_of_distance", " km")
                        ),
                        6000
                    )
                        .show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        //Zapamietywanie pozycji spinnera
        val spinnerPosition = preferences.getInt("SPINNER CURRENCY POSITION", 0)
        //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinnera
        binding.spinnerSettingsCurrency.setSelection(spinnerPosition)


        // Spinner do wyboru jednostki objętości
        val arrayAdapter2 = ArrayAdapter(requireContext(), R.layout.dropdown_item, unitOfVolumeList)
        binding.spinnerSettingsUnitOfVolume.adapter = arrayAdapter2
        binding.spinnerSettingsUnitOfVolume.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    when (position) {
                        0 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    unitOfVolumeListShortcut[0],
                                    "unit_of_volume"
                                )
                            }
                        1 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    unitOfVolumeListShortcut[1],
                                    "unit_of_volume"
                                )
                            }

                    }
                    // Zapisanie pozycji spinnera do SharedPreferences
                    saveDataIntToSharedPreferences(
                        requireContext(),
                        position,
                        "SPINNER UNIT OF VOLUME POSITION"
                    )
                    Snackbar.make(
                        requireView(),
                        getString(
                            R.string.settings_fragment_change_units_message,
                            preferences.getString("currency", " zł"),
                            preferences.getString("unit_of_volume", " L"),
                            preferences.getString("unit_of_distance", " km")
                        ),
                        6000
                    )
                        .show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        //Zapamietywanie pozycji spinnera
        val spinnerPosition2 = preferences.getInt("SPINNER UNIT OF VOLUME POSITION", 0)
        //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinnera
        binding.spinnerSettingsUnitOfVolume.setSelection(spinnerPosition2)

        //
        // Spinner do wyboru jednostki objętości
        val arrayAdapter3 =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, unitOfDistanceList)
        binding.spinnerSettingsUnitOfDistance.adapter = arrayAdapter3
        binding.spinnerSettingsUnitOfDistance.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    when (position) {
                        0 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    unitOfDistanceListShortcut[0],
                                    "unit_of_distance"
                                )
                            }
                        1 ->
                            activity?.let {
                                saveDataToSharedPreferences(
                                    it,
                                    unitOfDistanceListShortcut[1],
                                    "unit_of_distance"
                                )
                            }

                    }
                    // Zapisanie pozycji spinnera do SharedPreferences
                    saveDataIntToSharedPreferences(
                        requireContext(),
                        position,
                        "SPINNER UNIT OF DISTANCE POSITION"
                    )
                    Snackbar.make(
                        requireView(),
                        getString(
                            R.string.settings_fragment_change_units_message,
                            preferences.getString("currency", " zł"),
                            preferences.getString("unit_of_volume", " L"),
                            preferences.getString("unit_of_distance", " km")
                        ),
                        6000
                    )
                        .show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        //Zapamietywanie pozycji spinnera
        val spinnerPosition3 = preferences.getInt("SPINNER UNIT OF DISTANCE POSITION", 0)
        //Ustawienie pozycji spinnera działa dopiero po dodaniu elementów z listy do Spinnera
        binding.spinnerSettingsUnitOfDistance.setSelection(spinnerPosition3)


    }
}