package com.simplycarfleet.data

import com.google.firebase.Timestamp

data class Car(
    val brand: String? = null,
    val yearOfProduction: String? = null,
    val model: String? = null,
    val fuelType: String? = null,
    val VIN: String? = null,
    val policyNumber: String? = null,
    val notes: String? = null,
    val id: String? = null,
    val dateCreated: Timestamp? = null,
    val carImageId: String? = null
)
