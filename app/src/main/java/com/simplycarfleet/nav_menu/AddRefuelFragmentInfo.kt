package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentAddRefuelInfoBinding
import com.simplycarfleet.functions.FunctionsAndValues

class AddRefuelFragmentInfo : FunctionsAndValues() {
    private var _binding: FragmentAddRefuelInfoBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRefuelInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Usunięcie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.hide()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val typeOfStatistic = preferences.getString("typeOfStatistic", "").toString()
        val statisticDate = preferences.getString("statisticDate", "").toString()
        val carMileage = preferences.getString("carMileage", "").toString()
        val gasStation = preferences.getString("gasStation", "").toString()
        val fuelAmount = preferences.getString("fuelAmount", "").toString()
        val fuelPrice = preferences.getString("fuelPrice", "").toString()
        val totalCost = preferences.getString("totalCost", "").toString()
        val refuelImageId1 = preferences.getString("refuelImageId1", "").toString()
        val refuelImageId2 = preferences.getString("refuelImageId2", "").toString()
        val refuelImageId3 = preferences.getString("refuelImageId3", "").toString()
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        binding.refuelingTypeOfStatisticEditTextAddRefuelInfo.setText(typeOfStatistic)
        binding.refuelingStatisticDateEdittextAddRefuelInfo.setText(statisticDate)
        binding.refuelingCarMileageEdittextAddRefuelInfo.setText(carMileage)
        binding.refuelingGasStationEdittextAddRefuelInfo.setText(gasStation)
        binding.refuelingFuelAmountEdittextAddRefuelInfo.setText(fuelAmount)
        binding.refuelingFuelPriceEdittextAddRefuelInfo.setText(fuelPrice)
        binding.refuelingTotalCostEdittextAddRefuelInfo.setText(totalCost)

        binding.refuelingCarMileageTextfieldAddRefuelInfo.hint =
            getString(R.string.refueling_more_info_last_mileage) + " [${unitOfDistance?.trim()}]"
        binding.refuelingFuelAmountTextfieldAddRefuelInfo.hint =
            getString(R.string.refueling_more_info_fuel_amount) + " [${unitOfVolume?.trim()}]"
        binding.refuelingFuelPriceTextfieldAddRefuelInfo.hint =
            getString(R.string.refueling_more_info_fuel_price) + " [${unitTotalCost?.trim()}]"
        binding.refuelingTotalCostTextfieldAddRefuelInfo.hint =
            getString(R.string.refueling_more_info_total_cost) + " [${unitTotalCost?.trim()}]"

        // Wyświetlenie zdjęć paragonów
        Glide.with(view).load(refuelImageId1).into(binding.receiptImageViewAddRefuelInfo)
        Glide.with(view).load(refuelImageId2).into(binding.receiptImageView2AddRefuelInfo)
        Glide.with(view).load(refuelImageId3).into(binding.receiptImageView3AddRefuelInfo)

        binding.receiptImageViewAddRefuelInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), refuelImageId1, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddRefuelFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView2AddRefuelInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), refuelImageId2, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddRefuelFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView3AddRefuelInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), refuelImageId3, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddRefuelFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.buttonCancelAddRefuelInfo.setOnClickListener {
            replaceFragment(HomeFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        // W przypadku kliknięcia przycisku wstecz przekieruj do poprzedniego fragmentu
        requireView().setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                replaceFragment(HomeFragment())
                true
            } else false
        }
    }
}