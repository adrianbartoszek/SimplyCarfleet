package com.simplycarfleet.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private var _carId = MutableLiveData("")
    val carId: LiveData<String> = _carId

    private var _spinnerPosition = MutableLiveData(0)
    val spinnerPosition: LiveData<Int> = _spinnerPosition
  
    fun saveCarId(carId: String){
        _carId.value = carId
    }
    fun saveSpinnerPosition(spinnerPosition: Int){
        _spinnerPosition.value = spinnerPosition
    }
}