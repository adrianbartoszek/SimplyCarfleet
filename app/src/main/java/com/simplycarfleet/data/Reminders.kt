package com.simplycarfleet.data


data class Reminders(
    val id: String? = null,
    val typeOfReminder: String? = null,
    val reminderDescription: String? = null,
    // W przypadku przypomnienia odnosnie dni
    val dateToRemindInMillis: String? = null,
    val numberOfDays: String? = null,
    // W przypadku przypomnienia odnosnie przebiegu
    val carMileage: String? = null,
    val carMileageToUpdate: String? = null
)


