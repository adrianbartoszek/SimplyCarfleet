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
import com.simplycarfleet.databinding.FragmentAddExpenditureInfoBinding
import com.simplycarfleet.functions.FunctionsAndValues

class AddExpenditureFragmentInfo : FunctionsAndValues() {
    private var _binding: FragmentAddExpenditureInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenditureInfoBinding.inflate(inflater, container, false)
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
        val expenditureType = preferences.getString("expenditureType", "").toString()
        val totalCost = preferences.getString("totalCost", "").toString()
        val expenditureImageId1 = preferences.getString("expenditureImageId1", "").toString()
        val expenditureImageId2 = preferences.getString("expenditureImageId2", "").toString()
        val expenditureImageId3 = preferences.getString("expenditureImageId3", "").toString()
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        binding.expenditureTypeOfStatisticEdittextAddExpenditureInfo.setText(typeOfStatistic)
        binding.expenditureDateSelectedEdittextAddExpenditureInfo.setText(statisticDate)
        binding.expenditureCarMileageEditTextAddExpenditureInfo.setText(carMileage)
        binding.expenditureExpenditureTypeEdittextAddExpenditureInfo.setText(expenditureType)
        binding.expenditureTotalCostEdittextAddExpenditureInfo.setText(totalCost)

        binding.expenditureCarMileageTextfieldAddExpenditureInfo.hint =
            getString(R.string.expenditure_more_info_last_mileage) + " [${unitOfDistance?.trim()}]"
        binding.expenditureTotalCostTextfieldAddExpenditureInfo.hint =
            getString(R.string.expenditure_more_info_total_cost) + " [${unitTotalCost?.trim()}]"

        // Wyświetlenie zdjęć paragonów
        if (expenditureImageId1.isNotEmpty()) Glide.with(view).load(expenditureImageId1)
            .into(binding.receiptImageViewAddExpenditureInfo)
        if (expenditureImageId2.isNotEmpty()) Glide.with(view).load(expenditureImageId2)
            .into(binding.receiptImageView2AddExpenditureInfo)
        if (expenditureImageId3.isNotEmpty()) Glide.with(view).load(expenditureImageId3)
            .into(binding.receiptImageView3AddExpenditureInfo)

        binding.receiptImageViewAddExpenditureInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), expenditureImageId1, "fullScreenImageId")
            saveDataToSharedPreferences(
                requireContext(),
                "AddExpenditureFragmentInfo",
                "fragmentName"
            )
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView2AddExpenditureInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), expenditureImageId2, "fullScreenImageId")
            saveDataToSharedPreferences(
                requireContext(),
                "AddExpenditureFragmentInfo",
                "fragmentName"
            )
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView3AddExpenditureInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), expenditureImageId3, "fullScreenImageId")
            saveDataToSharedPreferences(
                requireContext(),
                "AddExpenditureFragmentInfo",
                "fragmentName"
            )
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.buttonCancelAddExpenditureInfo.setOnClickListener {
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