package com.simplycarfleet.nav_menu

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.data.Reminders

class RemindersAdapter(
    private val c: Context,
    private val remindersList: ArrayList<Reminders>,
) :
    RecyclerView.Adapter<RemindersAdapter.RemindersViewHolder>() {
    // Inne zmienne
    private lateinit var mListener: OnItemClickListener
    private lateinit var mListener2: OnRemindersMoreOptionsClickListener
    private val preferences: SharedPreferences =
        c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)

    // Zmienne Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid
    private val cloud: FirebaseFirestore = FirebaseFirestore.getInstance()

    interface OnItemClickListener {
        fun onItemClick(position: Int, reminders: Reminders)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    interface OnRemindersMoreOptionsClickListener {
        fun onItemClick(position: Int, reminders: Reminders)
    }

    fun setOnReminderMoreOptionsClickListener(listener: OnRemindersMoreOptionsClickListener) {
        mListener2 = listener
    }

    inner class RemindersViewHolder(
        view: View,
        listener: OnItemClickListener,
        listener2: OnRemindersMoreOptionsClickListener,
    ) :
        RecyclerView.ViewHolder(view) {
        val remindersDateOrMileageValue: TextView =
            view.findViewById(R.id.reminders_date_or_mileage_value_text_view)
        val remindersTypeOfReminder: TextView =
            view.findViewById(R.id.reminders_type_of_reminder_text_view)
        val remindersFrequencyValue: TextView =
            view.findViewById(R.id.reminders_frequency_value_text_view)
        val remindersMoreOptions: ImageView = view.findViewById(R.id.reminders_more_options)
        val reminderImageView: ImageView = view.findViewById(R.id.reminders_image_view)
        val dateOrMileageTextView: TextView =
            view.findViewById(R.id.reminders_date_or_mileage_text_view)
        val remindersFrequencyTextView: TextView =
            view.findViewById(R.id.reminders_frequency_text_view)

        init {
            view.setOnClickListener {
                listener.onItemClick(adapterPosition, remindersList[adapterPosition])
            }
            remindersMoreOptions.setOnClickListener {
                listener2.onItemClick(adapterPosition, remindersList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemindersViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_row_reminders, parent, false)
        return RemindersViewHolder(view, mListener, mListener2)
    }

    override fun onBindViewHolder(holder: RemindersViewHolder, position: Int) {
        val reminders: Reminders = remindersList[position]
        val unitOfDistance = preferences.getString("unit_of_distance", " km")
        // WSPÓLNE POLA
        holder.remindersTypeOfReminder.text = reminders.typeOfReminder
        holder.dateOrMileageTextView.text =
            holder.itemView.context.getString(R.string.reminders_adapter_left)
        holder.remindersFrequencyTextView.text =
            holder.itemView.context.getString(R.string.reminders_adapter_every)

        val preferences: SharedPreferences =
            c.getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val carId = preferences.getString("CAR ID", "").toString()

        when (reminders.carMileage) {
            "" -> {
                // Aktualna data w milisekundach
                val todayDateInMillis = MaterialDatePicker.todayInUtcMilliseconds()
                // Data dodania wpisu w milisekundach (musi byc aktualizowana co liczbe dni podana w reminderDateToRemind
                val reminderDateInMillis = reminders.dateToRemindInMillis
                // Wartosc jednego dnia w milisekundach
                val dayInMillis = 86400000
                // Liczba dni co ile ma byc przypomnienie pomnozone przez dzien w milisekundach
                val numberOfDaysInMillis = reminders.numberOfDays?.toLong()?.times(dayInMillis)
                // Data w ktorej system ma przypomniec
                val dateToRemindInMillis =
                    reminderDateInMillis.toString().toLong() + numberOfDaysInMillis!!
                // Liczba pozostalych dni do daty przypomnienia
                val daysToRemind =
                    ((dateToRemindInMillis - todayDateInMillis) / dayInMillis).toInt()

                holder.remindersDateOrMileageValue.text =
                    holder.itemView.context.getString(R.string.reminders_adapter_days, daysToRemind.toString())
                holder.remindersFrequencyValue.text = holder.itemView.context.getString(
                    R.string.reminders_adapter_days,
                    reminders.numberOfDays
                )

                // Jeżeli dni przypomnienia są mniejsze lub równe 0 - zrob update zeby odliczanie zaczelo sie od nowa
                when {
                    daysToRemind <= 0 -> {
                        val newDateInMillis =
                            reminders.dateToRemindInMillis!!.toLong() + reminders.numberOfDays.toLong() * dayInMillis
                        // Zaktualizuj dane w bazie na potrzeby przyszłych obliczeń
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_reminders")
                            .document(reminders.id.toString())
                            .update(
                                mapOf(
                                    "dateToRemindInMillis" to newDateInMillis.toString()
                                )
                            )
                            .addOnSuccessListener {
                                /*Toast.makeText(c,
                                    "Pomyślnie zmodyfikowano wpis",
                                    Toast.LENGTH_SHORT)
                                    .show()*/
                            }
                            .addOnFailureListener {
                                /*Toast.makeText(c, "Nie Udalo sie", Toast.LENGTH_SHORT).show()*/
                            }
                    }
                    // Jezeli liczba pozostałych dni do przypomnienia jest równa 3, zmień kolor ikony na żółty
                    daysToRemind == 3 -> {
                        holder.reminderImageView.setColorFilter(
                            ContextCompat.getColor(
                                holder.reminderImageView.context,
                                R.color.reminderYellow
                            )
                        )
                    }
                    // Jezeli liczba pozostałych dni do przypomnienia jest równa 2, zmień kolor ikony na pomarańczowy
                    daysToRemind == 2 -> {
                        holder.reminderImageView.setColorFilter(
                            ContextCompat.getColor(
                                holder.reminderImageView.context,
                                R.color.reminderOrange
                            )
                        )
                    }
                    // Jezeli liczba pozostałych dni do przypomnienia jest równa 1, zmień kolor ikony na czerwony
                    daysToRemind == 1 -> {
                        holder.reminderImageView.setColorFilter(
                            ContextCompat.getColor(
                                holder.reminderImageView.context,
                                R.color.reminderRed
                            )
                        )
                        holder.remindersDateOrMileageValue.text = holder.itemView.context.getString(
                            R.string.reminders_adapter_day,
                            daysToRemind.toString()
                        )

                        if (reminders.numberOfDays == "1") holder.remindersFrequencyValue.text =
                            holder.itemView.context.getString(
                                R.string.reminders_adapter_days,
                                reminders.numberOfDays
                            )
                    }
                    else -> {
                        holder.reminderImageView.setColorFilter(
                            ContextCompat.getColor(
                                holder.reminderImageView.context,
                                R.color.black
                            )
                        )
                    }
                }
            }
            else -> {
                // Zmienna do przechowywania ostatniego przebiegu
                var lastCarMileage = 0
                // Pobieranie z bazy ostatniego przebiegu
                val cloudCarCollectionReference = cloud.collection("users")
                    .document(uid.toString())
                    .collection("cars")
                    .document(carId)
                    .collection("Car_statistics")
                    .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                    .limit(1)

                cloudCarCollectionReference.get().addOnCompleteListener { result ->
                    fun onComplete(task: Task<QuerySnapshot?>) {
                        if (task.isSuccessful) {
                            for (document in task.result!!) {
                                lastCarMileage = document.getString("carMileage").toString().toInt()
                            }
                            // Jeżeli częstotliwość przypomnienia nie jest pusta
                            if (reminders.carMileageToUpdate != "") {
                                // Częstotliwość - przebieg = ile pozostało do przypomnienia
                                val remainingMileage =
                                    reminders.carMileageToUpdate.toString().toInt() - lastCarMileage
                                // Jeżeli pozostały przebieg jest <= 0 wtedy dodaj
                                // częstotliwość do sumy ostatniego przebiegu i częstotliwości
                                if (remainingMileage <= 0) {
                                    val newCarMileageToUpdate =
                                        reminders.carMileageToUpdate.toString()
                                            .toInt() + reminders.carMileage.toString().toInt()

                                    // Zaktualizuj dane w bazie na potrzeby przyszłych obliczeń
                                    cloud.collection("users").document(uid.toString())
                                        .collection("cars")
                                        .document(carId).collection("Car_reminders")
                                        .document(reminders.id.toString())
                                        .update(
                                            mapOf(
                                                "carMileageToUpdate" to newCarMileageToUpdate.toString()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            /*Toast.makeText(c,
                                                "UPDATE",
                                                Toast.LENGTH_SHORT)
                                                .show()*/
                                        }
                                        .addOnFailureListener {
                                            /*Toast.makeText(c, "ERROR", Toast.LENGTH_SHORT).show()*/
                                        }
                                }
                                // Obliczanie 20% wartości przebiegu pozostałego do przypomnienia
                                val mileageToChangeColorOfIcon =
                                    reminders.carMileage.toString().toDouble() * 0.2
                                // Zmiana ikony na czerwoną jeżeli pozostała ilość km do przypomnienia jest mniejsza lub równa 20%
                                if (remainingMileage <= mileageToChangeColorOfIcon.toInt()) {
                                    holder.reminderImageView.setColorFilter(
                                        ContextCompat.getColor(
                                            holder.reminderImageView.context,
                                            R.color.reminderRed
                                        )
                                    )
                                } else {
                                    holder.reminderImageView.setColorFilter(
                                        ContextCompat.getColor(
                                            holder.reminderImageView.context,
                                            R.color.black
                                        )
                                    )
                                }
                                holder.remindersDateOrMileageValue.text =
                                    remainingMileage.toString() + unitOfDistance
                                holder.remindersFrequencyValue.text =
                                    reminders.carMileage + unitOfDistance
                            }
                        }
                    }
                    onComplete(result)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return remindersList.size
    }
}
