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
import com.simplycarfleet.databinding.FragmentAddServiceInfoBinding
import com.simplycarfleet.functions.FunctionsAndValues

class AddServiceFragmentInfo : FunctionsAndValues() {
    private var _binding: FragmentAddServiceInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddServiceInfoBinding.inflate(inflater, container, false)
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
        val carWorkshop = preferences.getString("carWorkshop", "").toString()
        val serviceType = preferences.getString("serviceType", "").toString()
        val totalCost = preferences.getString("totalCost", "").toString()
        val serviceImageId1 = preferences.getString("serviceImageId1", "").toString()
        val serviceImageId2 = preferences.getString("serviceImageId2", "").toString()
        val serviceImageId3 = preferences.getString("serviceImageId3", "").toString()
        val unitTotalCost = preferences.getString("currency", " zł")
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        binding.serviceTypeOfStatisticEdittextAddServiceInfo.setText(typeOfStatistic)
        binding.serviceStatisticDateEdittextDialog.setText(statisticDate)
        binding.serviceCarMileageEdittextAddServiceInfo.setText(carMileage)
        binding.serviceWorkshopEdittextAddServiceInfo.setText(carWorkshop)
        binding.serviceServiceTypeEdittextAddServiceInfo.setText(serviceType)
        binding.serviceTotalCostEdittextAddServiceInfo.setText(totalCost)

        binding.serviceCarMileageTextfieldAddServiceInfo.hint =
            getString(R.string.service_more_info_last_mileage) + " [${unitOfDistance?.trim()}]"
        binding.serviceTotalCostTextfieldAddServiceInfo.hint =
            getString(R.string.service_more_info_total_cost) + " [${unitTotalCost?.trim()}]"

        // Wyświetlenie zdjęć paragonów
        Glide.with(view).load(serviceImageId1).into(binding.receiptImageViewAddServiceInfo)
        Glide.with(view).load(serviceImageId2).into(binding.receiptImageView2AddServiceInfo)
        Glide.with(view).load(serviceImageId3).into(binding.receiptImageView3AddServiceInfo)

        binding.receiptImageViewAddServiceInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), serviceImageId1, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddServiceFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView2AddServiceInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), serviceImageId2, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddServiceFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView3AddServiceInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), serviceImageId3, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddServiceFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.buttonCancelAddServiceInfo.setOnClickListener {
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