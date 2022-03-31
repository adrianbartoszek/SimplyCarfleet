package com.simplycarfleet.data

data class Statistics(
    //WSPOLNE
    val id: String? = null,
    val typeOfStatistic: String? = null,
    val statisticDate: String? = null,
    val statisticDateInMillis: String? = null,
    val totalCost: String? = null,
    val carMileage: String? = null,

    //TANKOWANIE
    val gasStation: String? = null,
    val fuelAmount: String? = null,
    val fuelPrice: String? = null,
    val distanceTravelledSinceRefueling: String? = null,
    val refuelImageId1: String? = null,
    val refuelImageId2: String? = null,
    val refuelImageId3: String? = null,

    //SERWIS
    val serviceType: String? = null,
    val carWorkshop: String? = null,
    val serviceImageId1: String? = null,
    val serviceImageId2: String? = null,
    val serviceImageId3: String? = null,

    //WYDATEK
    val expenditureType: String? = null,
    val expenditureImageId1: String? = null,
    val expenditureImageId2: String? = null,
    val expenditureImageId3: String? = null,

    //NOTATKA
    val noteDescription: String? = null,
    val noteImageId1: String? = null,
    val noteImageId2: String? = null,
    val noteImageId3: String? = null
)

