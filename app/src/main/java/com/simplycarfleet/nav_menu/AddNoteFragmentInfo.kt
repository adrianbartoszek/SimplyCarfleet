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
import com.simplycarfleet.databinding.FragmentAddNoteInfoBinding
import com.simplycarfleet.functions.FunctionsAndValues

class AddNoteFragmentInfo : FunctionsAndValues() {
    private var _binding: FragmentAddNoteInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNoteInfoBinding.inflate(inflater, container, false)
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
        val noteDescription = preferences.getString("noteDescription", "").toString()
        val noteImageId1 = preferences.getString("noteImageId1", "").toString()
        val noteImageId2 = preferences.getString("noteImageId2", "").toString()
        val noteImageId3 = preferences.getString("noteImageId3", "").toString()
        val unitOfDistance = preferences.getString("unit_of_distance", " km")

        binding.noteTypeOfStatisticEdittextAddNoteInfo.setText(typeOfStatistic)
        binding.noteDateSelectedEdittextAddNoteInfo.setText(statisticDate)
        binding.noteCarMileageEdittextAddNoteInfo.setText(carMileage)
        binding.noteNoteDescriptionEdittextAddNoteInfo.setText(noteDescription)

        binding.noteCarMileageTextfieldAddNoteInfo.hint =
            getString(R.string.note_more_info_last_mileage) + " [${unitOfDistance?.trim()}]"

        Glide.with(view).load(noteImageId1).into(binding.receiptImageViewAddNoteInfo)
        Glide.with(view).load(noteImageId2).into(binding.receiptImageView2AddNoteInfo)
        Glide.with(view).load(noteImageId3).into(binding.receiptImageView3AddNoteInfo)

        binding.receiptImageViewAddNoteInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), noteImageId1, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddNoteFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView2AddNoteInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), noteImageId2, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddNoteFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }
        binding.receiptImageView3AddNoteInfo.setOnClickListener {
            saveDataToSharedPreferences(requireContext(), noteImageId3, "fullScreenImageId")
            saveDataToSharedPreferences(requireContext(), "AddNoteFragmentInfo", "fragmentName")
            replaceFragment(FullScreenPhotoFragment())
        }

        binding.buttonCancelAddNoteInfo.setOnClickListener {
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