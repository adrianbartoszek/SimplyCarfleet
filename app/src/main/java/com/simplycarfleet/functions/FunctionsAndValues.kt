package com.simplycarfleet.functions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.data.SharedViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


abstract class FunctionsAndValues : Fragment() {

    //Zmienne

    // Zmienne Firebase
    protected val auth: FirebaseAuth = FirebaseAuth.getInstance()
    protected val cloud: FirebaseFirestore = FirebaseFirestore.getInstance()
    protected val storage: FirebaseStorage = FirebaseStorage.getInstance()
    protected val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()
    protected val uid = auth.currentUser?.uid

    // Odwołanie do kolekcji "cars"
    protected val cloudCarCollectionReference = cloud.collection("users")
        .document(uid.toString())
        .collection("cars")

    protected val sharedViewModel: SharedViewModel by activityViewModels()

    //Formatowanie aby wyświetlało 2 miejsca po przecinku
    //bez wyświetlania samotnych zer
    protected val formatToDisplayTwoDecimalPlaces = DecimalFormat("0.##")

    protected val simpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    //Aktualna data
    @RequiresApi(Build.VERSION_CODES.O)
    protected val newDate: String = DateTimeFormatter
        .ofPattern("dd MMM yyyy")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now())


    // Inne zmienne
    private val repoDebug = "REPO_DEBUG"




    //Funkcje

    protected fun uploadStatisticPhoto(
        bytes: ByteArray,
        carId: String,
        statisticId: String,
        imageName: String,
        formatter: String,
        referenceName: String
    ) {

        val formatter = SimpleDateFormat(formatter, Locale.getDefault())
        val dateNow = Date()
        val fileName = formatter.format(dateNow)

        storage.getReference(referenceName)
            .child(auth.currentUser!!.uid + fileName)
            .putBytes(bytes)
            .addOnCompleteListener {
                Log.d(repoDebug, "Complete upload")
            }
            .addOnSuccessListener {
                it.storage.downloadUrl
                    .addOnSuccessListener {
                        cloud.collection("users")
                            .document(auth.currentUser!!.uid).collection("cars").document(carId)
                            .collection("Car_statistics").document(statisticId)
                            .update(imageName, it.toString())
                            .addOnSuccessListener {
                                Log.d(repoDebug, "Complete upload photo")
                            }
                            .addOnFailureListener {
                                Log.d(repoDebug, it.message.toString())
                            }
                    }
            }
            .addOnFailureListener {
                Log.d(repoDebug, it.message.toString())
            }
    }

    protected fun uploadCarPhoto(
        bytes: ByteArray,
        carId: String,
        imageName: String,
        formatter: String,
        referenceName: String
    ) {

        val formatter = SimpleDateFormat(formatter, Locale.getDefault())
        val dateNow = Date()
        val fileName = formatter.format(dateNow)

        storage.getReference(referenceName)
            .child(auth.currentUser!!.uid + fileName)
            .putBytes(bytes)
            .addOnCompleteListener {
                Log.d(repoDebug, "Complete upload")
            }
            .addOnSuccessListener {
                it.storage.downloadUrl
                    .addOnSuccessListener {
                        cloud.collection("users")
                            .document(auth.currentUser!!.uid).collection("cars").document(carId)
                            .update(imageName, it.toString())
                            .addOnSuccessListener {
                                Log.d(repoDebug, "Complete upload photo")
                            }
                            .addOnFailureListener {
                                Log.d(repoDebug, it.message.toString())
                            }
                    }
            }
            .addOnFailureListener {
                Log.d(repoDebug, it.message.toString())
            }
    }


    protected fun saveDataToSharedPreferences(
        c: Context,
        value: String,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putString(valueIdToReference, value)
        }.apply()
    }

    protected fun saveDataIntToSharedPreferences(
        c: Context,
        value: Int,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putInt(valueIdToReference, value)
        }.apply()
    }

    protected fun saveDataFloatToSharedPreferences(
        c: Context,
        value: Float,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putFloat(valueIdToReference, value)
        }.apply()
    }

    protected fun saveDataLongToSharedPreferences(
        c: Context,
        value: Long,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putLong(valueIdToReference, value)
        }.apply()
    }


    protected fun saveDateRangeValuesToSharedPreferences(
        c: Context,
        startDate: String,
        endDate: String,
        dateSelected: String,
        startDateId: String,
        endDateId: String,
        dateSelectedId: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.apply {
            putString(startDateId, startDate)
            putString(endDateId, endDate)
            putString(dateSelectedId, dateSelected)
        }.apply()
    }

    protected fun replaceFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.commit {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            replace(R.id.FrameLayoutMain, fragment)
        }
    }

    protected fun saveArrayToSharedPreferences(
        c: Context,
        list: MutableList<Float>,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        val gson = Gson()
        val jsonString = gson.toJson(list)

        editor.apply {
            putString(valueIdToReference, jsonString)
        }.apply()
    }

    protected fun saveArrayOfIntToSharedPreferences(
        c: Context,
        list: MutableList<Int>,
        valueIdToReference: String
    ) {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        val gson = Gson()
        val jsonString = gson.toJson(list)

        editor.apply {
            putString(valueIdToReference, jsonString)
        }.apply()
    }

    protected fun loadArrayFromSharedPreferences(
        c: Context,
        valueIdToReference: String
    ): MutableList<Float>? {
        val preferences: SharedPreferences = c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val json = preferences.getString(valueIdToReference, "")
        val listType = object : TypeToken<MutableList<Float>>() {}.type
        return Gson().fromJson(json, listType)
    }

    protected fun getFirstAndLastDateFromFirebase() {
        var firstDateInMillis = "1265065200000"
        var lastDateInMillis = System.currentTimeMillis().toString()

        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val carId = preferences.getString("CAR ID", "").toString()

        // Odwołanie do kolekcji Car_statistics wybranego pojazdu
        val cloudStatisticsCollectionReference = cloud.collection("users")
            .document(uid.toString())
            .collection("cars")
            .document(carId.toString())
            .collection("Car_statistics")

        //Odwołanie do pierwszej daty
        val firstDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.ASCENDING
            )
                .limit(1)

        //Odwołanie do ostatniej daty
        val lastDateReference =
            cloudStatisticsCollectionReference.orderBy(
                "statisticDateInMillis",
                Query.Direction.DESCENDING
            )
                .limit(1)

        //Pobranie pierwszej daty w milisekundach
        firstDateReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        firstDateInMillis = document.getString("statisticDateInMillis").toString()
                    }
                    saveDataToSharedPreferences(
                        requireContext(),
                        firstDateInMillis,
                        "firstDateInMillis"
                    )
                }
            }
            onComplete(result)
        }

        //Pobranie ostatniej daty w milisekundach
        lastDateReference.get().addOnCompleteListener { result ->
            fun onComplete(task: Task<QuerySnapshot?>) {
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        lastDateInMillis = document.getString("statisticDateInMillis").toString()
                    }
                    saveDataToSharedPreferences(
                        requireContext(),
                        lastDateInMillis,
                        "lastDateInMillis"
                    )
                }
            }
            onComplete(result)
        }

    }

    protected fun startApp() {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }


}