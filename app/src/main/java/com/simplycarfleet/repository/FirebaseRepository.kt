package com.simplycarfleet.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simplycarfleet.data.User

class FirebaseRepository {
    private val repoDebug = "REPO_DEBUG"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val cloud: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun createNewUser(user: User) {
        cloud.collection("users")
            .document(user.uid!!)
            .set(user)
    }

    fun getUserData(): LiveData<User> {
        val cloudResult = MutableLiveData<User>()
        val uid = auth.currentUser?.uid

        cloud.collection("users")
            .document(uid!!)
            .get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)
                cloudResult.postValue(user!!)
            }
            .addOnFailureListener {
                Log.d(repoDebug, it.message.toString())
            }
        return cloudResult
    }
}