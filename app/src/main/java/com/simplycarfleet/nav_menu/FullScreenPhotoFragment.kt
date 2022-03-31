package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplycarfleet.databinding.FragmentFullScreenPhotoBinding
import com.simplycarfleet.functions.FunctionsAndValues

class FullScreenPhotoFragment : FunctionsAndValues() {
    private var _binding: FragmentFullScreenPhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val fullScreenImageId = preferences.getString("fullScreenImageId","").toString()
        val fragmentName = preferences.getString("fragmentName","").toString()
        Glide.with(view).load(fullScreenImageId).into(binding.imageViewFullScreenPhotoFragment)

        binding.buttonCancelFragmentFullScreenPhoto.setOnClickListener{
            when(fragmentName){
                "AddExpenditureFragmentInfo" -> replaceFragment(AddExpenditureFragmentInfo())
                "AddNoteFragmentInfo" -> replaceFragment(AddNoteFragmentInfo())
                "AddRefuelFragmentInfo" -> replaceFragment(AddRefuelFragmentInfo())
                "AddServiceFragmentInfo" -> replaceFragment(AddServiceFragmentInfo())
            }



        }

    }

    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        // W przypadku kliknięcia przycisku wstecz przekieruj do poprzedniego fragmentu
        val fragmentName = preferences.getString("fragmentName","").toString()
        requireView().setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when(fragmentName){
                    "AddExpenditureFragmentInfo" -> replaceFragment(AddExpenditureFragmentInfo())
                    "AddNoteFragmentInfo" -> replaceFragment(AddNoteFragmentInfo())
                    "AddRefuelFragmentInfo" -> replaceFragment(AddRefuelFragmentInfo())
                    "AddServiceFragmentInfo" -> replaceFragment(AddServiceFragmentInfo())
                }
                true
            } else false
        }
    }

}