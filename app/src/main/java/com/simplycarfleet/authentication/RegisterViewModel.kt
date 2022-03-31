package com.simplycarfleet.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.simplycarfleet.data.User
import com.simplycarfleet.repository.FirebaseRepository

class RegisterViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    val user: LiveData<User> = repository.getUserData()
    fun createNewUser(user: User) {
        repository.createNewUser(user)
    }
}